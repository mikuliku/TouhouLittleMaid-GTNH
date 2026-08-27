package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

public final class GT5UMachineExecutorTool implements Tool {
    public String getName() { return "gt5u_machine_execute"; }

    public String getDescription() {
        return "Validate a planned GT5U operation. Automatic item/fluid insertion is disabled in this stage.";
    }

    public ToolResult execute(ToolContext context, String argumentsJson) {
        if (context == null || context.getPlayer() == null)
            return ToolResult.failure("No player context.");

        return ToolResult.success(
            "GT5U execution bridge is ready in validation mode. " +
            "No items or fluids were moved and no machine was started.");
    }
}
