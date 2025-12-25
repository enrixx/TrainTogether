package de.othr.traintogether.dto;

import de.othr.traintogether.model.Gym;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GymMapDto {
    private Long id;
    private String name;
    private double lat;
    private double lon;

    public GymMapDto(Gym gym) {
        this.id = gym.getId();
        this.name = gym.getName();
        this.lat = gym.getLat();
        this.lon = gym.getLon();
    }
}
