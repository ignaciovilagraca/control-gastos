package com.xowl.spending.frameworks.telegram;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class TelegramErrorResponse {
    @SerializedName("ok")
    private Boolean status;
    @SerializedName("error_code")
    private Integer errorCode;
    @SerializedName("description")
    private String description;
}
