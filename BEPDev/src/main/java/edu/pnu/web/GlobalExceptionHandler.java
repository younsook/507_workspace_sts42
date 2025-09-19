package edu.pnu.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.pnu.dto.common.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 409 Conflict - username/email 중복
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "중복 아이디/이메일"));
    }

    // 400 Bad Request - DTO 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst().orElse("요청 값이 올바르지 않습니다");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, msg));
    }

    // 409 또는 500 - DB 무결성 위반 (중복이면 409, 그 외 500)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e) {
        if (isUniqueViolation(e)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError(409, "중복 아이디/이메일"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "내부 서버 오류"));
    }

    // 500 Internal Server Error - 그 외 모든 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "내부 서버 오류"));
    }

    // 원인 체인에서 중복 제약 판단 (DB/드라이버별 메시지 키워드 포함)
    private boolean isUniqueViolation(Throwable t) {
        while (t != null) {
            String m = t.getMessage();
            if (m != null) {
                String lower = m.toLowerCase();
                if (lower.contains("duplicate entry")      // MySQL
                    || lower.contains("unique constraint")  // 일반 표현
                    || lower.contains("uk_")                // 제약명 접두
                    || lower.contains("uq_")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }
}
