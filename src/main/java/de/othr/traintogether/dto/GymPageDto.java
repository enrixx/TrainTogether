package de.othr.traintogether.dto;

import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GymPageDto {
    private Gym gym;
    private boolean canEdit;
    private List<ReviewDto> googleReviews;
    private Map<Long, WeatherDto> courseWeather;
    private User currentUser;
}
