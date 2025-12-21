package de.othr.traintogether.service;

import de.othr.traintogether.model.Gym;
import de.othr.traintogether.repository.GymRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GymService {

    private final GymRepository gymRepository;

    public GymService(GymRepository gymRepository) {
        this.gymRepository = gymRepository;
    }

    public List<Gym> getAllGyms() {
        return (List<Gym>) gymRepository.findAll();
    }

    public Optional<Gym> getGymById(Long id) {
        return gymRepository.findById(id);
    }

    public void saveGym(Gym gym) {
        gymRepository.save(gym);
    }
}