package com.xowl.spending.bot.handler;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;

@Getter
public class TelegramUpdateResponse {
    @SerializedName("ok")
    private boolean ok;
    @SerializedName("result")
    private List<Update> result;

    @Getter
    public static class Update {
        @SerializedName("update_id")
        private long updateId;
        @SerializedName("message")
        private Message message;
    }

    @Getter
    public static class Message {
        @SerializedName("text")
        private String text;
        @SerializedName("chat")
        private Chat chat;
    }

    @Getter
    public static class Chat {
        @SerializedName("id")
        private long id;
    }
}
