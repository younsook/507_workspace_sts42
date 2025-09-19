package edu.pnu.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
	@NotBlank @Size(max = 100)
    private String username;

    @NotBlank @Email @Size(max = 200)
    private String email;

    @NotBlank @Size(min = 8, max = 64)
    private String password; // 평문 입력만 받음
}
