package edu.pnu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.pnu.persistence.UserRepository;



@SpringBootTest
class GroundwaterApplicationTests {
	@Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        userRepository.findAll()
                .forEach(u -> System.out.println(u.getUserId() + " / " + u.getUsername() + " / " + u.getEmail()));
    }
}
