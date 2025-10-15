package edu.pnu.service.auth;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.pnu.domain.User;
import edu.pnu.dto.auth.RegisterRequest;
import edu.pnu.repository.auth.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

//Service (예외를 던지는 곳)
@Service
@RequiredArgsConstructor
public class AuthService {
		private final UserRepository userRepository;
	    private final PasswordEncoder passwordEncoder;

	    @Transactional
	    public User register(RegisterRequest req) {
	    	// 1) 사전 중복 체크
	        if (userRepository.existsByUsername(req.getUsername()) ||
	            userRepository.existsByEmail(req.getEmail())) {
	        	throw new DuplicateKeyException("중복 아이디/이메일");
	        }
	        // 2) 저장
	        User user = new User();
	        user.setUsername(req.getUsername());
	        user.setEmail(req.getEmail());
	        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
	        user.setRole("USER");
	        user.setIsActive(true);
	        return userRepository.save(user);
	    }
}
