package edu.pnu.service.board;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import edu.pnu.domain.Board;
import edu.pnu.domain.User;
import edu.pnu.repository.auth.UserRepository;
import edu.pnu.repository.board.BoardRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // ★ 컨트롤러에서 요구한 시그니처
    @Transactional(readOnly = true)
    public Page<Board> list(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = (q == null) ? "" : q;
        return boardRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Board get(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Long create(String username, String title, String content, List<MultipartFile> files) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음: " + username));

        Board b = new Board();
        b.setAuthor(author);
        b.setTitle(title);
        b.setContent(content);
        b.setCnt(0L);

        Board saved = boardRepository.save(b);

        // TODO: files 저장 로직이 필요하면 여기서 처리 (로컬/S3 등)
        return saved.getId();
    }

    // 마이페이지용 목록
    @Transactional(readOnly = true)
    public Page<Board> listByAuthor(String username, int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = (q == null) ? "" : q;
        return boardRepository.findByAuthor_UsernameAndTitleContainingIgnoreCase(username, keyword, pageable);
    }

    // 소유자 검사 포함 단건 조회
    @Transactional(readOnly = true)
    public Board getOwned(Long id, String username) {
        return boardRepository.findByIdAndAuthor_Username(id, username)
                .orElseThrow(() -> new IllegalArgumentException("본인 소유 글이 아닙니다 또는 존재하지 않음: " + id));
    }


}
