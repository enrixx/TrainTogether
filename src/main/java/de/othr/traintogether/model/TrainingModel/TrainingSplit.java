package de.othr.traintogether.model.TrainingModel;

public class TrainingSplit {

    private String name;  // z.B. "Push Pull Legs", "5er Split"
    private String description; // Optional

    public TrainingSplit() {}

    public TrainingSplit(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}