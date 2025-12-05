package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class Measurement {

    private double value = 0.0;

    public Measurement(double value) {
        this.value = value;
    }
}
