package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolRegistry {
    private static final Map<String, Tool> TOOLS =
            new LinkedHashMap<String, Tool>();

    private ToolRegistry() {}

    public static synchronized void register(Tool tool) {
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("Tool cannot be null.");
        }
        TOOLS.put(tool.getName(), tool);
    }

    public static synchronized Tool get(String name) {
        return TOOLS.get(name);
    }

    public static synchronized Collection<Tool> all() {
        return TOOLS.values();
    }

    public static synchronized String describeTools() {
        StringBuilder result = new StringBuilder();

        for (Tool tool : TOOLS.values()) {
            result.append("- ")
                    .append(tool.getName())
                    .append(": ")
                    .append(tool.getDescription())
                    .append('\n');
        }

        return result.toString();
    }
}
