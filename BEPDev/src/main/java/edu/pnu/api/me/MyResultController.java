package edu.pnu.api.me;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // ← org.springframework.http.MediaType
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import edu.pnu.service.board.BoardService;

@RestController
@RequestMapping("/api/v1/me/results")
@RequiredArgsConstructor
public class MyResultController {
	private final BoardService boardService;

	  // 목록: 내 게시글만
	  @GetMapping
	  public Page<Map<String,Object>> myList(
	      Authentication auth,
	      @RequestParam(defaultValue="0") int page,
	      @RequestParam(defaultValue="10") int size,
	      @RequestParam(required=false) String q) {

	    return boardService.listByAuthor(auth.getName(), page, size, q)
	        .map(b -> Map.of(
	            "id", b.getId(),
	            "title", b.getTitle(),
	            "createdAt", b.getCreatedAt(),
	            "cnt", b.getCnt()
	        ));
	  }

	  // 상세: 내 게시글 소유권 확인
	  @GetMapping("/{resultId}")
	  public Map<String,Object> myDetail(@PathVariable Long resultId, Authentication auth) {
	    var b = boardService.getOwned(resultId, auth.getName()); // 소유자 검사 포함
	    return Map.of(
	        "id", b.getId(),
	        "title", b.getTitle(),
	        "content", b.getContent(),
	        "createdAt", b.getCreatedAt(),
	        "cnt", b.getCnt()
	    );
	  }

	  // 다운로드: CSV
	  @GetMapping(value = "/{resultId}/export", produces = "text/csv")
	  public void exportCsv(@PathVariable Long resultId,
	                        @RequestParam(defaultValue="csv") String format,
	                        Authentication auth,
	                        HttpServletResponse resp) throws IOException {
	    if (!"csv".equalsIgnoreCase(format)) {
	      resp.sendError(HttpStatus.BAD_REQUEST.value(), "지원하지 않는 포맷");
	      return;
	    }
	    var b = boardService.getOwned(resultId, auth.getName());

	    resp.setContentType("text/csv; charset=UTF-8");
	    resp.setHeader("Content-Disposition", "attachment; filename=\"result-" + resultId + ".csv\"");
	    try (PrintWriter w = resp.getWriter()) {
	      // 예시 컬럼: 필요한대로 수정
	      w.println("id,title,author,createdAt,cnt");
	      w.printf("%d,%s,%s,%s,%d%n",
	          b.getId(),
	          escapeCsv(b.getTitle()),
	          escapeCsv(b.getAuthor().getUsername()),
	          b.getCreatedAt(),
	          b.getCnt());
	    }
	  }

	  // 매우 단순한 CSV 이스케이프(콤마/따옴표만 처리)
	  private static String escapeCsv(String s) {
	    if (s == null) return "";
	    String t = s.replace("\"","\"\"");
	    if (t.contains(",") || t.contains("\"") || t.contains("\n")) {
	      return "\"" + t + "\"";
	    }
	    return t;
	  }
}
