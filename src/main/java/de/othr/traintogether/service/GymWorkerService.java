package de.othr.traintogether.service;

import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.GymWorker;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.AuthorityRepository;
import de.othr.traintogether.repository.GymWorkerRepository;
import de.othr.traintogether.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GymWorkerService {

    private static final Logger logger = LoggerFactory.getLogger(GymWorkerService.class);

    private final GymWorkerRepository gymWorkerRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final GymService gymService;

    public GymWorkerService(GymWorkerRepository gymWorkerRepository,
                         UserRepository userRepository,
                         AuthorityRepository authorityRepository,
                         PasswordEncoder passwordEncoder,
                         GymService gymService) {
        this.gymWorkerRepository = gymWorkerRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.gymService = gymService;
    }

    @Transactional(readOnly = true)
    public List<GymWorker> findAll() {
        return gymWorkerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<GymWorker> findById(Long id) {
        return gymWorkerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public GymWorker getById(Long id) {
        return gymWorkerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gym worker not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<GymWorker> findByGymId(Long gymId) {
        return gymWorkerRepository.findByGymId(gymId);
    }

    @Transactional(readOnly = true)
    public Optional<GymWorker> findByUserId(Long userId) {
        return gymWorkerRepository.findByUserId(userId);
    }

    @Transactional
    public GymWorker createGymWorker(String email, String password, String firstName, String lastName,
                               Long gymId, User actingUser) {
        // Verify permission (Gym Owner or Admin)
        boolean isAdmin = actingUser.getAuthorities().contains("ADMIN");
        if (!isAdmin && !gymService.isOwner(gymId, actingUser.getId())) {
            throw new RuntimeException("Only gym owner or admin can create gym workers");
        }

        Gym gym = gymService.getById(gymId);

        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        // Create user account
        User user = new User(email, passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user = userRepository.save(user);

        // Add GYM_WORKER authority
        Authority workerAuthority = new Authority(user, "GYM_WORKER");
        authorityRepository.save(workerAuthority);

        // Create gym worker
        GymWorker gymWorker = new GymWorker(user, gym, actingUser);
        gymWorker = gymWorkerRepository.save(gymWorker);

        logger.info("Created gym worker account for user: {} at gym: {}", email, gym.getName());
        return gymWorker;
    }

    @Transactional
    public GymWorker updateGymWorker(Long id, String email, String firstName, String lastName, String password, User actingUser) {
        GymWorker gymWorker = getById(id);

        // Verify permission (Gym Owner or Admin)
        boolean isAdmin = actingUser.getAuthorities().contains("ADMIN");
        if (!isAdmin && !gymService.isOwner(gymWorker.getGym().getId(), actingUser.getId())) {
            throw new RuntimeException("Only gym owner or admin can update gym workers");
        }

        User user = gymWorker.getUser();

        if (!user.getEmail().equals(email) && userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        userRepository.save(user);

        logger.info("Updated gym worker with id: {}", id);
        return gymWorker;
    }

    @Transactional
    public GymWorker save(GymWorker gymWorker) {
        return gymWorkerRepository.save(gymWorker);
    }

    @Transactional
    public void deleteById(Long id) {
        logger.info("Deleting gym worker with id: {}", id);
        GymWorker gymWorker = getById(id);
        User user = gymWorker.getUser();

        gymWorkerRepository.delete(gymWorker);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        return gymWorkerRepository.existsByUserId(userId);
    }
}
