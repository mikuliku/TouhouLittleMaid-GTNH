package com.mikuliku.touhoulittlemaidgtnh.ai;

public final class ToolResult {
    public final boolean success;
    public final String content;

    private ToolResult(boolean success, String content) {
        this.success = success;
        this.content = content;
    }

    public static ToolResult success(String content) {
        return new ToolResult(true, content);
    }

    public static ToolResult failure(String content) {
        return new ToolResult(false, content);
    }
}
