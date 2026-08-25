package com.mikuliku.touhoulittlemaidgtnh.ai;

public final class ChatMessage {
    public final String role;
    public final String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
