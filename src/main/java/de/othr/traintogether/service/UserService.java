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

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       AuthorityRepository authorityRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
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
}
