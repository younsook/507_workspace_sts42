package edu.pnu.repository;
import edu.pnu.domain.User;
import edu.pnu.repository.auth.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;



@DataJpaTest   
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = edu.pnu.domain.User.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/water",
    "spring.datasource.username=busan",
    "spring.datasource.password=busan123!",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @Commit
    void save_and_find_user() {
        // given
        User user = User.builder()
                .username("meta")
                .email("meta@example.com")
                .passwordHash(encoder.encode("pw1234!")) // 비밀번호는 해싱해서 저장
                .role("USER")
                .isActive(true)
                .build();

        // when
        User saved = userRepository.save(user);

        // then
        Optional<User> found = userRepository.findById(saved.getUserId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("meta");

        System.out.println("저장된 사용자: " + found.get());
    }
}