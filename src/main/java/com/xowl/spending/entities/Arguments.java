package com.xowl.spending.entities;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Arguments {
    private Authentication authentication;
    private BotToken gastosBotToken;
    private BotToken errorsBotToken;
}
