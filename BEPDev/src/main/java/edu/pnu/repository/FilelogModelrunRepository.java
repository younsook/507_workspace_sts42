package edu.pnu.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.FilelogModelrun;
import edu.pnu.domain.enums.RunStatus;

public interface FilelogModelrunRepository extends JpaRepository<FilelogModelrun, Long> {
	List<FilelogModelrun> findByUserIdOrderByCreatedDtDesc(Long userId);
	List<FilelogModelrun> findByStatusOrderByCreatedDtDesc(RunStatus status);
	long countByStatus(RunStatus status);
	List<FilelogModelrun> findByCreatedDtBetween(LocalDateTime from, LocalDateTime to);

}
