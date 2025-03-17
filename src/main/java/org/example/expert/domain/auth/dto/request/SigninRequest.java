package org.example.expert.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SigninRequest {

    @NotBlank(message = "이메일 입력해주세요")
    @Email
    private String email;

    @NotBlank(message = "비번 일벽해주세요")
    private String password;

    @NotBlank(message = "닉네임 입력해주세요")
    @Size(min = 2, max = 8)
    private String nickname;
}
