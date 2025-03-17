package org.example.expert.domain.todo.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TodoRepositoryQueryDslImpl implements TodoRepositoryQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        QTodo todo = QTodo.todo;
        QUser user = QUser.user;

        Todo result = queryFactory
                .selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<Todo> searchTodos(Long userId, String weather, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        QTodo todo = QTodo.todo;

        List<Todo> results = queryFactory
                .selectFrom(todo)
                .where(
                        todo.user.id.eq(userId),
                        weatherEq(weather),
                        startDateGoe(startDate),
                        endDateLoe(endDate)
                )
                .orderBy(todo.modifiedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(todo.count())
                .from(todo)
                .where(
                        todo.user.id.eq(userId),
                        weatherEq(weather),
                        startDateGoe(startDate),
                        endDateLoe(endDate)
                )
                .fetchOne();

        return PageableExecutionUtils.getPage(results, pageable, () -> total);
    }

    private BooleanExpression weatherEq(String weather) {
        return weather != null ? QTodo.todo.weather.eq(weather) : null;
    }

    private BooleanExpression startDateGoe(LocalDateTime startDate) {
        return startDate != null ? QTodo.todo.modifiedAt.goe(startDate) : null;
    }

    private BooleanExpression endDateLoe(LocalDateTime endDate) {
        return endDate != null ? QTodo.todo.modifiedAt.loe(endDate) : null;
    }

    @Override
    public Page<Todo> findAllByUserIdOrderByModifiedAtDesc(Long userId, Pageable pageable) {
        QTodo todo = QTodo.todo;

        List<Todo> results = queryFactory
                .selectFrom(todo)
                .where(todo.user.id.eq(userId)) // userId 필터 추가
                .orderBy(todo.modifiedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(todo.count())
                .from(todo)
                .where(todo.user.id.eq(userId))
                .fetchOne();

        return PageableExecutionUtils.getPage(results, pageable, () -> total);
    }
}
