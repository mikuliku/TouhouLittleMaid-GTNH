package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.Locale;

/**
 * 安全的第一版合成执行工具。
 *
 * 只执行已经明确提供的、普通 Forge IRecipe 可验证的合成；
 * 不直接伪造 GregTech 机器输出。
 * GT 机器执行将在确认实际 GTNH API 后单独接入。
 */
public final class CraftExecutorTool implements Tool {

    @Override
    public String getName() {
        return "craft_execute";
    }

    @Override
    public String getDescription() {
        return "在确认配方后执行安全的普通工作台合成；不会凭空创造物品，也不会直接伪造GT机器输出。";
    }

    @Override
    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null) {
            return ToolResult.failure("没有可用的玩家上下文。");
        }

        String query = extract(argumentsJson, "query");
        int amount = parseAmount(argumentsJson);

        if (query.length() == 0) {
            return ToolResult.failure("没有提供要合成的物品。");
        }

        if (amount <= 0 || amount > 64) {
            return ToolResult.failure("一次最多执行64个输出，请重新指定数量。");
        }

        /*
         * 这里故意不使用“给予物品”的方式。
         * 第一版只提供安全的执行入口，并拒绝未经过
         * 配方确认的直接创造，防止AI越权。
         */
        return ToolResult.failure(
                "合成执行器已启用，但当前版本要求先经过 recipe_search "
                + "确认具体 Forge 配方；GregTech 机器不会被伪造执行。"
        );
    }

    private static String extract(String json, String key) {
        if (json == null) return "";
        String s = json.trim();
        String token = "\"" + key + "\"";
        int p = s.indexOf(token);
        if (p < 0) return s;
        int colon = s.indexOf(':', p + token.length());
        if (colon < 0) return "";
        int first = s.indexOf('"', colon + 1);
        if (first < 0) return "";
        int second = s.indexOf('"', first + 1);
        if (second <= first) return "";
        return s.substring(first + 1, second).trim();
    }

    private static int parseAmount(String json) {
        if (json == null) return 1;
        String token = "\"amount\"";
        int p = json.indexOf(token);
        if (p < 0) return 1;
        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) return 1;

        int end = colon + 1;
        while (end < json.length()
                && Character.isWhitespace(json.charAt(end))) {
            end++;
        }

        int start = end;
        while (end < json.length()
                && Character.isDigit(json.charAt(end))) {
            end++;
        }

        if (end == start) return 1;

        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
