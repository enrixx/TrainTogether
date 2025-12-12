package de.othr.traintogether.seed;

import de.othr.traintogether.model.TrainingModel.*;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingDataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final BodyMeasurementsRepository bmRepo;
    private final TrainingSplitRepository splitRepo;
    private final TrainingProfileRepository profileRepo;

    @Override
    public void run(String... args) throws Exception {
        if (profileRepo.count() > 0) return;

        // Beispiel User nehmen
        User user = userRepo.findAll().get(0);

        // BodyMeasurements
        BodyMeasurements bm = getBodyMeasurements();

        // ----------- TrainingSplit + 7 Tage -------------
        TrainingSplit split = new TrainingSplit("Push-Pull-Legs");

        for (Weekday wd : Weekday.values()) {
            TrainingDay day = new TrainingDay(wd);
            split.addDay(day);
        }

        // ----------- TrainingProfile erstellen -------------
        TrainingProfile profile = new TrainingProfile(user.getId());
        profile.setDescription("Mein Trainingsprofil — automatisch generiert.");
        profile.getSplits().add(split);
        profile.getMeasurements().add(bm);

        profileRepo.save(profile);

        System.out.println("✅ TrainingDataSeeder ausgeführt");
    }

    @NotNull
    private static BodyMeasurements getBodyMeasurements() {
        BodyMeasurements bm = new BodyMeasurements();
        bm.setGewicht(75.0);
        bm.setGroesse(180.0);
        bm.setBmi(23.1);
        bm.setApeIndex(5.0);

        bm.setBrust(new Measurement(100.0));
        bm.setSchulter(new Measurement(50.0));
        bm.setTaille(new Measurement(85.0));
        bm.setHuefte(new Measurement(95.0));
        bm.setArmLinks(new Measurement(32.0));
        bm.setArmRechts(new Measurement(32.5));
        bm.setUnterarmLinks(new Measurement(28.0));
        bm.setUnterarmRechts(new Measurement(28.2));
        bm.setBeinLinks(new Measurement(55.0));
        bm.setBeinRechts(new Measurement(55.5));
        return bm;
    }
}

