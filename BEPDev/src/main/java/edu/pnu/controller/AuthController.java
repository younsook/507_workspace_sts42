package edu.pnu.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.auth.LoginRequest;
import edu.pnu.dto.auth.RegisterRequest;
import edu.pnu.dto.auth.RegisterResponse;
import edu.pnu.dto.common.ApiError;
import edu.pnu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
	private final AuthService authService;
	private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        var saved = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegisterResponse(saved.getUsername()));
    }
    
    // ====== POST /api/v1/auth/login ======
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        var authToken = new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword());
        try {
            // 1) 인증
            Authentication auth = authenticationManager.authenticate(authToken);

            // 2) 세션에 SecurityContext 저장 (중요)
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);

            // 3) 세션 만료(서버 설정과 동기화)
            var session = request.getSession(false);
            if (session != null) {
                session.setMaxInactiveInterval(1800); // 30분 = 1800초
            }
            int expiresIn = (session != null) ? session.getMaxInactiveInterval() : 1800;

            // 권한 "ROLE_USER" → "USER"
            String roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                    .collect(Collectors.joining(","));

            return ResponseEntity.ok(Map.of(
                    "user", Map.of(
                            "username", auth.getName(),
                            "roles", roles
                    ),
                    "sessionExpiresIn", expiresIn   // 스펙과 맞추고 싶으면 3600으로 고정해도 됨
            ));

        } catch (BadCredentialsException e) {
            // 스펙 메시지를 그대로 사용 (username 로그인이어도 그대로 쓰기로 함)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            		 .body(new ApiError(401, "이메일 또는 비밀번호가 올바르지 않습니다."));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            		.body(new ApiError(401, "이메일 또는 비밀번호가 올바르지 않습니다."));
        } catch (Exception e) {
            // 기타 예외는 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            		.body(new ApiError(500, "서버 오류가 발생했습니다."));
        }
    }
    
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            		.body(new ApiError(401, "인증 필요"));
        return ResponseEntity.ok(Map.of("username", auth.getName()));
    }
    
    
    
    
    
    
    
    
    
    
    
//    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
//        try {
//            var saved = authService.register(req);
//            // 성공: 201 Created
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(new RegisterResponse(saved.getUsername()));
//        } catch (DuplicateKeyException e) {
//            // 중복 username/email: 409 Conflict
//            return ResponseEntity.status(HttpStatus.CONFLICT)
//                    .body(new ApiError(e.getMessage(), "중복 아이디/이메일"));
//        } catch (Exception e) {
//            // 서버 오류: 500
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new ApiError("INTERNAL", "내부 서버 오류"));
//        }
//    }
}
