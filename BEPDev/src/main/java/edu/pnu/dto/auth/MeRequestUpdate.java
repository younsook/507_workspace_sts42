package edu.pnu.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MeRequestUpdate {
	@Email @Size(max = 200)
    private String email;            // 선택

    private String currentPassword;  // 비번 변경 시 필수
    @Size(min = 8, max = 64)
    private String newPassword;      // 선택
}

