package de.othr.traintogether.service;

import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.AuthorityRepository;
import de.othr.traintogether.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public User registerUser(String email, String rawPassword,String role, String userName) {
        if(userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, hashedPassword);
        if(!userName.isBlank()){
            user.setUsername(userName);
        }
        userRepository.save(user);

        Authority authorityRole = new Authority(user, role);
        authorityRepository.save(authorityRole);

        return user;
    }
}
