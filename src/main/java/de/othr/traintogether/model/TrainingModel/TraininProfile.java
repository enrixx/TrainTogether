package de.othr.traintogether.model.TrainingModel;

import java.util.ArrayList;
import java.util.List;

public class TraininProfile {
    private BodyMeasurements measurements = new BodyMeasurements();
    private TrainingSplit trainingSplit = new TrainingSplit();

    private List<TrainingDay> days = new ArrayList<>();

    public BodyMeasurements getMeasurements() { return measurements; }
    public void setMeasurements(BodyMeasurements measurements) { this.measurements = measurements; }

    public TrainingSplit getTrainingSplit() { return trainingSplit; }
    public void setTrainingSplit(TrainingSplit trainingSplit) { this.trainingSplit = trainingSplit; }

    public List<TrainingDay> getDays() { return days; }
    public void setDays(List<TrainingDay> days) { this.days = days; }
}
