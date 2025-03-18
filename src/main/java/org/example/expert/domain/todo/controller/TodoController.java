package org.example.expert.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;

    @PostMapping("/todos")
    public ResponseEntity<TodoSaveResponse> saveTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TodoSaveRequest todoSaveRequest
    ) {
        log.debug("TodoController : {}",authUser);
        return ResponseEntity.ok(todoService.saveTodo(authUser, todoSaveRequest));
    }

    // 참고해서 검색 부분 구현.
    @GetMapping("/todos")
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(name ="page",defaultValue = "1") int page,
            @RequestParam(name = "size",defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(todoService.getTodos(authUser.getId(), page, size));
    }

    @GetMapping("/todos/{todoId}")
    public ResponseEntity<TodoResponse> getTodo(@AuthenticationPrincipal AuthUser authUser,@PathVariable(name = "todoId") long todoId) {
        return ResponseEntity.ok(todoService.getTodo(authUser.getId(), todoId));
    }

    /*
    404 에러 설정(커스터마이징)
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequestException(InvalidRequestException ex) {
        Map<String, Object> response = Map.of(
                "status", HttpStatus.NOT_FOUND.name(),
                "code", HttpStatus.NOT_FOUND.value(),
                "message", ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 검색
    @GetMapping("/todos/search")
    public ResponseEntity<Page<TodoResponse>> searchTodos(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String weather,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(todoService.searchTodos(authUser.getId(), weather, startDate, endDate, page, size));
    }
}
