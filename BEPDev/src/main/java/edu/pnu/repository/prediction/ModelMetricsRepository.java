package edu.pnu.repository.prediction;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import edu.pnu.domain.ModelMetrics;

public interface ModelMetricsRepository extends JpaRepository<ModelMetrics, Long> {
	// metrics 평가지표
    interface MetricsRow {
        Double getNse();
        Double getKge();
        Double getRmse();
        Double getR2();
    }

    @Query(value = """
        SELECT nse AS nse, kge AS kge, rmse AS rmse, r2 AS r2
        FROM model_metrics
        WHERE station = :station AND modelrun_id = :runId
        ORDER BY ts DESC, modelrun_id DESC
        LIMIT 1
        """, nativeQuery = true)
    MetricsRow findMetricsByRun(@Param("station") short station, @Param("runId") long runId);

    @Query(value = """
        SELECT nse AS nse, kge AS kge, rmse AS rmse, r2 AS r2
        FROM model_metrics
        WHERE station = :station
        ORDER BY ts DESC, modelrun_id DESC
        LIMIT 1
        """, nativeQuery = true)
    MetricsRow findLatestMetrics(@Param("station") short station);
}