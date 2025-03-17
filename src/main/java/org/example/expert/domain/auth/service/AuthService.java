package org.example.expert.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {
        log.info("회원가입 요청, Email: {}, Nickname: {}, Role: {}",
                signupRequest.getEmail(), signupRequest.getNickname(), signupRequest.getUserRole());

        if (signupRequest.getPassword() == null || signupRequest.getPassword().isBlank()) {
            throw new InvalidRequestException("비밀번호를 입력해주세요.");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new InvalidRequestException("이미 존재하는 이메일입니다.");
        }

        log.info("입력된 원본 비밀번호: {}", signupRequest.getPassword());

        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        log.info("암호화된 비번: {}", encodedPassword);

        UserRole userRole = UserRole.of(signupRequest.getUserRole());

        User newUser = new User(
                signupRequest.getEmail(),
                encodedPassword,
                signupRequest.getNickname(),
                userRole
        );

        log.info("생성된 유저 객체: {}", newUser);
        User savedUser = userRepository.save(newUser);

        log.info("저장된 유저 객체 정보 - Id: {}, Email:{}, Password: {}, Nickname: {}",
                savedUser.getId(),savedUser.getEmail(),savedUser.getPassword(),savedUser.getNickname());

        String bearerToken = jwtUtil.createToken(savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getNickname(),
                userRole);

        log.info("jwt 토큰: {}", bearerToken);

        return new SignupResponse(bearerToken, savedUser.getNickname());
    }

    @Transactional
    public SigninResponse signin(SigninRequest signinRequest) {
        User user = userRepository.findByEmail(signinRequest.getEmail()).orElseThrow(
                () -> new InvalidRequestException("가입되지 않은 유저입니다."));

        // 로그인 시 이메일과 비밀번호가 일치하지 않을 경우 401을 반환합니다.
        if (!passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())) {
            throw new AuthException("잘못된 비밀번호입니다.");
        }

        String bearerToken = jwtUtil.createToken(user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getUserRole());

        return new SigninResponse(bearerToken, user.getNickname());
    }
}
