package com.xowl.spending.frameworks.telegram;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TelegramMessage {

    @SerializedName("text")
    private String text;

    @SerializedName("chat_id")
    private String chatId;

    @SerializedName("parse_mode")
    private String parseMode;
}
