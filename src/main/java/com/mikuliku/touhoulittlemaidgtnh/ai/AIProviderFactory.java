package com.mikuliku.touhoulittlemaidgtnh.ai;

public final class AIProviderFactory {
    private AIProviderFactory() {}

    public static AIProvider create() {
        return new OpenAICompatibleProvider(
                AIConfig.apiUrl,
                AIConfig.apiKey,
                AIConfig.model
        );
    }
}
