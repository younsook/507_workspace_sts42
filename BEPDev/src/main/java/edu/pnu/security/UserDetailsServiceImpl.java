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
}