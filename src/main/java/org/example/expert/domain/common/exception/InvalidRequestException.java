package org.example.expert.domain.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// @ResponseStatus(HttpStatus.NOT_FOUND) //기본 상태 코드 404 -> 컨트롤러에 커스터마이징 했기 때문에 안 써도 됨.
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
