package org.example.expert.domain.user.dto.response;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
public class UserResponse {

    private final Long id;
    private final String email;

    public UserResponse(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    /*
    @EqualsAndHashCode를 추가햇음에도
    객체가 동일하지 않아서, 테스트 코드에서 문제가 발생중.
    해결하기 위해 필드 값 문제를 확인해보려함.
     */
    @Override
    public String toString() {
        return "UserResponse{id=" + id + ", email='" + email + "'}";
    }
}
