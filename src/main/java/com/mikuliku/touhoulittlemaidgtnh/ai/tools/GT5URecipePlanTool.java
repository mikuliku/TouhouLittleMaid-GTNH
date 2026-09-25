package com.mikuliku.touhoulittlemaidgtnh.ai.tools;

import com.mikuliku.touhoulittlemaidgtnh.ai.Tool;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolContext;
import com.mikuliku.touhoulittlemaidgtnh.ai.ToolResult;

import java.util.List;

/**
 * GT5U 真实配方规划工具。
 *
 * 只读取 GTRecipe，不移动材料，也不启动机器。
 * 让 AI 在执行前先看到真实 GTNH 配方、所属 RecipeMap、EU/t 和时间。
 */
public final class GT5URecipePlanTool implements Tool {

    @Override
    public String getName() {
        return "gt5u_recipe_plan";
    }

    @Override
    public String getDescription() {
        return "查询真实GT5U RecipeMap 配方并生成执行前计划。参数：{\"query\":\"物品名称\"}。只读，不移动材料。";
    }

    @Override
    public ToolResult execute(ToolContext context, String argumentsJson) {
        String query = getString(argumentsJson, "query");

        if (query.length() == 0) {
            return ToolResult.failure("没有指定要查询的物品。参数示例：{\"query\":\"铝板\"}");
        }

        List<GT5URecipeAdapter.RecipeMatch> matches =
                GT5URecipeAdapter.findMatches(query);

        if (matches.isEmpty()) {
            return ToolResult.failure(
                    "没有在当前加载的GT5U RecipeMap 中找到与“"
                            + query + "”匹配的真实配方。");
        }

        StringBuilder result = new StringBuilder();
        result.append("找到 ")
                .append(matches.size())
                .append(" 个真实GT5U配方：\n");

        int index = 1;
        for (GT5URecipeAdapter.RecipeMatch match : matches) {
            result.append(index++)
                    .append(". ")
                    .append(match.describe())
                    .append('\n');
        }

        result.append("下一步：先检查/转移材料，再使用gt5u_machine_execute。机器不会由本工具启动。");
        return ToolResult.success(result.toString());
    }

    private static String getString(String json, String key) {
        if (json == null) {
            return "";
        }

        String token = "\"" + key + "\"";
        int p = json.indexOf(token);
        if (p < 0) {
            return "";
        }

        int colon = json.indexOf(':', p + token.length());
        if (colon < 0) {
            return "";
        }

        int first = json.indexOf('"', colon + 1);
        if (first < 0) {
            return "";
        }

        int second = json.indexOf('"', first + 1);
        if (second <= first) {
            return "";
        }

        return json.substring(first + 1, second).trim();
    }
}
