package edu.pnu.domain;
import java.time.LocalDateTime;

//
import org.hibernate.annotations.DynamicInsert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@DynamicInsert  // Hibernate가 null 필드는 INSERT문에서 생략
@Table(name="users")  
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long userId;

    private String username;
    @Column(name="email", nullable=false, unique=true)
    private String email;

    @Column(name="password_hash")
    private String passwordHash;

    private String role;

    @Column(name="is_active")
    private Boolean isActive;

    @Column(name="created_dt", updatable=false, insertable=false)
    private LocalDateTime createdDt;

    @Column(name="updated_dt", insertable=false, updatable=false)
    private LocalDateTime updatedDt;
}
