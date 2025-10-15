package edu.pnu.api.prediction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.repository.prediction.ModelMetricsRepository;
import edu.pnu.repository.prediction.Predicted3yRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rawdata")
@RequiredArgsConstructor
public class SummaryPredictController {


    private final Predicted3yRepository repo;        // 조인 쿼리 사용
    private final ModelMetricsRepository metricsRepo; // 최신/특정 run metrics

    // 10p 대시보드>장기추세(3년 예측 요약 테이블)
    @GetMapping("/summary/predict")
    public Map<String, Object> summaryPredict(
            @RequestParam short station,
            @RequestParam(defaultValue = "monthly") String timestep, // monthly | yearly
            @RequestParam(required = false) Long runId               // 있으면 해당 run의 metrics, 없으면 최신
    ) {
        // 기간: 최근 36개월(요구사항에 맞춰 필요시 조정 가능)
//        LocalDateTime to   = LocalDateTime.now()
//                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
//        LocalDateTime from = to.minusMonths(36);
    	
    	LocalDateTime from = LocalDateTime.of(2021, 1, 1, 0, 0);
    	LocalDateTime to   = LocalDateTime.of(2024, 1, 1, 0, 0); // '< :to' 이므로 2023-12까지 포함


        // 관측(2014~2020 elev) + 예측(2021~2023 actual/predicted) 조합 원시시계열
        var rows = repo.findJoinedByStationBetween(station, from, to);

        // 집계
        List<List<Object>> tableData =
                "yearly".equalsIgnoreCase(timestep)
                        ? aggregateYearly(rows, station)
                        : aggregateMonthly(rows, station);

        // metrics
//        Object[] m = (runId != null)
//                ? metricsRepo.findMetricsByRun(station, runId)
//                : metricsRepo.findLatestMetrics(station);
//        Map<String, Object> metrics = (m == null) ? Map.of()
//                : Map.of("NSE", m[2], "KGE", m[1], "RMSE", m[3], "R2", m[0]); // 쿼리 컬럼 순서에 맞게 매핑
        var mr = (runId != null)
                ? metricsRepo.findMetricsByRun(station, runId)
                : metricsRepo.findLatestMetrics(station);

        Map<String,Object> metrics = (mr == null)
                ? Map.of()
                : Map.of(
                    "NSE",  mr.getNse(),
                    "KGE",  mr.getKge(),
                    "RMSE", mr.getRmse(),
                    "R2",   mr.getR2()
                  );
        
        // 응답 스펙
        Map<String, Object> table = Map.of(
                "columns", List.of("관측소", "관측 월/관측 년도", "실측값", "예측값", "오차 평균"),
                "table_data_3y", tableData
        );
        return Map.of("metrics", metrics, "table", table);
    }

    /* ===== 집계 유틸 ===== */

    // Predicted3yRepository.JoinedRow 그대로 사용(프로젝션)
    private static class Acc {
        BigDecimal sum = BigDecimal.ZERO; int cnt = 0;
        void add(BigDecimal v){ sum = sum.add(v); cnt++; }
        BigDecimal avg(){ return cnt==0 ? null : sum.divide(BigDecimal.valueOf(cnt), 5, RoundingMode.HALF_UP); }
    }
    private static class PairAcc { Acc a = new Acc(); Acc p = new Acc(); }

    private List<List<Object>> aggregateMonthly(
            List<Predicted3yRepository.JoinedRow> rows, short station) {

        Map<Integer, PairAcc> by = new LinkedHashMap<>();
        for (var r : rows) {
            int key = r.getTs().getYear() * 100 + r.getTs().getMonthValue();
            var pa = by.computeIfAbsent(key, k -> new PairAcc());
            if (r.getActual() != null)    pa.a.add(r.getActual());
            if (r.getPredicted() != null) pa.p.add(r.getPredicted());
        }
        List<List<Object>> out = new ArrayList<>();
        by.forEach((k, pa) -> {
            BigDecimal a = pa.a.avg(), p = pa.p.avg();
            BigDecimal err = (a != null && p != null) ? a.subtract(p).abs() : null;
            if (a != null || p != null) out.add(List.of(station, k, a, p, err));
        });
        return out;
    }

    private List<List<Object>> aggregateYearly(
            List<Predicted3yRepository.JoinedRow> rows, short station) {

        Map<Integer, PairAcc> by = new LinkedHashMap<>();
        for (var r : rows) {
            int key = r.getTs().getYear();
            var pa = by.computeIfAbsent(key, k -> new PairAcc());
            if (r.getActual() != null)    pa.a.add(r.getActual());
            if (r.getPredicted() != null) pa.p.add(r.getPredicted());
        }
        List<List<Object>> out = new ArrayList<>();
        by.forEach((k, pa) -> {
            BigDecimal a = pa.a.avg(), p = pa.p.avg();
            BigDecimal err = (a != null && p != null) ? a.subtract(p).abs() : null;
            if (a != null || p != null) out.add(List.of(station, k, a, p, err));
        });
        return out;
    }
}