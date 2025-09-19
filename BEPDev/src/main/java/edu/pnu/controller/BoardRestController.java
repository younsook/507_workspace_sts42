package edu.pnu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.pnu.service.board.BoardService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
public class BoardRestController {
	private final BoardService boardService;

    // 목록 (GET 공개)
    @GetMapping
    public Page<Map<String,Object>> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String q) {
        return boardService.list(page,size,q)
            .map(b -> Map.of(
                "id", b.getId(),
                "title", b.getTitle(),
                "author", b.getAuthor().getUsername(),
                "createdAt", b.getCreatedAt(),
                "cnt", b.getCnt()
            ));
    }

    // 상세 (GET 공개)
    @GetMapping("/{id}")
    public Map<String,Object> get(@PathVariable Long id) {
        var b = boardService.get(id);
        return Map.of(
            "id", b.getId(), "title", b.getTitle(), "content", b.getContent(),
            "author", b.getAuthor().getUsername(), "createdAt", b.getCreatedAt(),
            "cnt", b.getCnt()
        );
    }

    // 작성 (로그인 필요, 파일 첨부 포함)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            Authentication auth,
            @RequestParam String title,
            @RequestParam(required=false) String content,
            @RequestParam(required=false, name="files") List<MultipartFile> files) {
        Long id = boardService.create(auth.getName(), title, content, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }
}
