package org.example.expert.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class SignupResponse {

    private final String bearerToken;
    private final String nickname;

    public SignupResponse(String bearerToken, String nickname) {
        this.bearerToken = bearerToken;
        this.nickname = nickname;
    }
}
