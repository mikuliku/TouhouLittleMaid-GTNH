package com.mikuliku.touhoulittlemaidgtnh.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ConversationMemory {
    private static final Map<UUID, List<ChatMessage>> MEMORY =
            new HashMap<UUID, List<ChatMessage>>();

    private ConversationMemory() {}

    public static synchronized void add(UUID playerId, String role, String content) {
        List<ChatMessage> history = MEMORY.get(playerId);
        if (history == null) {
            history = new ArrayList<ChatMessage>();
            MEMORY.put(playerId, history);
        }

        history.add(new ChatMessage(role, content));

        int max = Math.max(2, AIConfig.memoryTurns * 2);
        while (history.size() > max) {
            history.remove(0);
        }
    }

    public static synchronized List<ChatMessage> get(UUID playerId) {
        List<ChatMessage> source = MEMORY.get(playerId);
        List<ChatMessage> result = new ArrayList<ChatMessage>();

        if (source != null) {
            result.addAll(source);
        }

        return result;
    }

    public static synchronized void clear(UUID playerId) {
        MEMORY.remove(playerId);
    }
}
