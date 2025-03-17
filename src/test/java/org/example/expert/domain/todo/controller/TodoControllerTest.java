package org.example.expert.domain.todo.controller;

import org.example.expert.config.JwtUtil;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TodoService todoService;


    @Test
    void todo_목록_조회_성공() throws Exception {
        // given
        long userId = 1L;
        int page = 1;
        int size = 10;

        AuthUser authUser = new AuthUser(userId, "email", "nickname", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail());

        List<TodoResponse> responseList = List.of(
                new TodoResponse(1L, "title1", "contents1", "Sunny", userResponse, LocalDateTime.now(), LocalDateTime.now()),
                new TodoResponse(2L, "title2", "contents2", "Rainy", userResponse, LocalDateTime.now(), LocalDateTime.now())
        );

        // when
        String jwtToken = "Bearer MOCK_JWT_TOKEN";
        when(todoService.getTodos(userId, page, size)).thenReturn(new org.springframework.data.domain.PageImpl<>(responseList));

        // then
        mockMvc.perform(get("/todos")
                        .header("Authorization", jwtToken)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[0].title").value("title1"))
                .andExpect(jsonPath("$.content[1].title").value("title2"));
    }

    // @WithMockUser(username = "testUser", roles = {"USER"}) 이건 시큐리티 쓰는 방법이고, 아래
    // 시큐리티 안 써서 이렇게 테스트에서도 필터를 통과할 쑤 있도록 Authorization 헤더를 수동으로 추가해줘야함
    @Test
    void todo_단건_조회에_성공한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 1L;
        String title = "title";

        AuthUser authUser = new AuthUser(userId, "email", "nickname", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail());

        TodoResponse response = new TodoResponse(
                todoId,
                title,
                "contents",
                "Sunny",
                userResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        String jwtToken = "Bearer MOCK_JWT_TOKEN";
        when(jwtUtil.createToken(userId, "email", "nickname", UserRole.USER)).thenReturn("MOCK_JWT_TOKEN");
        when(todoService.getTodo(userId, todoId)).thenReturn(response);


        // then
        mockMvc.perform(get("/todos/{todoId}", todoId)
                        .header("Authorization", jwtToken)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다() throws Exception {
        // given
        long userId = 1L;
        long todoId = 1L;

        // when
        String jwtToken = "Bearer MOCK_JWT_TOKEN";
        when(jwtUtil.createToken(userId, "email", "nickname", UserRole.USER)).thenReturn("MOCK_JWT_TOKEN");
        when(todoService.getTodo(userId, todoId))
                .thenThrow(new InvalidRequestException("일정을 찾을 수 없습니다."));

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId)
                        .header("Authorization", jwtToken)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.name()))
                .andExpect(jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.message").value("일정을 찾을 수 없습니다."));
    }
}
