package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
현재 기준에서, @SpringBootTest + @DataJpaTest + Testcontainers
방식이 가장 많이 쓰이는 것 같아서 해당 방식으로 line 별로 테스트 코드 구현하고 싶었지만 어려움.
그래서 그냥 모키토 빈쓰고 목 사용해서 가짜 데이터로 테스트 해봄.
 */
@SpringBootTest
@Transactional
public class TodoServiceTest {

    private TodoService todoService;

    @MockitoBean
    private TodoRepository todoRepository;

    @MockitoBean
    private WeatherClient weatherClient;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository, weatherClient);
    }

    @Test
    @DisplayName("할 일 저장 시 User 반환, 날씨 정보, todo 생성, db 저장을 정상적으로 수행해야 합니다.")
    void saveTodo_성공() {
        // given
        AuthUser authUser = new AuthUser(1L, "test@user.com", "nickname", UserRole.USER); // ✅ 닉네임 추가
        TodoSaveRequest request = new TodoSaveRequest("할 일 title", "할 일 내용");
        String expectedWeather = "맑음";

        // Mock 설정
        when(weatherClient.getTodayWeather()).thenReturn(expectedWeather);
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            //ID 포함된 todos 객체 반환
            Todo savedTodo = new Todo(todo.getTitle(), todo.getContents(), todo.getWeather(), todo.getUser());
            return setTodoId(savedTodo, 1L);
        });

        // when
        TodoSaveResponse response = todoService.saveTodo(authUser, request);

        // then
        User expectedUser = User.fromAuthUser(authUser);

        //필드 값만 비교 (객체 주소 무시)
        assertThat(response.getUser())
                .usingRecursiveComparison()
                .isEqualTo(new UserResponse(expectedUser.getId(), expectedUser.getEmail()));

        // 날씨 정보 가져오는 거 검증
        verify(weatherClient, times(1)).getTodayWeather();

        // 투두 생성 확인
        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository, times(1)).save(todoCaptor.capture());
        Todo savedTodo = todoCaptor.getValue();

        assertThat(savedTodo.getTitle()).isEqualTo(request.getTitle());
        assertThat(savedTodo.getContents()).isEqualTo(request.getContents());
        assertThat(savedTodo.getWeather()).isEqualTo(expectedWeather);
        assertThat(savedTodo.getUser())
                .usingRecursiveComparison()
                .isEqualTo(expectedUser);

        // 응답 객체 검증
        assertThat(response.getId()).isNotNull();
        assertThat(response.getTitle()).isEqualTo(request.getTitle());
        assertThat(response.getContents()).isEqualTo(request.getContents());
        assertThat(response.getWeather()).isEqualTo(expectedWeather);
    }

    // 리플렉션을 활용한 id 강제 설정
    private Todo setTodoId(Todo todo, Long id) {
        try {
            java.lang.reflect.Field idField = Todo.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(todo, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
        return todo;
    }

    @Test
    @DisplayName("사용자의 할 일 목록을 최신순으로 조회할 수 있다.")
    void getTodos_성공() {
        // given
        long userId = 1L;
        int page = 1;
        int size = 10;
        PageRequest pageable = PageRequest.of(page - 1, size);

        AuthUser authUser = new AuthUser(userId, "test@user.com", "nickname", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        List<Todo> todos = List.of(
                new Todo("할 일1", "내용1", "Sunny", user),
                new Todo("할 일2", "내용2", "Rainy", user)
        );

        Page<Todo> todoPage = new PageImpl<>(todos, pageable, todos.size());

        // Mock 설정
        when(todoRepository.findAllByUserIdOrderByModifiedAtDesc(userId, pageable)).thenReturn(todoPage);

        // when
        Page<TodoResponse> response = todoService.getTodos(userId, page, size);

        // then
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getTitle()).isEqualTo("할 일1");
        assertThat(response.getContent().get(1).getTitle()).isEqualTo("할 일2");

        verify(todoRepository, times(1)).findAllByUserIdOrderByModifiedAtDesc(userId, pageable);
    }


}
