package edu.pnu.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MeRequestDelete {
	// (탈퇴 시 비번 확인을 받을 경우)
	private String currentPassword; // 선택(정책에 따라 필수로 바꿔도 됨)
}
