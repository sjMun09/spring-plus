package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

/*
QueryDSL을 사용할 메서드를 정의
 */
public interface TodoRepositoryQueryDsl {
    Optional<Todo> findByIdWithUser(Long todoId);

    Page<Todo> searchTodos(String weather, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
