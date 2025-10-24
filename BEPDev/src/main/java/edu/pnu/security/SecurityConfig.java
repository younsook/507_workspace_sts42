package edu.pnu.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
	
		@Bean
	    public PasswordEncoder passwordEncoder() {
			return new BCryptPasswordEncoder();
	    }
		
		@Bean
	    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
	        return config.getAuthenticationManager();
	    }

	
	 	@Bean
	    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

	        // 1) CORS: Next.js 개발 서버(예: http://localhost:3000) 허용
	 		http.cors(cors -> cors.configurationSource(req -> {
	 		   CorsConfiguration cfg = new CorsConfiguration();
	 		   cfg.setAllowedOrigins(List.of("http://localhost:3000"));
	 		   cfg.setAllowedOrigins(List.of(
	 		       "http://10.125.121.222:3000",   // 🔴 실제 프론트 주소
	 		       "http://localhost:3000"         // (필요시 개발 로컬도 허용)
	 		   ));
	 		   cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
	 		   cfg.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Authorization"));
	 		   cfg.setAllowedHeaders(List.of("*"));           // 🔴 프리플라이트에서 어떤 헤더가 와도 허용
	 		   cfg.setAllowCredentials(true); // 세션 쿠키 허용
	 		    return cfg;
	 		}));

	        // 2) CSRF: (세션 기반 API 개발 단계: 비활성)(운영 전환 시 CookieCsrfTokenRepository 사용 권장)
	        http.csrf(csrf -> csrf.disable());

	        // 3) 인가 규칙(★ 순서 중요): 읽기 공개, 쓰기 보호 + 인증/회원가입 경로는 열어둠
	        http.authorizeHttpRequests(auth -> auth
	        		// 공개 엔드포인트
	        	    .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
	        	    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight 허용(선택)


	        	    // ★ 마이페이지(me)는 전부 인증 필요 (GET/PATCH/DELETE)
	        	    //.requestMatchers("/api/v1/me/**").authenticated()
	        	    .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
	        	    .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()   
	        	    // 대시보드 JSON 공개 (GET만)
	        	    .requestMatchers(HttpMethod.GET, "/api/v1/rawdata/**").permitAll()
	        	    .requestMatchers(HttpMethod.GET,    "/api/v1/auth/me").authenticated()
	                .requestMatchers(HttpMethod.PATCH,  "/api/v1/auth/me").authenticated()
	                .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/me").authenticated()

	        	    // 게시판: 조회는 공개, 쓰기는 인증 필요
	        	    .requestMatchers(HttpMethod.GET, "/api/v1/boards/**").permitAll()
	        	    .requestMatchers(HttpMethod.POST, "/api/v1/boards/**").authenticated()
	        	    .requestMatchers(HttpMethod.PUT,  "/api/v1/boards/**").authenticated()
	        	    .requestMatchers(HttpMethod.DELETE,"/api/v1/boards/**").authenticated()

	        	    // 나머지는 기본적으로 보호
	        	    .anyRequest().authenticated()                              // 그 외(쓰기/관리) 보호
	        );

	        // 4) 세션 전략: 필요할 때만 생성(기본값과 동일)
	        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

	        // 5) 폼로그인 비활성 (프론트에서 처리)
	        http.formLogin(form -> form.disable()); // 프론트가 로그인 폼 담당

	        //세션 무효화 + 컨텍스트 클리어 + 표준 JSON 응답
	        http
	        .exceptionHandling(ex -> ex
	          // 401: 인증 실패/미인증 접근
	          .authenticationEntryPoint((request, response, authEx) -> {
	              var session = request.getSession(false);
	              if (session != null) session.invalidate();             // ★ 세션 무효화
	              org.springframework.security.core.context.SecurityContextHolder.clearContext();

	              // (선택) 브라우저 쿠키 제거 힌트
	              jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", "");
	              cookie.setPath("/"); cookie.setMaxAge(0); cookie.setHttpOnly(true);
	              response.addCookie(cookie);

	              response.setStatus(401);
	              response.setContentType("application/json;charset=UTF-8");
	              response.getWriter().write("{\"code\":401,\"message\":\"인증 필요\"}");
	          })
	          // 403: 인증은 되었지만 권한 부족
	          .accessDeniedHandler((request, response, accessEx) -> {
	              // 403에서는 보통 세션은 유지하지만, 정책상 끊고 싶다면 아래 2줄 추가
	              // var session = request.getSession(false);
	              // if (session != null) session.invalidate();
	              response.setStatus(403);
	              response.setContentType("application/json;charset=UTF-8");
	              response.getWriter().write("{\"code\":403,\"message\":\"접근 거부\"}");
	          })
	        );

	        

	        return http.build();
	    }
} 	
	 	 
