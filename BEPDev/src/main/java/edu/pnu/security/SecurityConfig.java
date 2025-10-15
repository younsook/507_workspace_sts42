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

	        // 2) CSRF: 개발 단계에선 끄고 시작 (운영 전환 시 CookieCsrfTokenRepository 사용 권장)
	        http.csrf(csrf -> csrf.disable());

	        // 3) 인가 규칙: 읽기 공개, 쓰기 보호 + 인증/회원가입 경로는 열어둠
	        http.authorizeHttpRequests(auth -> auth
	        		// 공개 엔드포인트
	        	    .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
	        	    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight 허용(선택)

	        	    // 인증/회원가입은 공개
	        	    .requestMatchers(
	        	    		"/api/v1/auth/**",
	        	    		"/api/v1/rawdata/**"
	        	    ).permitAll()

	        	    // ★ 마이페이지(me)는 전부 인증 필요
	        	    .requestMatchers("/api/v1/me/**").authenticated()

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

	        // 5) 폼 로그인/기본 인증: 프론트가 로그인 처리하므로 폼로그인은 비활성
	        http.formLogin(form -> form.disable()); // 프론트가 로그인 폼 담당
	        // http.httpBasic(Customizer.withDefaults()); // (옵션) 임시 테스트용 Basic 인증

	        return http.build();
	    }
} 	
	 	 
