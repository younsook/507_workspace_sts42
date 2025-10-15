package edu.pnu.repository.board;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Board;

public interface  BoardRepository extends JpaRepository<Board, Long>{
	// 목록 검색(작성자까지 함께 로딩)
    @EntityGraph(attributePaths = {"author"})
    Page<Board> findByTitleContainingIgnoreCase(String q, Pageable pageable);

    // 마이페이지용: 내 글만
    @EntityGraph(attributePaths = {"author"})
    Page<Board> findByAuthor_UsernameAndTitleContainingIgnoreCase(String username, String q, Pageable pageable);

    // 소유자 검사 포함 단건 조회
    @EntityGraph(attributePaths = {"author"})
    Optional<Board> findByIdAndAuthor_Username(Long id, String username);
}
