package edu.pnu.repository.prediction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.Predicted3y;

public interface Predicted3yRepository extends JpaRepository<Predicted3y, Long> {
	// runId 사용하는 경우 // 모델런+스테이션
	List<Predicted3y> findByModelrunIdAndStationOrderByTsAsc(
			Long modelrunId, Short station);
	// 기간 조건(모델run 없음)
	List<Predicted3y> findByStationAndTsBetweenOrderByTsAsc(
			Short station, LocalDateTime from, LocalDateTime to);
	// 기간 조건(모델런 포함)
	List<Predicted3y> findByModelrunIdAndStationAndTsBetweenOrderByTsAsc(
			Long modelrunId, Short station, LocalDateTime from, LocalDateTime to);
	// 전체(모델런 없음), 스테이션만 조건
	List<Predicted3y> findByStationOrderByTsAsc(Short station);
	
	 // 인터페이스 기반 프로젝션 (컬럼 alias 와 getter 이름 일치)
    interface JoinedRow {
        LocalDateTime getTs();
        BigDecimal    getActual();
        BigDecimal    getPredicted();
    }
    
    /** /summary/weather 응답용 월 집계 결과 프로젝션 */
    interface WeatherMonthlyRow {
        Integer    getYyyymm();       // yyyyMM
        BigDecimal getPredicted();    // 월 평균 예측 (2021-01 이전은 null)
        BigDecimal getRainMm();       // 월 강수량 합 (정책)
        BigDecimal getTempC();        // 월 평균 기온
        BigDecimal getHumidityPct();  // 월 평균 습도
    }

	/** 
     * (핵심) 관측/예측 타임라인 합집합을 만든 뒤,
     * actual=observed_data.elev, predicted=predicted_3y.y_predicted 로 분리해서 가져옴
     * 기간 제한(from~to)도 함께.
     */
    @Query(value = """
        SELECT
          t.ts                  AS ts,
          o.elev                AS actual,
          p.y_predicted         AS predicted
        FROM (
          SELECT ts FROM observed_data WHERE station = :station AND ts BETWEEN :from AND :to
          UNION
          SELECT ts FROM predicted_3y  WHERE station = :station AND ts BETWEEN :from AND :to
        ) t
        LEFT JOIN observed_data o
               ON o.station = :station AND o.ts = t.ts
        LEFT JOIN predicted_3y p
               ON p.station = :station AND p.ts = t.ts
        ORDER BY t.ts
        """, nativeQuery = true)
     List<JoinedRow> findTimelineJoinedByStationBetween(
    		@Param("station") Short station,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
     );
    
    //3년 예측 요약 테이블
    @Query(value = """
            SELECT
              t.ts AS ts,
              CASE WHEN t.ts <  '2021-01-01'
                   THEN o.elev
                   ELSE p.actual
              END AS actual,
              CASE WHEN t.ts >= '2021-01-01'
                   THEN p.y_predicted
                   ELSE NULL
              END AS predicted
            FROM (
              SELECT ts FROM observed_data  WHERE station = :station AND ts >= :from AND ts < :to
              UNION
              SELECT ts FROM predicted_3y   WHERE station = :station AND ts >= :from AND ts < :to
            ) t
            LEFT JOIN observed_data o ON o.station = :station AND o.ts = t.ts
            LEFT JOIN predicted_3y  p ON p.station = :station AND p.ts = t.ts
            ORDER BY t.ts
            """, nativeQuery = true)
     List<JoinedRow> findJoinedByStationBetween(
            @Param("station") Short station,
            @Param("from")    LocalDateTime from,
            @Param("to")      LocalDateTime to
     );

    /**
     * 월단위 요약: predicted_3y(예측) + observed_data(강수/기온/습도)
     *  - 예측은 2021-01 이전은 null로 강제
     *  - 강수는 SUM, 기온/습도는 AVG (정책은 필요시 변경)
     */
    @Query(value = """
        SELECT
          (YEAR(t.ts) * 100 + MONTH(t.ts))                AS yyyymm,
          AVG(CASE WHEN t.ts >= '2021-01-01' THEN p.y_predicted END) AS predicted,
          SUM(o.rain_mm)                                  AS rain_mm,
          AVG(o.temp_c)                                   AS temp_c,
          AVG(o.humidity_pct)                             AS humidity_pct
        FROM (
          SELECT ts FROM observed_data WHERE station = :station AND ts BETWEEN :from AND :to
          UNION
          SELECT ts FROM predicted_3y  WHERE station = :station AND ts BETWEEN :from AND :to
        ) t
        LEFT JOIN observed_data o ON o.station = :station AND o.ts = t.ts
        LEFT JOIN predicted_3y  p ON p.station = :station AND p.ts = t.ts
        GROUP BY yyyymm
        ORDER BY yyyymm
        """, nativeQuery = true)
    List<WeatherMonthlyRow> findWeatherSummaryMonthly(
        @Param("station") Short station,
        @Param("from")    LocalDateTime from,
        @Param("to")      LocalDateTime to
    );
}
   

