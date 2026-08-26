package com.mikuliku.touhoulittlemaidgtnh.ai;

import com.mikuliku.touhoulittlemaidgtnh.ai.tools.CraftExecutorTool;
import com.mikuliku.touhoulittlemaidgtnh.ai.tools.MaterialCheckTool;
import com.mikuliku.touhoulittlemaidgtnh.ai.tools.RecipeSearchTool;
import com.mikuliku.touhoulittlemaidgtnh.ai.tools.StorageContainerTool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolRegistry {

    private static final Map<String, Tool> TOOLS =
            new LinkedHashMap<String, Tool>();

    private static boolean defaultsRegistered = false;

    private ToolRegistry() {}

    public static synchronized void register(Tool tool) {
        if (tool == null || tool.getName() == null) {
            throw new IllegalArgumentException("Tool cannot be null.");
        }

        TOOLS.put(tool.getName(), tool);
    }

    public static synchronized void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }

        register(new RecipeSearchTool());
        register(new CraftExecutorTool());
        register(new StorageContainerTool());
        register(new MaterialCheckTool());

        defaultsRegistered = true;
    }

    public static synchronized Tool get(String name) {
        registerDefaults();
        return TOOLS.get(name);
    }

    public static synchronized Collection<Tool> all() {
        registerDefaults();
        return TOOLS.values();
    }

    public static synchronized String describeTools() {
        registerDefaults();

        StringBuilder result =
                new StringBuilder();

        for (Tool tool : TOOLS.values()) {
            result.append("- ")
                    .append(tool.getName())
                    .append(": ")
                    .append(tool.getDescription())
                    .append('
');
        }

        return result.toString();
    }
}
