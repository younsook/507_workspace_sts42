package edu.pnu.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.pnu.domain.User;
import edu.pnu.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));
        
        String role = (u.getRole() != null) ? u.getRole() : "USER";
        
        return new org.springframework.security.core.userdetails.User(
            u.getUsername(), u.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
//    @Override
//    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
//        // usernameOrEmail: 컨트롤러에서 username 필드로 아이디/이메일 아무거나 받을 때
//        User u = userRepository.findByUsername(usernameOrEmail)
//                .or(() -> userRepository.findByEmail(usernameOrEmail)) // ★ 이메일 로그인도 허용(선택)
//                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));
//
//        String role = (u.getRole() != null && !u.getRole().isBlank()) ? u.getRole() : "USER";
//
//        return new CustomUserDetails(
//                u.getUsername(),                          // 로그인 식별자(아이디)
//                u.getEmail(),                             // ★ 이메일
//                u.getPasswordHash(),
//                List.of(new SimpleGrantedAuthority("ROLE_" + role)),
//                true
//        );
//    }
}