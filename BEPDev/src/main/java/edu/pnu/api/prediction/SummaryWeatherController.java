package edu.pnu.api.prediction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.repository.prediction.Predicted3yRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/rawdata")
@RequiredArgsConstructor
public class SummaryWeatherController {
	private final Predicted3yRepository repo;
	
	//11p 대시보드>장기추세(3년 예측 기간 기상+예측 수위 상관관계)
	@GetMapping("/summary/weather")
	public Map<String, Object> weatherSummary(
			@RequestParam Short station,
			@RequestParam(required = false) Integer horizons){
		
		LocalDateTime from = LocalDateTime.of(2021, 1, 1, 0, 0);
        LocalDateTime to   = LocalDateTime.of(2023,12,31,0, 0);

        var rows = repo.findWeatherSummaryMonthly(station, from, to);

        List<List<Object>> predicted = new ArrayList<>();
        List<List<Object>> rain      = new ArrayList<>();
        List<List<Object>> temp      = new ArrayList<>();
        List<List<Object>> humid     = new ArrayList<>();

        for (var r : rows) {
            Integer ym = r.getYyyymm();
            predicted.add(Arrays.asList(ym, r.getPredicted()));
            rain.add     (Arrays.asList(ym, r.getRainMm()));
            temp.add     (Arrays.asList(ym, r.getTempC()));
            humid.add    (Arrays.asList(ym, r.getHumidityPct()));
        }
		
		predicted = applyHorizons(predicted, horizons);
        rain      = applyHorizons(rain, horizons);
        temp      = applyHorizons(temp, horizons);
        humid     = applyHorizons(humid, horizons);
		
		return Map.of(
	            "data", Map.of(
	                "series_raw", Map.of(
	                    "predicted",    predicted,
	                    "rain_mm",      rain,
	                    "temp_c",       temp,
	                    "humidity_pct", humid
	                )
	            )
	        );
	}
	
	private List<List<Object>> applyHorizons(List<List<Object>> data, Integer horizons) {
        if (horizons == null || horizons <= 0 || data.size() <= horizons) return data;
        return new ArrayList<>(data.subList(data.size() - horizons, data.size()));
    }
}
