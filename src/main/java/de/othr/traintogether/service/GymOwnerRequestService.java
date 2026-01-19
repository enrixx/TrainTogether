package de.othr.traintogether.service;

import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.Gym;
import de.othr.traintogether.model.GymOwnerRequest;
import de.othr.traintogether.model.RequestStatus;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.AuthorityRepository;
import de.othr.traintogether.repository.GymOwnerRequestRepository;
import de.othr.traintogether.repository.GymRepository;
import de.othr.traintogether.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class GymOwnerRequestService {

    private static final Logger logger = LoggerFactory.getLogger(GymOwnerRequestService.class);

    private final GymOwnerRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final EmailService emailService;
    private final GymRepository gymRepository;
    private final GymService gymService;

    public GymOwnerRequestService(GymOwnerRequestRepository requestRepository,
                                  UserRepository userRepository,
                                  AuthorityRepository authorityRepository,
                                  EmailService emailService,
                                  GymRepository gymRepository,
                                  GymService gymService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.emailService = emailService;
        this.gymRepository = gymRepository;
        this.gymService = gymService;
    }

    @Transactional(readOnly = true)
    public List<GymOwnerRequest> getAllRequests() {
        return requestRepository.findAllByOrderByRequestedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<GymOwnerRequest> getPendingRequests() {
        return requestRepository.findByStatusOrderByRequestedAtDesc(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public GymOwnerRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }

    @Transactional
    public void approveRequest(Long requestId, String adminEmail) {
        GymOwnerRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been reviewed");
        }

        User user = request.getUser();

        // Remove PENDING_GYM_OWNER authority if exists
        Authority pendingAuthority = authorityRepository.findByUserAndAuthority(user, "PENDING_GYM_OWNER");
        if (pendingAuthority != null) {
            authorityRepository.delete(pendingAuthority);
        }

        // Add GYM_OWNER authority
        Authority gymOwnerAuthority = new Authority(user, "GYM_OWNER");
        authorityRepository.save(gymOwnerAuthority);

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(admin);

        requestRepository.save(request);

        // Send email in the language the user used when submitting the request; default is english
        String language = request.getRequestLanguage() != null ? request.getRequestLanguage() : "en";
        emailService.sendGymOwnerApprovalEmail(
                user.getEmail(),
                user.getFirstName(),
                request.getGym().getName(),
                language
        );
    }

    @Transactional
    public void rejectRequest(Long requestId, String adminEmail) {
        GymOwnerRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been reviewed");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(admin);

        requestRepository.save(request);

        // Send email in the language the user used when submitting the request; default is english
        String language = request.getRequestLanguage() != null ? request.getRequestLanguage() : "en";
        emailService.sendGymOwnerRejectionEmail(
                request.getUser().getEmail(),
                request.getUser().getFirstName(),
                request.getGym().getName(),
                language
        );
    }

    @Transactional(readOnly = true)
    public boolean hasPendingRequest(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return requestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public boolean hasRejectedRequest(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return requestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.REJECTED);
    }

    @Transactional
    public void submitGymOwnerRequest(String userEmail, String gymName, String gymAddress,
                                     String city, String postalCode, String phoneNumber,
                                     String gymDescription) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));


        if (requestRepository.existsByUserIdAndStatus(user.getId(), RequestStatus.PENDING)) {
            throw new RuntimeException("You already have a pending gym owner request");
        }

        Authority gymOwnerAuthority = authorityRepository.findByUserAndAuthority(user, "GYM_OWNER");
        if (gymOwnerAuthority != null) {
            throw new RuntimeException("You are already a gym owner");
        }

        Authority pendingAuthority = authorityRepository.findByUserAndAuthority(user, "PENDING_GYM_OWNER");
        if (pendingAuthority == null) {
            pendingAuthority = new Authority(user, "PENDING_GYM_OWNER");
            authorityRepository.save(pendingAuthority);
            logger.info("Added PENDING_GYM_OWNER authority to user: {}", userEmail);
        }

        // Check if user has a previously REJECTED request that they want to resubmit
        GymOwnerRequest request = requestRepository.findByUserIdAndStatus(user.getId(), RequestStatus.REJECTED)
                .orElse(null);

        // Get the language the user is currently using to submit the request
        Locale currentLocale = LocaleContextHolder.getLocale();
        String language = currentLocale.getLanguage();

        Gym gym;

        if (request != null) {
            // User is RESUBMITTING after rejection
            // Update the existing gym with new details
            logger.info("User resubmitting after rejection - updating existing gym for user: {}", userEmail);

            gym = request.getGym();
            
            // Create a temporary object to hold new values for update
            Gym gymUpdate = new Gym();
            gymUpdate.setName(gymName);
            gymUpdate.setAddress(gymAddress);
            gymUpdate.setCity(city);
            gymUpdate.setPostalCode(postalCode);
            gymUpdate.setPhoneNumber(phoneNumber);
            gymUpdate.setDescription(gymDescription);
            // Preserve existing banner info as it's not in the form
            gymUpdate.setBannerText(gym.getBannerText());
            gymUpdate.setBannerTextColor(gym.getBannerTextColor());
            gymUpdate.setBannerImageUrl(gym.getBannerImageUrl());
            
            // Use GymService to update (and geocode if address changed)
            gym = gymService.update(gym.getId(), gymUpdate);

            // Update the request back to PENDING status
            request.setRequestMessage(gymDescription);
            request.setRequestLanguage(language); // Store the language used for this submission
            request.setStatus(RequestStatus.PENDING);
            request.setRequestedAt(LocalDateTime.now());
            request.setReviewedAt(null);
            request.setReviewedBy(null);
        } else {
            logger.info("First-time submission - creating new gym and request for user: {}", userEmail);

            gym = new Gym();
            gym.setName(gymName);
            gym.setAddress(gymAddress);
            gym.setCity(city);
            gym.setPostalCode(postalCode);
            gym.setPhoneNumber(phoneNumber);
            gym.setDescription(gymDescription);
            
            // Use GymService to create (and geocode)
            gym = gymService.create(gym, user);

            request = new GymOwnerRequest();
            request.setUser(user);
            request.setGym(gym);
            request.setRequestMessage(gymDescription);
            request.setRequestLanguage(language); // Store the language used for this submission
            request.setStatus(RequestStatus.PENDING);
            request.setRequestedAt(LocalDateTime.now());
        }

        requestRepository.save(request);
        logger.info("Gym owner request submitted successfully by user: {}", userEmail);
    }
}
