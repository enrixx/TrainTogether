package de.othr.traintogether.model.trainingModel;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "body_measurements")
@Data
@NoArgsConstructor
public class BodyMeasurements {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    // Gewicht, Größe, BMI
    private double gewicht = 0.0;
    private double groesse = 0.0;
    private double bmi = 0.0;

    // APE Index
    private double apeIndex = 0.0;

    // ----- Grundumfang -----

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "brust"))
    private Measurement brust = new Measurement(0.0);

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "schulter"))
    private Measurement schulter = new Measurement(0.0);

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "taille"))
    private Measurement taille = new Measurement(0.0);

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "huefte"))
    private Measurement huefte = new Measurement(0.0);

    // ----- Arme -----

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "arm_links"))
    private Measurement armLinks = new Measurement(0.0);

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "arm_rechts"))
    private Measurement armRechts = new Measurement(0.0);

    // ----- Unterarme -----

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "unterarm_links"))
    private Measurement unterarmLinks = new Measurement(0.0);

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "unterarm_rechts"))
    private Measurement unterarmRechts = new Measurement(0.0);

    // ----- Beine -----

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "bein_links"))
    private Measurement beinLinks = new Measurement(0.0);

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "bein_rechts"))
    private Measurement beinRechts = new Measurement(0.0);

}