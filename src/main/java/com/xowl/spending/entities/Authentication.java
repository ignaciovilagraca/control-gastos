package com.xowl.spending.entities;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Authentication {
    private String documentNumber;
    private String password;
    private String nickname;
}
