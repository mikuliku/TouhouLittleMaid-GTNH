package com.mikuliku.touhoulittlemaidgtnh.ai;

public interface AIProvider {
    String chat(String systemPrompt, String userMessage) throws Exception;
}
