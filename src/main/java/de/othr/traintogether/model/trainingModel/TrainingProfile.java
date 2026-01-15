package de.othr.traintogether.model.trainingModel;

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

    private Long activeTrainingSplitId;

    public TrainingProfile(Long userId) {
        this.userId = userId;
        this.description = "";
    }

    public void addSplit(TrainingSplit newSplit){
        splits.addFirst(newSplit);
    }

    public void deleteSplit(Long splitId){
        if (splitId == null) return;
        boolean removed = splits.removeIf(s -> s.getId().equals(splitId));
        if (removed && splitId.equals(activeTrainingSplitId)) {
            if (!splits.isEmpty()) {
                this.activeTrainingSplitId = splits.getFirst().getId();
            } else {
                this.activeTrainingSplitId = null;
            }
        }
    }

    public Long getActiveTrainingSplitId(){
        return activeTrainingSplitId;
    }

    public TrainingSplit getActiveTrainingSplit(){
        if(activeTrainingSplitId == null || splits == null){
            return null;
        }
        for (TrainingSplit split : splits) {
            if (activeTrainingSplitId.equals(split.getId())) {
                return split;
            }
        }
        return null;
    }

    public void setActiveTrainingSplitId(Long activeTrainingSplitId){
        this.activeTrainingSplitId = activeTrainingSplitId;
    }

    public void addMeasurements(BodyMeasurements measurements) {
        this.measurements.add(measurements);
    }
}
