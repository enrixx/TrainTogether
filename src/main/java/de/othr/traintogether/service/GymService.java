package de.othr.traintogether.service;

import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.GymRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GymService {

    private static final Logger logger = LoggerFactory.getLogger(GymService.class);

    private final GymRepository gymRepository;

    public GymService(GymRepository gymRepository) {
        this.gymRepository = gymRepository;
    }

    @Transactional(readOnly = true)
    public List<Gym> findAll() {
        return gymRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Gym> findById(Long id) {
        return gymRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Gym getById(Long id) {
        return gymRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gym not found with id: " + id));
    }

    @Transactional
    public Gym save(Gym gym) {
        logger.info("Saving gym: {}", gym.getName());
        return gymRepository.save(gym);
    }

    @Transactional
    public Gym create(Gym gym, User owner) {
        gym.setOwner(owner);
        logger.info("Creating gym: {} for owner: {}", gym.getName(), owner.getEmail());
        return gymRepository.save(gym);
    }

    @Transactional
    public Gym update(Long id, Gym updatedGym) {
        Gym existingGym = getById(id);

        existingGym.setName(updatedGym.getName());
        existingGym.setAddress(updatedGym.getAddress());
        existingGym.setCity(updatedGym.getCity());
        existingGym.setPostalCode(updatedGym.getPostalCode());
        existingGym.setPhoneNumber(updatedGym.getPhoneNumber());
        existingGym.setDescription(updatedGym.getDescription());

        logger.info("Updating gym with id: {}", id);
        return gymRepository.save(existingGym);
    }

    @Transactional
    public void deleteById(Long id) {
        logger.info("Deleting gym with id: {}", id);
        gymRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Gym> findByOwnerId(Long ownerId) {
        return gymRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Gym> findByCity(String city) {
        return gymRepository.findByCity(city);
    }

    @Transactional(readOnly = true)
    public List<Gym> searchByCity(String city) {
        return gymRepository.findByCityContainingIgnoreCase(city);
    }

    @Transactional(readOnly = true)
    public List<Gym> searchByName(String name) {
        return gymRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long gymId, Long userId) {
        return gymRepository.findById(gymId)
                .map(gym -> gym.getOwner().getId().equals(userId))
                .orElse(false);
    }
}

