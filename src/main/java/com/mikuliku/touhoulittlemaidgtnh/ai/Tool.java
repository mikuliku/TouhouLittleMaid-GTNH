package com.mikuliku.touhoulittlemaidgtnh.ai;

public interface Tool {
    String getName();
    String getDescription();
    ToolResult execute(ToolContext context, String argumentsJson);
}
