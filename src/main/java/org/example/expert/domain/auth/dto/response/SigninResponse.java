package org.example.expert.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class SigninResponse {

    private final String bearerToken;
    private final String nickname;

    public SigninResponse(String bearerToken, String nickname) {
        this.bearerToken = bearerToken;
        this.nickname = nickname;
    }
}
