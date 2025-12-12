package de.othr.traintogether.service;

import de.othr.traintogether.dto.RegisterDto;
import de.othr.traintogether.dto.UpdateProfileDto;
import de.othr.traintogether.dto.UserDto;
import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.AuthorityRepository;
import de.othr.traintogether.repository.UserRepository;
import de.othr.traintogether.service.customExceptions.EmailAlreadyRegisteredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       AuthorityRepository authorityRepository,
                       PasswordEncoder passwordEncoder,
                       MinioService minioService,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.minioService = minioService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void registerUser(RegisterDto registerDto) {
        String email = registerDto.getEmail();
        String rawPassword = registerDto.getPassword();
        String role = registerDto.getRole();
        String userName = registerDto.getUsername();
        String firstName = registerDto.getFirstName();
        String lastName = registerDto.getLastName();

        if(userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException("{error.email.exists}");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, hashedPassword);
        if(userName != null && !userName.isBlank()){
            user.setUsername(userName);
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);

        userRepository.save(user);

        Authority authorityRole = new Authority(user, role);
        authorityRepository.save(authorityRole);
        
        eventPublisher.publishEvent(new UserCreatedEvent(user));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Transactional(readOnly = true)
    public UserDto findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserDto::new)
                .orElse(null);
    }

    @Transactional
    public void updateProfile(String currentEmail, UpdateProfileDto updateDto) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(currentEmail)) {
            if (userRepository.findByEmail(updateDto.getEmail()).isPresent()) {
                throw new EmailAlreadyRegisteredException("{error.email.exists}");
            }
            user.setEmail(updateDto.getEmail());
        }

        if (updateDto.getUsername() != null) {
            user.setUsername(updateDto.getUsername());
        }

        user.setFirstName(updateDto.getFirstName());
        user.setLastName(updateDto.getLastName());

        userRepository.save(user);
    }

    @Transactional
    public boolean updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
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
        User user = userRepository.findByEmail(email)
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            minioService.deleteProfilePicture(user.getProfilePictureUrl());
            user.setProfilePictureUrl(null);
            userRepository.save(user);
        }
    }
}
