package edu.pnu.api.prediction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.repository.prediction.Predicted3yRepository;
import edu.pnu.repository.prediction.Predicted3yRepository.JoinedRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/rawdata")
@CrossOrigin(
	    origins = {"http://10.125.121.222:3000"},   // 프론트 주소
	    allowedHeaders = "*",
	    methods = { RequestMethod.GET, RequestMethod.OPTIONS },
	    allowCredentials = "true"                   // 쿠키/세션 쓰면 true
	)
@RequiredArgsConstructor
public class Predicted3yController {
	private final Predicted3yRepository repo;
	
	//9p 장기추세 그래프 (7년역사+3년 예측)
	@GetMapping("/longterm")
	public Map<String, Object> longterm(
		@RequestParam Short station,
		@RequestParam(required = false) Integer horizons,// limit N개만 // 집계 후 개수 제한 (예: 120 = 최근 120개월)
		@RequestParam(required = false, defaultValue = "monthly") String timestep
	){
		//1) 예측 + 실측 조인 (원시 시계열)
		//List<JoinedRow> rows = repo.findByStationOrderByTsAsc(station);
		
		// ★ 최근 10년 윈도우(필요시 years 파라미터로 빼도 됨)
//	    var now  = LocalDateTime.now();
//	    var from = now.minusYears(10);
		LocalDateTime from = LocalDateTime.of(2014, 1, 1, 0, 0);
	    LocalDateTime to   = LocalDateTime.of(2023, 12, 31, 0, 0);
		// ★ 조인된 원시 시계열을 10년 범위로 가져옴
	    List<JoinedRow> rows = repo.findTimelineJoinedByStationBetween(station, from, to);
		
		//2) 집계 (monthly/yearly 평균 + horizons 제한)
		List<List<Object>> actual;
		List<List<Object>> predicted;
		
		switch (timestep.toLowerCase()) {
			case "yearly" -> {
				actual = aggregateYearly(rows, true, from, to);
				predicted = aggregateYearly(rows, false, from, to);	
			}
			case "monthly" -> {
				actual = aggregateMonthly(rows, true, from, to);
				predicted = aggregateMonthly(rows, false, from, to);	
			}
			default -> { // 원시 (epoch seconds) 그대로
				actual = new ArrayList<>();
				predicted = new ArrayList<>();
				for (JoinedRow r : rows) {			
					long t = toEpochSec(r.getTs());
		            if (r.getActual() != null) actual.add(List.of(t, r.getActual()));
		            if (r.getPredicted() != null) predicted.add(List.of(t, r.getPredicted()));
				}
				
			}
			
		
		}
		// 3) horizons(개수 제한) 적용 — 예: monthly에서 120이면 최근 120개월만
		actual    = applyHorizons(actual, horizons);
		predicted = applyHorizons(predicted, horizons);
				
		// 응답
		return Map.of(
				"data", Map.of(
						"series", Map.of(
								"actual", actual, 
								"predicted", predicted)
						)
				);
	}
	
	private long toEpochSec(LocalDateTime ts) {
        return ts.atZone(ZoneId.of("Asia/Seoul")).toEpochSecond();
    }
	
	// 합계/개수 누적용 내부 유틸
	private static class Acc {
	    BigDecimal sum = BigDecimal.ZERO;
	    int count = 0;
	    void add(BigDecimal v) {
	        sum = sum.add(v);
	        count++;
	    }
	    BigDecimal avg() {
	        return count == 0 ? null : sum.divide(BigDecimal.valueOf(count), 5, RoundingMode.HALF_UP);
	    }
	}
	
	// 월 평균 집계 (yyyymm -> 평균)
	// 월 평균 집계 (actual은 from~to, predicted는 "실제 값이 시작되는 첫 yyyymm"부터)
	private List<List<Object>> aggregateMonthly(List<JoinedRow> rows, boolean isActual,
			LocalDateTime from, LocalDateTime to) {
	    Map<Integer, Acc> byMonth = new LinkedHashMap<>();
	    for (JoinedRow r : rows) {
	        BigDecimal v = isActual ? r.getActual() : r.getPredicted();
	        if (v == null) continue;
	        int yyyymm = r.getTs().getYear() * 100 + r.getTs().getMonthValue();
	        byMonth.computeIfAbsent(yyyymm, k -> new Acc()).add(v);
	    }
	    
	    // 시작 지점 결정: actual은 from, predicted는 실제 데이터가 있는 첫 키
	    int fromYm = from.getYear() * 100 + from.getMonthValue();
	    int startYm;
	    if (isActual) {
	        startYm = fromYm;
	    } else {
	        startYm = byMonth.isEmpty() ? fromYm
	                : byMonth.keySet().stream().min(Integer::compareTo).orElse(fromYm);
	    }
	    
	    var out = new ArrayList<List<Object>>();
	    var cursor = LocalDateTime.of(startYm / 100, startYm % 100, 1, 0, 0);
	    var end    = to.withDayOfMonth(1);
	    
	    while (!cursor.isAfter(end)) {
	        int yyyymm = cursor.getYear() * 100 + cursor.getMonthValue();
	        Acc acc = byMonth.get(yyyymm);
	        out.add(Arrays.asList(yyyymm, acc == null ? null : acc.avg()));
	        cursor = cursor.plusMonths(1);
	    }
	    return out;
	}

	// 연 집계: 각 연의 평균값
	// 연 평균 집계 (actual은 from~to, predicted는 "실제 값이 시작되는 첫 year"부터)
	private List<List<Object>> aggregateYearly(List<JoinedRow> rows, boolean isActual,
			LocalDateTime from, LocalDateTime to) {
	    Map<Integer, Acc> byYear = new LinkedHashMap<>();
	    for (JoinedRow r : rows) {
	        BigDecimal v = isActual ? r.getActual() : r.getPredicted();
	        if (v == null) continue;
	        int year = r.getTs().getYear();
	        byYear.computeIfAbsent(year, k -> new Acc()).add(v);
	    }
	    
	    int startYear;
	    if (isActual) {
	        startYear = from.getYear();
	    } else {
	        startYear = byYear.isEmpty() ? from.getYear()
	                : byYear.keySet().stream().min(Integer::compareTo).orElse(from.getYear());
	    }
	    
	    var out = new ArrayList<List<Object>>();
	    for (int year = startYear; year <= to.getYear(); year++) {
	        Acc acc = byYear.get(year);
	        out.add(Arrays.asList(year, acc == null ? null : acc.avg()));
	    }
	    return out;
	}
	
	  
	//집계 결과에 limit 적용
	private List<List<Object>> applyHorizons(List<List<Object>> data, Integer horizons) {
	    if (horizons == null || horizons <= 0 || data.size() <= horizons) return data;
	    return new ArrayList<>(data.subList(data.size() - horizons, data.size()));
	}
}

