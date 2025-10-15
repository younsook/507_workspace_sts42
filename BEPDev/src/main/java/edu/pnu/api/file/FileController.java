package edu.pnu.api.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.pnu.repository.FilelogModelrunRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
class TestVal {
	private String username;
	private String str;
	private int val;
}

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
class TestDTO {
	private String name1;
	private String name2;
}

//
// PostMan에서 Body - form-data
//
// Key			Value						Content-Type
//-----------------------------------------------------------------------------------------------
// val	 - Text		{ "username":"user","str":"str","int":10}	application/json
// files - File		2 files selected				application/octet-stream
//

@Slf4j
@RestController
public class FileController {
 /**
  * 업로드 필드
  * */
	@Value("${spring.servlet.multipart.location:e:/Temp/uploads/}")
	private String location;
	//분석 서버 설정 (기본값 사용)
	@Value("${app.analysis.base-url:http://localhost:5000}")
	private String analysisBaseUrl;
	@Value("${app.analysis.run-endpoint:/api/run}")
	private String analysisRunEndpoint;
	
	@Autowired private FilelogModelrunRepository repo;
	
	private final RestTemplate restTemplate = new RestTemplate();
	
	// 컨트롤러 클래스 내부
	// 커밋 이후(After Commit)에 실행할 작업을 담아두는 값 객체
	private static record EnqueueTask(Long runId, String fileUrl) {}
	
	// 분석 서버 호출 URL (application.properties 설정값 기반)
	private String buildRunUrl() {
	    return analysisBaseUrl + analysisRunEndpoint; // ex) http://localhost:5000 + /api/run
	}
	
/** 업로드 메소드
 * 업로드 → UUID 저장 → DB(PENDING) → AFTER_COMMIT에서 분석 enqueue → RUNNING/FAILED 갱신
 */
	//@PostMapping("/api/upload")
	//@Transactional	
	@PostMapping(value="/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadFile(
	    @AuthenticationPrincipal(expression = "userid") Long userId, // ← User.getUserId()를 호출
		
		@RequestPart(value="jsonData", required=false) TestVal tv,
		@RequestPart(value="testDto", required=false) TestDTO td,
		@RequestPart("files") MultipartFile[] files) {
		System.out.println("tv:" + tv);
		System.out.println("td:" + td);
		
		
		if (userId == null) {
	        return ResponseEntity.status(401).body(Map.of("code","401","message","인증 필요"));
	    }
	    if(files == null || files.length != 1 || files[0].isEmpty()) { //(files == null || files.length == 0) {
	        return ResponseEntity.badRequest().body(Map.of("code","400","message","파일은 1개만 업로드하세요"));
	    }
	    
	    StringBuffer sb = new StringBuffer();
	    var results = new ArrayList<Map<String,Object>>();
	    var tasks   = new ArrayList<EnqueueTask>();  // After Commit에서 보낼 작업들
		
		if (files != null && 0 < files.length) {
			try {
				// (권장) 루트 폴더 보장: 반복문 밖에서 1회 수행 가능
				java.nio.file.Files.createDirectories(java.nio.file.Paths.get(location).normalize());
				
				// 1) 업로드 루프
				for(MultipartFile file : files) {
					if (file == null || file.isEmpty()) continue;
					System.out.println("file:" + location + file.getOriginalFilename());
					
					// A-1) 저장명-원본명 정리
					final String original = org.springframework.util.StringUtils.cleanPath(
							java.util.Optional.ofNullable(file.getOriginalFilename()).orElse("file"));
					// A-2) 저장명-확장자 추출 
					final String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
					// A-3) 저장명-UUID 저장명 생성 (충돌 방지)
					final String stored = java.util.UUID.randomUUID() + ext;
					
					// B-4) 저장-안전 경로 결합
					final Path target = Paths.get(location).resolve(stored).normalize();
					log.info("UPLOAD start userId={}, original={}, stored={}, path={}", 
								userId, original, stored, target);
					// B-5) 저장-실제 저장
					file.transferTo(target.toFile());
					// B-6) 저장-저장 크기 (DB 기록용)
					final long size = java.nio.file.Files.size(target);
					
					// 7) 공개 다운로드 URL (분석 서버가 받아갈 URL)
					String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/download/")
							.path(stored).toUriString();
					
					// 9) DB INSERT (PENDING)
					var run = new edu.pnu.domain.FilelogModelrun();
					run.setUserId(userId);
					run.setOriginalFilename(original);		//원본명
					run.setSaveFilename(target.toString()); //실제 저장 경로(UUID 포함)
					run.setFileSizeBytes(size);
					run.setStatus(edu.pnu.domain.enums.RunStatus.PENDING); //package edu.pnu.domain.enums;
					run.setCreatedDt(java.time.LocalDateTime.now());
					run = repo.save(run); //PK 생성
					
					// 10) 응답용 결과 & AFTER_COMMIT 작업 리스트에 추가
					//각 업로드 파일에 대해 프론트로 반환할 JSON 한 항목을 만듬
					results.add(Map.of(
							"runId", run.getModelrunId(),
							"storedName", stored, // UUID+확장자=save_filename
							"originalName", original,
							"size", size,
							"status", run.getStatus()
					));
					tasks.add(new EnqueueTask(run.getModelrunId(), fileUrl));
					
					// File의 Method를 이용한 방법
//					file.transferTo(new File(location + file.getOriginalFilename()));
					
					// FileOutputStream 객체를 이용하는 방법
//					FileOutputStream fos = new FileOutputStream(location + file.getOriginalFilename());
//					fos.write(file.getBytes());
//					fos.close();					
				} // 1) 업로드 루프 END
				//sb.append(files.length + "개의 파일이 전송되었습니다.\n");
				
				//업로드 처리 결과(results 리스트)가 하나도 없다면 400 에러를 리턴.
				if (results.isEmpty()) {
		            return ResponseEntity.badRequest().body(Map.of("code","400","message","비어있는 파일입니다"));
		        }
				
				// ✔ 업로드 완료 이벤트 응답
		        return ResponseEntity.status(201).body(Map.of(
		                "event", "1: 업로드 완료",
		                "count", results.size(),
		                "items", results
		        ));
				
				
				
			} catch(Exception e) {
				//return ResponseEntity.badRequest().body("Exception :" + e.getMessage());
				return ResponseEntity.status(500).body(Map.of("code","500","message","업로드 실패","detail",e.getMessage()));
			}
		}
		sb.append("JSON 데이터가 전송되었습니다.");
		return ResponseEntity.ok(sb.toString());
	}

	/** 분석 서버가 다운로드할 엔드포인트 */
	@GetMapping("/api/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {

		// 다운로드 대상 파일
        String filePath = location + filename;
        File file = new File(filePath);

        if (!file.exists())	return ResponseEntity.notFound().build();

        // Resource 객체 생성
        Resource resource = new FileSystemResource(file);

        // 파일 전송 ==> 파일 이름만 제외하고 고정
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }
	/** 분석 서버에 URL만 보내기 */
	// runId → 저장된 파일의 공개 다운로드 URL 생성
	
	//분석서버에 작업시작을 전송 : 트리거 버튼 엔드포인트
//	@PostMapping("/api/analysis/run/{runId}")
//	public ResponseEntity<?> trigger(@PathVariable Long runId) {
//	  var payload = Map.of("runId", runId, "fileUrl", buildPublicUrl(runId)); //buildPublicUrl(runId): 분석 서버가 내려받을 파일 URL
//	  restTemplate.postForEntity(buildRunUrl(), payload, Void.class); //buildRunUrl(): 분석 서버의 enqueuing API 주소를 만
//	  //restTemplate.postForEntity() //runId + fileUrl을 분석 서버에 전송
//	  //클라이언트에 202 Accepted만 돌려
//	  return ResponseEntity.accepted().build();
//	}
	
//	//여러개파일 업로드 경우
//	@GetMapping("/api/upload/{filename}")
//	public ResponseEntity<?> sendFiles(@PathVariable String filename) {
//		
//		System.out.println("send:" + filename);
//		
//		try {
//			// 복수개의 파일을 전송하고자 하는 경우에는 파일명을 리스트로 넘기면 된다.
//			return sendFilesAndJSONData(Arrays.asList(filename), new TestDTO("name1", "name2"), new TestVal("username", "stringh", 1024));
//		} catch (IOException e) {
//			return ResponseEntity.badRequest().body(e.getMessage());
//		}
//	}
//	
//	// 복수개의 파일과 JSON 데이터 전송
//	private ResponseEntity<?> sendFilesAndJSONData(List<String> fnames, TestDTO dto, TestVal val) throws IOException {
//		
//		// REST API 요청 객체 생성 (동기 방식)
//		RestTemplate restTemplate = new RestTemplate();
//		
//        // 파일 전송 요청 헤더 생성
//        HttpHeaders headers = new HttpHeaders();
//        
//        // Content 타입 설정
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//        // 전송 데이터를 담을 Body 객체 생성
//        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//
//        // JSON data 준비 (testDto) 및 body에 저장
//        if (dto != null) {
//	        Map<String, Object> testDto = new HashMap<>();
//	        testDto.put("name1", dto.getName1());
//	        testDto.put("name2", dto.getName2());
//	        body.add("testDto", testDto);
//        }        
//        // JSON data 준비 (jsonData) 및 body에 저장
//        if (val != null) {
//	        Map<String, Object> jsonData = new HashMap<>();
//	        jsonData.put("username", val.getUsername());
//	        jsonData.put("str", val.getStr());
//	        jsonData.put("val", val.getVal());
//	        body.add("jsonData", jsonData);
//        }		
//        // 전송 파일 준비
//		for (String fname : fnames) {
//	        // 파일을 바이트 배열로 로딩
//	        byte[] fileBytes = Files.readAllBytes(Paths.get(location + fname));
//	
//	        // 로딩된 바이트로 리소스 객체 생성
//	        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
//	            @Override
//	            public String getFilename() {
//	                return fname;
//	            }
//	        };
//	        // 생성된 리소스 객체를 body에 저장 
//	        body.add("files", fileResource);
//		}
//
//        // 요청할 파라미터 객체를 생성
//        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//        return restTemplate.exchange("http://localhost:8080/api/upload", HttpMethod.POST, requestEntity, String.class);
//    }	
}
