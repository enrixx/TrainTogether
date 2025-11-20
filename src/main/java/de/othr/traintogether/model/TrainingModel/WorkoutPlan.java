package de.othr.traintogether.model.TrainingModel;

import de.othr.traintogether.model.TrainingModel.Exercise;

import java.util.ArrayList;
import java.util.List;

public class WorkoutPlan {

    private String day;
    private List<Exercise> exercises = new ArrayList<>();

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }
}