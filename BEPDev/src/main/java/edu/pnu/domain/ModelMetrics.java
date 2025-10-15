package edu.pnu.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "model_metrics")
public class ModelMetrics {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long id;

    @Column(name = "modelrun_id", nullable = false)
    private Long modelrunId;

    @Column(nullable = false)
    private Short station;

    @Column(nullable = false)
    private LocalDateTime ts;

    private Double nse;
    private Double kge;
    private Double rmse;
    private Double r2;

    @Column(name = "metrics_json", columnDefinition = "json")
    private String metricsJson;

    @Column(name = "created_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;
}
