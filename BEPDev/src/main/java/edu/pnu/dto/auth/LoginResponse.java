package edu.pnu.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
	private Object user;
	private long sessionExpiresIn;
	
	@Getter
    @AllArgsConstructor
    public static class UserPayload {
        private String username;
        private String email;
        private String roles;       // "USER,ADMIN"
    }
}
