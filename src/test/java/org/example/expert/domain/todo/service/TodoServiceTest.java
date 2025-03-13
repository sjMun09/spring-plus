package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
현재 기준에서, @SpringBootTest + @DataJpaTest + Testcontainers
방식이 가장 많이 쓰이는 것 같아서 해당 방식으로 line 별로 테스트 코드 구현.
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
        AuthUser authUser = new AuthUser(1L, "test@user.com", UserRole.USER);
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

    // 리플렉션을 활용한 ID 강제 설정
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
}
