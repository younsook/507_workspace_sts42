package edu.pnu.domain;
//JPA엔티티
import java.time.LocalDateTime;

import edu.pnu.domain.enums.RunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "filelog_modelrun")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class FilelogModelrun {
	
	
	
	//파일로그 모델실행 테이블, 고유ID
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "modelrun_id")
	private Long modelrunId;
	
	//사용자 교유ID
	@Column(name = "user_id", nullable = false)
	private Long userId;
	
	//원본 파일명
	@Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

	//저장 파일명
    @Column(name = "save_filename", nullable = false, length = 512)
    private String saveFilename;
    
    //파일크기
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    //모델명
    @Column(name = "model_name", length = 100)
    private String modelName;
    
    //모델 버전
    @Column(name = "model_version", length = 50) 
    private String modelVersion;
    
    // 하이퍼파라미터 JSON
    @Column(name = "hyperparams_json", columnDefinition = "json")
    private String hyperparamsJson;
    
    //PENDING 대기, RUNNING 실행중, DONE 성공완료, FAILED 실패
  	//public enum RunStatus { PENDING, RUNNING, DONE, FAILED }
    //실행상태
    @Enumerated(EnumType.STRING) 
    @Column(name = "status", nullable = false, length = 16)
    private RunStatus status;
	
    //실패사유
    @Column(name = "fail_reason", length = 500)
    private String failReason;
    
    //결과 CSV경로
    @Column(name = "result_path", length = 512)
    private String resultPath;
    
    //모델 결과 파일명
    @Column(name = "model_filename", length = 512)
    private String modelFilename;
    
    //시작시간
    @Column(name = "started_dt")
    private LocalDateTime startedDt;
    
    //종료시간
    @Column(name = "finished_dt")
    private LocalDateTime finishedDt;

    //생성일시
    @Column(name = "created_dt", nullable = false)
    private LocalDateTime createdDt;
    
    @PrePersist
    void onCreate() {
        if (createdDt == null) createdDt = LocalDateTime.now();
        if (status == null) status = RunStatus.PENDING;
    }

}
