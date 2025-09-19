package edu.pnu.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {
	private String username; // 성공 시 명세대로 리턴
}
