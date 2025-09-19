package edu.pnu.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
	private Object user;
	private long sessionExpiresIn;
}
