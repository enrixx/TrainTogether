package de.othr.traintogether.model.TrainingModel;

public class Measurement {
    private double value; // z.B. 40.5 cm

    public Measurement() {}

    public Measurement(double value) {
        this.value = value;
    }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}