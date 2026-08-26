package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

/**
 * 普通合成工具入口。
 *
 * 实际执行统一交给 RecipeSafeExecutorTool，避免两个执行器
 * 各自实现一套可能不一致的材料扣除逻辑。
 */
public final class CraftExecutorTool implements Tool {

    private final RecipeSafeExecutorTool safeExecutor =
            new RecipeSafeExecutorTool();

    @Override
    public String getName() {
        return "craft_execute";
    }

    @Override
    public String getDescription() {
        return "执行真实Forge 1.7.10普通合成；会检查真实配方、玩家背包和附近容器，"
                + "必要时自动取料，不凭空创造物品，也不伪造GregTech机器输出。";
    }

    @Override
    public ToolResult execute(
            ToolContext context,
            String argumentsJson) {

        return safeExecutor.execute(
                context,
                argumentsJson);
    }
}
