package edu.pnu.dto.auth;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.password.PasswordEncoder;

import edu.pnu.domain.User;
import edu.pnu.dto.common.ApiError;
import edu.pnu.repository.auth.UserRepository;
import edu.pnu.service.auth.AuthService;
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
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

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
            
            // ★ 로그인 식별자(아이디/이메일)로 email 조회
            String login = auth.getName();
            String email = userRepository.findByUsername(login)
                    .map(u -> u.getEmail())
                    .orElseGet(() -> userRepository.findByEmail(login).map(u -> u.getEmail()).orElse(null));
            
            return ResponseEntity.ok(Map.of(
                    "user", Map.of(
                    		"username", login,
                            "email", email,
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
        
        String login = auth.getName(); // 아이디 또는 이메일
     // ★ Java 8 방식: username → 실패 시 email로 재조회
        var userByUsername = userRepository.findByUsername(login);
        var user = userByUsername.isPresent()
                ? userByUsername.get()
                : userRepository.findByEmail(login).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "인증 필요"));
        }
        
        String roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .collect(Collectors.joining(","));
        

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", roles
        ));

    }
    
    // ====== PATCH /api/v1/auth/me ======
    @PatchMapping("/me")
    public ResponseEntity<?> updateMe(@Valid @RequestBody MeRequestUpdate req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "인증 필요"));
        }
        String login = auth.getName();
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "인증 필요"));
        }

        // 이메일 변경
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && !req.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError(409, "중복 이메일"));
            }
            user.setEmail(req.getEmail());
        }

        // 비밀번호 변경
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiError(400, "현재 비밀번호가 필요합니다."));
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(422) // Unprocessable Entity
                        .body(new ApiError(422, "현재 비밀번호가 올바르지 않습니다."));
            }
            user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "email", user.getEmail()
        ));
    }

    // ====== DELETE /api/v1/auth/me ======
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(@RequestBody(required = false) MeRequestDelete req,
                                      HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "인증 필요"));
        }
        String login = auth.getName();
        var opt = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login));
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError(401, "인증 필요"));
        }
        var user = opt.get();

        // 정책: 비번 확인을 요구한다면
        if (req != null && req.getCurrentPassword() != null && !req.getCurrentPassword().isBlank()) {
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(422)
                        .body(new ApiError(422, "현재 비밀번호가 올바르지 않습니다."));
            }
        }

        userRepository.delete(user);

        // 세션 무효화
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "deleted"));
    }
    
    
    
    
    
  
}
