package de.othr.traintogether.model.TrainingModel;

public class Exercise {

    public Exercise() {}

    private String name;       // z.B. Bankdrücken
    private int reps;          // Wiederholungen
    private int sets;          // Sätze

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }
}
