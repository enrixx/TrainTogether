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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private List<BodyMeasurements> measurements = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private List<TrainingSplit> splits = new ArrayList<>();

    private Long activeTraininSplitId;

    public TrainingProfile(Long userId) {
        this.userId = userId;
        this.description = "";
    }

    public void addSplit(TrainingSplit newSplit){
        splits.addFirst(newSplit);
    }

    public void deleteSplit(int splitId){
        splits.remove(splitId);
    }

    public Long getActiveTraininSplitId(){

        if(activeTraininSplitId == null){
            this.activeTraininSplitId = 1L;
        }

        return activeTraininSplitId;
    }

    public TrainingSplit getActiveTraininSplit(){
        if(activeTraininSplitId == null){
            this.activeTraininSplitId = 1L;
        }
        return splits.stream()
                .filter(s -> s.getId() != null && s.getId().equals(this.activeTraininSplitId))
                .findFirst()
                .orElse(null);

    }

    public void setActiveTraininSplitId(Long activeTraininSplitId){
        this.activeTraininSplitId = activeTraininSplitId;
    }

    public void addMeasurements(BodyMeasurements measurements) {
        this.measurements.add(measurements);
    }
}
