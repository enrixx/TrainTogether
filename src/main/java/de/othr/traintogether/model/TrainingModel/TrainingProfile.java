package de.othr.traintogether.model.TrainingModel;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "training_profiles")
@Getter
@Setter
@NoArgsConstructor
public class TrainingProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User-Verknüpfung (User-ID aus deiner users Tabelle)
    @Column(nullable = false)
    private Long userId;

    // Beschreibung der Person / Kommentar zum Training
    @Column(length = 2000)
    private String description;

    @OneToMany(cascade = CascadeType.ALL)
    private List<BodyMeasurements> measurements = new ArrayList<>();

    // 1:1 Trainingssplit
    @OneToOne(cascade = CascadeType.ALL)
    private TrainingSplit split = new TrainingSplit();

    public TrainingProfile(Long userId) {
        this.userId = userId;
        this.description = "";
    }
}