package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.AIConfig;
import com.mikuliku.touhoulittlemaidgtnh.ai.MaidMainThreadScheduler;
import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Forge 1.7.10 安全普通合成执行器。
 *
 * 本类现在真正执行 Forge IRecipe：
 * 1. 查找真实 CraftingManager 配方。
 * 2. 解析 vanilla shaped/shapeless 与 Forge OreDictionary shaped/shapeless。
 * 3. 同时检查玩家背包和附近 IInventory。
 * 4. 必要时从附近容器取材料。
 * 5. 用真实材料构造 3x3 InventoryCrafting。
 * 6. 再调用真实 recipe.matches() 做最终确认。
 * 7. 只有最终匹配成功后才扣材料并放入真实产物。
 *
 * GregTech 机器配方仍由 GT5U 专用执行器负责，本类绝不伪造机器输出。
 */
public final class RecipeSafeExecutorTool implements Tool {

    private static final int GRID_SIZE = 9;

    @Override
    public String getName() {
        return "recipe_safe_execute";
    }

    @Override
    public String getDescription() {
        return "真实执行Forge 1.7.10普通IRecipe；会从玩家背包和附近容器取材料，"
                + "支持普通、OreDictionary shaped/shapeless配方，不伪造GregTech机器输出。";
    }

    @Override
    public ToolResult execute(
            final ToolContext context,
            final String argumentsJson) {

        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        final EntityPlayer player = context.getPlayer();

        try {
            String result = MaidMainThreadScheduler.callAndWait(
                    new Callable<String>() {
                        @Override
                        public String call() {
                            return executeOnMainThread(
                                    player,
                                    argumentsJson);
                        }
                    },
                    15,
                    TimeUnit.SECONDS);

            return ToolResult.success(result);
        } catch (Exception e) {
            return ToolResult.failure(
                    "合成执行失败："
                            + e.getClass().getSimpleName()
                            + " "
                            + String.valueOf(e.getMessage()));
        }
    }

    private static String executeOnMainThread(
            EntityPlayer player,
            String argumentsJson) {

        if (player.worldObj == null) {
            return "玩家世界不可用。";
        }

        if (player.worldObj.isRemote) {
            return "合成执行必须在服务器逻辑侧进行。";
        }

        String query = extract(argumentsJson, "query");
        int amount = parseAmount(argumentsJson);

        if (query.length() == 0) {
            return "没有指定要合成的物品。";
        }

        if (amount <= 0 || amount > 64) {
            return "一次最多执行64次合成。";
        }

        IRecipe recipe = findRecipe(query);

        if (recipe == null) {
            return "没有在Forge CraftingManager中找到与“"
                    + query
                    + "”匹配的普通配方。";
        }

        ItemStack staticOutput;

        try {
            staticOutput = recipe.getRecipeOutput();
        } catch (Throwable t) {
            return "找到配方，但读取配方输出失败："
                    + t.getClass().getSimpleName();
        }

        if (staticOutput == null) {
            return "找到配方，但该配方没有静态输出。";
        }

        if (!isSupportedRecipe(recipe)) {
            return "找到真实配方“"
                    + staticOutput.getDisplayName()
                    + "”，但它是自定义IRecipe实现，"
                    + "当前安全执行器不会猜测其内部材料。"
                    + "请交给对应模组的专用执行器。";
        }

        int crafted = 0;

        for (int i = 0; i < amount; i++) {
            String one = craftOne(player, recipe);

            if (one.startsWith("SUCCESS:")) {
                crafted++;
                continue;
            }

            if (crafted == 0) {
                return one.substring("FAIL:".length());
            }

            return "已成功合成 "
                    + crafted
                    + " 次；第 "
                    + (crafted + 1)
                    + " 次停止："
                    + one.substring("FAIL:".length());
        }

        ItemStack output = recipe.getRecipeOutput();

        return "SUCCESS:已真实执行配方："
                + output.getDisplayName()
                + " × "
                + output.stackSize
                + "，合成次数 "
                + crafted
                + "。";
    }

    private static String craftOne(
            EntityPlayer player,
            IRecipe recipe) {

        World world = player.worldObj;

        List<Object> specifications = getSpecifications(recipe);

        if (specifications == null) {
            return "FAIL:无法解析该IRecipe的输入材料。";
        }

        List<SourceStack> sources = collectSources(player);

        if (sources.isEmpty()) {
            return "FAIL:玩家背包和附近容器中没有可用材料。";
        }

        ItemStack[] selected = new ItemStack[specifications.size()];

        if (!solveRequirements(
                specifications,
                0,
                sources,
                selected)) {

            return "FAIL:玩家背包和附近容器中没有足够的真实材料。";
        }

        ItemStack resultPreview;

        try {
            resultPreview = buildAndValidate(
                    recipe,
                    world,
                    selected);

            if (resultPreview == null) {
                return "FAIL:材料虽然满足静态输入，但真实recipe.matches()拒绝了这个材料组合。";
            }
        } catch (Throwable t) {
            return "FAIL:真实配方验证异常："
                    + t.getClass().getSimpleName()
                    + " "
                    + String.valueOf(t.getMessage());
        }

        if (resultPreview.stackSize <= 0) {
            return "FAIL:真实配方返回了无效的输出数量。";
        }

        if (!canFitInPlayerInventory(
                player,
                resultPreview)) {

            return "FAIL:玩家背包没有足够空间接收合成产物。";
        }

        if (!removeSelectedMaterials(
                player,
                selected)) {

            return "FAIL:扣除材料时发现库存发生变化，已停止本次合成。";
        }

        ItemStack output = resultPreview.copy();

        if (!player.inventory.addItemStackToInventory(output)) {
            /*
             * 正常情况下 canFitInPlayerInventory() 已经保证这里成功。
             * 如果某个特殊IInventory实现仍然拒绝，则尽量回滚材料。
             */
            restoreMaterials(player, selected);
            return "FAIL:产物无法放入玩家背包，材料已尝试回滚。";
        }

        return "SUCCESS:" + output.getDisplayName();
    }

    private static ItemStack buildAndValidate(
            IRecipe recipe,
            World world,
            ItemStack[] selected) {

        InventoryCrafting grid = createGrid();

        if (recipe instanceof ShapedRecipes) {
            ShapedRecipes shaped =
                    (ShapedRecipes) recipe;

            for (int i = 0;
                    i < shaped.recipeItems.length
                            && i < GRID_SIZE;
                    i++) {

                if (shaped.recipeItems[i] != null) {
                    grid.setInventorySlotContents(
                            i,
                            selected[i].copy());
                }
            }
        } else if (recipe instanceof ShapedOreRecipe) {
            ShapedOreRecipe shaped =
                    (ShapedOreRecipe) recipe;

            Object[] input = shaped.getInput();

            for (int i = 0;
                    i < input.length
                            && i < GRID_SIZE;
                    i++) {

                if (input[i] != null) {
                    grid.setInventorySlotContents(
                            i,
                            selected[i].copy());
                }
            }
        } else {
            /*
             * Shapeless recipes may be placed anywhere in the grid.
             * The first N slots are sufficient.
             */
            for (int i = 0;
                    i < selected.length
                            && i < GRID_SIZE;
                    i++) {

                grid.setInventorySlotContents(
                        i,
                        selected[i].copy());
            }
        }

        if (!recipe.matches(grid, world)) {
            return null;
        }

        ItemStack result =
                recipe.getCraftingResult(grid);

        if (result == null) {
            return null;
        }

        return result.copy();
    }

    private static List<Object> getSpecifications(
            IRecipe recipe) {

        List<Object> result =
                new ArrayList<Object>();

        if (recipe instanceof ShapedRecipes) {
            ShapedRecipes shaped =
                    (ShapedRecipes) recipe;

            for (ItemStack stack : shaped.recipeItems) {
                if (stack != null) {
                    result.add(stack);
                } else {
                    /*
                     * Shaped recipe的空槽必须保留在selected中，
                     * 因为buildAndValidate需要保持原始位置。
                     */
                    result.add(null);
                }
            }

            return result;
        }

        if (recipe instanceof ShapedOreRecipe) {
            ShapedOreRecipe shaped =
                    (ShapedOreRecipe) recipe;

            Object[] input =
                    shaped.getInput();

            Collections.addAll(
                    result,
                    input);

            return result;
        }

        if (recipe instanceof ShapelessRecipes) {
            ShapelessRecipes shapeless =
                    (ShapelessRecipes) recipe;

            result.addAll(shapeless.recipeItems);

            return result;
        }

        if (recipe instanceof ShapelessOreRecipe) {
            ShapelessOreRecipe shapeless =
                    (ShapelessOreRecipe) recipe;

            result.addAll(shapeless.getInput());

            return result;
        }

        return null;
    }

    private static boolean solveRequirements(
            List<Object> specifications,
            int index,
            List<SourceStack> sources,
            ItemStack[] selected) {

        if (index >= specifications.size()) {
            return true;
        }

        Object specification =
                specifications.get(index);

        /*
         * Shaped配方中的空格不需要材料。
         */
        if (specification == null) {
            selected[index] = null;

            return solveRequirements(
                    specifications,
                    index + 1,
                    sources,
                    selected);
        }

        List<Integer> candidates =
                findCandidateIndexes(
                        specification,
                        sources);

        /*
         * 优先使用库存较少的候选项，
         * 降低OreDictionary多个替代材料造成的死路概率。
         */
        sortCandidatesByCount(
                candidates,
                sources);

        for (Integer candidateIndex :
                candidates) {

            SourceStack source =
                    sources.get(candidateIndex);

            if (source.remaining <= 0) {
                continue;
            }

            source.remaining--;

            selected[index] =
                    source.prototype.copy();

            if (solveRequirements(
                    specifications,
                    index + 1,
                    sources,
                    selected)) {

                return true;
            }

            source.remaining++;
            selected[index] = null;
        }

        return false;
    }

    private static List<Integer> findCandidateIndexes(
            Object specification,
            List<SourceStack> sources) {

        List<Integer> result =
                new ArrayList<Integer>();

        for (int i = 0;
                i < sources.size();
                i++) {

            SourceStack source =
                    sources.get(i);

            if (source.remaining <= 0) {
                continue;
            }

            if (matchesSpecification(
                    specification,
                    source.prototype)) {

                result.add(i);
            }
        }

        return result;
    }

    private static void sortCandidatesByCount(
            List<Integer> candidates,
            final List<SourceStack> sources) {

        Collections.sort(
                candidates,
                new java.util.Comparator<Integer>() {
                    @Override
                    public int compare(
                            Integer a,
                            Integer b) {

                        return sources.get(a).remaining
                                - sources.get(b).remaining;
                    }
                });
    }

    private static boolean matchesSpecification(
            Object specification,
            ItemStack actual) {

        if (actual == null
                || specification == null) {
            return false;
        }

        if (specification instanceof ItemStack) {
            return matchesItemStack(
                    (ItemStack) specification,
                    actual);
        }

        if (specification instanceof List) {
            List<?> alternatives =
                    (List<?>) specification;

            for (Object alternative :
                    alternatives) {

                if (alternative instanceof ItemStack
                        && matchesItemStack(
                        (ItemStack) alternative,
                        actual)) {

                    return true;
                }
            }
        }

        return false;
    }

    private static boolean matchesItemStack(
            ItemStack required,
            ItemStack actual) {

        if (required == null
                || actual == null
                || required.getItem()
                != actual.getItem()) {

            return false;
        }

        int requiredMeta =
                required.getMetadata();

        return requiredMeta == 32767
                || requiredMeta == actual.getMetadata();
    }

    private static List<SourceStack> collectSources(
            EntityPlayer player) {

        List<SourceStack> result =
                new ArrayList<SourceStack>();

        addInventorySources(
                player.inventory,
                result);

        if (!AIConfig.storageEnabled) {
            return result;
        }

        List<IInventory> inventories =
                findNearbyInventories(player);

        for (IInventory inventory :
                inventories) {

            addInventorySources(
                    inventory,
                    result);
        }

        return result;
    }

    private static void addInventorySources(
            IInventory inventory,
            List<SourceStack> result) {

        if (inventory == null) {
            return;
        }

        for (int slot = 0;
                slot < inventory.getSizeInventory();
                slot++) {

            ItemStack stack;

            try {
                stack =
                        inventory.getStackInSlot(slot);
            } catch (Throwable ignored) {
                continue;
            }

            if (stack == null
                    || stack.stackSize <= 0
                    || stack.getItem() == null) {

                continue;
            }

            addOrMergeSource(
                    result,
                    stack);
        }
    }

    private static void addOrMergeSource(
            List<SourceStack> result,
            ItemStack stack) {

        for (SourceStack source :
                result) {

            if (source.prototype.isItemEqual(stack)) {
                source.remaining += stack.stackSize;
                return;
            }
        }

        result.add(
                new SourceStack(
                        stack.copy(),
                        stack.stackSize));
    }

    private static boolean removeSelectedMaterials(
            EntityPlayer player,
            ItemStack[] selected) {

        List<IInventory> inventories =
                new ArrayList<IInventory>();

        inventories.add(player.inventory);

        if (AIConfig.storageEnabled) {
            inventories.addAll(
                    findNearbyInventories(player));
        }

        /*
         * 先确认所有材料仍然存在。
         * 只有确认全部存在后才真正修改库存。
         */
        for (ItemStack required : selected) {
            if (required == null) {
                continue;
            }

            if (!containsAndCount(
                    inventories,
                    required,
                    1)) {

                return false;
            }
        }

        /*
         * 真正扣除。
         */
        for (ItemStack required : selected) {
            if (required == null) {
                continue;
            }

            if (!removeOne(
                    inventories,
                    required)) {

                /*
                 * 这个分支理论上不应发生。
                 * 上面的预检查和这里之间均在主线程执行。
                 */
                return false;
            }
        }

        return true;
    }

    private static boolean containsAndCount(
            List<IInventory> inventories,
            ItemStack required,
            int amount) {

        int found = 0;

        for (IInventory inventory :
                inventories) {

            for (int slot = 0;
                    slot < inventory.getSizeInventory();
                    slot++) {

                ItemStack stack;

                try {
                    stack =
                            inventory.getStackInSlot(slot);
                } catch (Throwable ignored) {
                    continue;
                }

                if (stack == null
                        || !matchesItemStack(
                        required,
                        stack)) {

                    continue;
                }

                found += stack.stackSize;

                if (found >= amount) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean removeOne(
            List<IInventory> inventories,
            ItemStack required) {

        for (IInventory inventory :
                inventories) {

            for (int slot = 0;
                    slot < inventory.getSizeInventory();
                    slot++) {

                ItemStack stack;

                try {
                    stack =
                            inventory.getStackInSlot(slot);
                } catch (Throwable ignored) {
                    continue;
                }

                if (stack == null
                        || !matchesItemStack(
                        required,
                        stack)) {

                    continue;
                }

                ItemStack removed;

                try {
                    removed =
                            inventory.decrStackSize(
                                    slot,
                                    1);
                } catch (Throwable ignored) {
                    continue;
                }

                if (removed == null
                        || removed.stackSize <= 0) {

                    continue;
                }

                inventory.markDirty();
                return true;
            }
        }

        return false;
    }

    private static boolean canFitInPlayerInventory(
            EntityPlayer player,
            ItemStack output) {

        if (output == null
                || output.stackSize <= 0) {

            return false;
        }

        int remaining =
                output.stackSize;

        for (int slot = 0;
                slot < player.inventory.getSizeInventory();
                slot++) {

            ItemStack existing =
                    player.inventory.getStackInSlot(slot);

            if (existing == null) {
                remaining -= Math.min(
                        output.getMaxStackSize(),
                        remaining);
            } else if (existing.isItemEqual(output)) {
                int max = Math.min(
                        existing.getMaxStackSize(),
                        player.inventory.getInventoryStackLimit());

                int free =
                        Math.max(
                                0,
                                max - existing.stackSize);

                remaining -= Math.min(
                        free,
                        remaining);
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }

    private static void restoreMaterials(
            EntityPlayer player,
            ItemStack[] selected) {

        for (ItemStack stack :
                selected) {

            if (stack == null) {
                continue;
            }

            ItemStack restore =
                    stack.copy();

            restore.stackSize = 1;

            if (!player.inventory
                    .addItemStackToInventory(
                            restore)) {

                /*
                 * 极端情况下玩家背包在执行期间变满。
                 * 不凭空删除材料；掉落到玩家当前位置。
                 */
                player.entityDropItem(
                        restore,
                        0.0F);
            }
        }
    }

    private static List<IInventory> findNearbyInventories(
            EntityPlayer player) {

        List<IInventory> result =
                new ArrayList<IInventory>();

        if (player == null
                || player.worldObj == null) {

            return result;
        }

        int radius =
                Math.max(
                        1,
                        AIConfig.storageRadius);

        int x0 =
                (int)Math.floor(player.posX);

        int y0 =
                (int)Math.floor(player.posY);

        int z0 =
                (int)Math.floor(player.posZ);

        for (int x = x0 - radius;
                x <= x0 + radius;
                x++) {

            for (int y =
                         Math.max(0, y0 - radius);
                    y <= y0 + radius;
                    y++) {

                for (int z = z0 - radius;
                        z <= z0 + radius;
                        z++) {

                    TileEntity tile;

                    try {
                        tile =
                                player.worldObj.getTileEntity(
                                        x,
                                        y,
                                        z);
                    } catch (Throwable ignored) {
                        continue;
                    }

                    if (!(tile instanceof IInventory)) {
                        continue;
                    }

                    IInventory inventory =
                            (IInventory) tile;

                    if (!result.contains(
                            inventory)) {

                        result.add(inventory);
                    }

                    if (result.size()
                            >= AIConfig.storageMaxContainers) {

                        return result;
                    }
                }
            }
        }

        return result;
    }

    private static boolean isSupportedRecipe(
            IRecipe recipe) {

        return recipe instanceof ShapedRecipes
                || recipe instanceof ShapelessRecipes
                || recipe instanceof ShapedOreRecipe
                || recipe instanceof ShapelessOreRecipe;
    }

    private static IRecipe findRecipe(
            String query) {

        String q =
                query.toLowerCase();

        List<?> recipes =
                CraftingManager.getInstance()
                        .getRecipeList();

        for (Object object :
                recipes) {

            if (!(object instanceof IRecipe)) {
                continue;
            }

            IRecipe recipe =
                    (IRecipe)object;

            ItemStack output;

            try {
                output =
                        recipe.getRecipeOutput();
            } catch (Throwable ignored) {
                continue;
            }

            if (output == null) {
                continue;
            }

            String display =
                    output.getDisplayName();

            String unlocalized =
                    output.getUnlocalizedName();

            if ((display != null
                    && display.toLowerCase()
                    .contains(q))
                    || (unlocalized != null
                    && unlocalized.toLowerCase()
                    .contains(q))) {

                return recipe;
            }
        }

        return null;
    }

    private static InventoryCrafting createGrid() {

        return new InventoryCrafting(
                new Container() {
                    @Override
                    public boolean canInteractWith(
                            EntityPlayer player) {

                        return false;
                    }

                    @Override
                    public ItemStack transferStackInSlot(
                            EntityPlayer player,
                            int slot) {

                        return null;
                    }
                },
                3,
                3);
    }

    private static String extract(
            String json,
            String key) {

        if (json == null) {
            return "";
        }

        String token =
                "\"" + key + "\"";

        int keyPos =
                json.indexOf(token);

        if (keyPos < 0) {
            return "";
        }

        int colon =
                json.indexOf(
                        ':',
                        keyPos + token.length());

        if (colon < 0) {
            return "";
        }

        int firstQuote =
                json.indexOf(
                        '"',
                        colon + 1);

        if (firstQuote < 0) {
            return "";
        }

        int secondQuote =
                json.indexOf(
                        '"',
                        firstQuote + 1);

        if (secondQuote <= firstQuote) {
            return "";
        }

        return json.substring(
                firstQuote + 1,
                secondQuote).trim();
    }

    private static int parseAmount(
            String json) {

        if (json == null) {
            return 1;
        }

        String token =
                "\"amount\"";

        int p =
                json.indexOf(token);

        if (p < 0) {
            return 1;
        }

        int colon =
                json.indexOf(
                        ':',
                        p + token.length());

        if (colon < 0) {
            return 1;
        }

        int end =
                colon + 1;

        while (end < json.length()
                && Character.isWhitespace(
                json.charAt(end))) {

            end++;
        }

        int start = end;

        while (end < json.length()
                && Character.isDigit(
                json.charAt(end))) {

            end++;
        }

        if (end == start) {
            return 1;
        }

        try {
            return Integer.parseInt(
                    json.substring(
                            start,
                            end));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static final class SourceStack {

        private final ItemStack prototype;
        private int remaining;

        private SourceStack(
                ItemStack prototype,
                int remaining) {

            this.prototype =
                    prototype;

            this.remaining =
                    remaining;
        }
    }
}
