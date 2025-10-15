package edu.pnu.domain;

import java.math.BigDecimal;
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
@Table(name = "predicted_3y")
public class Predicted3y {
	@Id 
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "pred3y_id")
	private Long id;
	
	@Column(name = "modelrun_id")
	private Long modelrunId;
	
	@Column(nullable = false)
	private Short station;

	@Column(name = "ts", nullable = false)
	private LocalDateTime ts;

	@Column(name = "actual", precision = 10, scale = 5)
	private BigDecimal actual;

	@Column(name = "y_predicted", nullable = false, precision = 10, scale = 5)
	private BigDecimal predicted;
	
	//created_dt를 DB 기본값으로 채우기
	@Column(name = "created_dt", nullable = false, insertable = false, updatable = false)
	private LocalDateTime created_dt;

}
