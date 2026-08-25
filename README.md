# TouhouLittleMaid-GTNH

Minecraft 1.7.10 / Forge 10.13.4.1614 / GTNH oriented lightweight maid project.

## Current milestone: 0.1

- Maid entity named **酒狐**
- Basic tamable/follow-owner foundation
- AI configuration file
- OpenAI-compatible chat endpoint
- Basic chat manager

## Config

After first launch:

`config/touhoulittlemaidgtnh.cfg`

Example:

```ini
AI {
    B:enabled=true
    S:apiKey=YOUR_API_KEY
    S:apiUrl=https://api.openai.com/v1/chat/completions
    S:model=gpt-4o-mini
    D:temperature=0.7
    I:maxTokens=512
}
```

Do NOT commit your API key.

## Planned milestones

0.2 - safe main-thread AI callbacks and chat trigger
0.3 - game-context Tool API
0.4 - GT5U recipe resolver
0.5 - production-chain planner
0.6 - crafting execution
0.7 - GTNH machine adapters
