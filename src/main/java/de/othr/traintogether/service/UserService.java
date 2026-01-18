package de.othr.traintogether.service;

import de.othr.traintogether.dto.GymOwnerRegisterDto;
import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.dto.UpdateProfileDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.*;
import de.othr.traintogether.repository.*;
import de.othr.traintogether.service.customExceptions.EmailAlreadyRegisteredException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;
    private final GymOwnerRequestRepository gymOwnerRequestRepository;
    private final GymRepository gymRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GymService gymService;

    public UserService(UserRepository userRepository,
                       AuthorityRepository authorityRepository,
                       PasswordEncoder passwordEncoder,
                       MinioService minioService,
                       GymOwnerRequestRepository gymOwnerRequestRepository,
                       GymRepository gymRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       ApplicationEventPublisher eventPublisher,
                       GymService gymService) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.minioService = minioService;
        this.gymOwnerRequestRepository = gymOwnerRequestRepository;
        this.gymRepository = gymRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.eventPublisher = eventPublisher;
        this.gymService = gymService;
    }

    // FRONTEND CALLS
    @Transactional
    public void registerUser(RegisterDto registerDto) {
        registerUser(registerDto, Role.USER);
    }

    // FOR TESTING PURPOSES ONLY / DATA INITIALIZER
    @Transactional
    public void registerUser(RegisterDto registerDto, Role role) {
        String email = registerDto.getEmail().toLowerCase();
        String rawPassword = registerDto.getPassword();
        String firstName = registerDto.getFirstName();
        String lastName = registerDto.getLastName();
        String gender = registerDto.getGender();
        LocalDate birthday = registerDto.getBirthday();

        if(userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException("{error.email.exists}");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, hashedPassword);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGender(gender);
        user.setBirthday(birthday);

        userRepository.save(user);

        Authority authorityRole = new Authority(user, role.name());
        authorityRepository.save(authorityRole);

        eventPublisher.publishEvent(new UserCreatedEvent(user));
    }

    @Transactional
    public void registerGymOwner(GymOwnerRegisterDto registerDto) {
        String email = registerDto.getEmail().toLowerCase();
        String rawPassword = registerDto.getPassword();
        String firstName = registerDto.getFirstName();
        String lastName = registerDto.getLastName();
        String gender = registerDto.getGender();
        LocalDate birthday = registerDto.getBirthday();

        if(userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException("{error.email.exists}");
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, hashedPassword);

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setGender(gender);
        user.setBirthday(birthday);


        userRepository.save(user);

        // Create PENDING_GYM_OWNER authority
        Authority authorityRole = new Authority(user, Role.PENDING_GYM_OWNER.name());
        authorityRepository.save(authorityRole);

        // Create the Gym entity with the registration data
        Gym gym = new Gym();
        gym.setName(registerDto.getGymName());
        gym.setAddress(registerDto.getGymAddress());
        gym.setCity(registerDto.getCity());
        gym.setPostalCode(registerDto.getPostalCode());
        gym.setPhoneNumber(registerDto.getPhoneNumber());
        gym.setDescription(registerDto.getGymDescription());
        
        // Use GymService to create (and geocode) the gym
        gym = gymService.create(gym, user);

        // Get the language the user is currently using for registration
        Locale currentLocale = LocaleContextHolder.getLocale();
        String language = currentLocale.getLanguage();

        // Create a gym owner request linked to the gym
        GymOwnerRequest request = new GymOwnerRequest();
        request.setUser(user);
        request.setGym(gym);
        request.setRequestMessage(registerDto.getGymDescription());
        request.setRequestLanguage(language); // Store the language used during registration
        request.setStatus(RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        gymOwnerRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email.toLowerCase()).isPresent();
    }

    @Transactional(readOnly = true)
    public UserDto findUserDTOByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .map(UserDto::new)
                .orElse(null);
    }

    @Transactional
    public void updateProfile(String currentEmail, UpdateProfileDto updateDto) {
        User user = userRepository.findByEmail(currentEmail.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDto.getEmail() != null && !updateDto.getEmail().toLowerCase().equals(currentEmail.toLowerCase())) {
            String newEmail = updateDto.getEmail().toLowerCase();
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new EmailAlreadyRegisteredException("{error.email.exists}");
            }
            user.setEmail(newEmail);
        }


        user.setFirstName(updateDto.getFirstName());
        user.setLastName(updateDto.getLastName());
        user.setGender(updateDto.getGender());
        user.setBirthday(updateDto.getBirthday());
        user.setBio(updateDto.getBio());

        userRepository.save(user);
    }

    @Transactional
    public boolean updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Transactional
    public String uploadProfilePicture(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old profile picture if exists
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            minioService.deleteProfilePicture(user.getProfilePictureUrl());
        }

        // Upload new profile picture
        String pictureUrl = minioService.uploadProfilePicture(file, user.getId());
        user.setProfilePictureUrl(pictureUrl);
        userRepository.save(user);

        return pictureUrl;
    }

    @Transactional
    public void deleteProfilePicture(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            minioService.deleteProfilePicture(user.getProfilePictureUrl());
            user.setProfilePictureUrl(null);
            userRepository.save(user);
        }
    }

    @Transactional
    public String createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElse(null);

        if (user == null) {
            // Don't reveal if email exists or not for security reasons
            return null;
        }

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        return token;
    }

    @Transactional(readOnly = true)
    public User validatePasswordResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null || resetToken.isExpired() || resetToken.isUsed()) {
            return null;
        }

        return resetToken.getUser();
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElse(null);

        if (resetToken == null || resetToken.isExpired() || resetToken.isUsed()) {
            return false;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return true;
    }

    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        passwordResetTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
