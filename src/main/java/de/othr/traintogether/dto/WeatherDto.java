package de.othr.traintogether.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class WeatherDto {
    private Double temperatureC;
    private String description;
    private String icon;
    private Instant timestamp;
    private Boolean forecast; // true if from forecast, false if current
    private Double windSpeed;
    private Integer humidity;
}

