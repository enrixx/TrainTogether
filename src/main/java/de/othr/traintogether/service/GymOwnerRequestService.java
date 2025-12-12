package de.othr.traintogether.service;

import de.othr.traintogether.model.Authority;
import de.othr.traintogether.model.GymOwnerRequest;
import de.othr.traintogether.model.RequestStatus;
import de.othr.traintogether.model.User;
import de.othr.traintogether.repository.AuthorityRepository;
import de.othr.traintogether.repository.GymOwnerRequestRepository;
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

    public GymOwnerRequestService(GymOwnerRequestRepository requestRepository,
                                  UserRepository userRepository,
                                  AuthorityRepository authorityRepository,
                                  EmailService emailService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.emailService = emailService;
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
    public boolean approveRequest(Long requestId, String adminEmail) {
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

        // Send approval email using session locale
        Locale currentLocale = LocaleContextHolder.getLocale();
        String language = currentLocale.getLanguage();
        boolean emailSent = emailService.sendGymOwnerApprovalEmail(
                user.getEmail(),
                user.getFirstName(),
                request.getGymName() != null ? request.getGymName() : "your gym",
                language
        );

        if (emailSent) {
            logger.info("Approval email sent to user: {} in {} language", user.getEmail(), language);
        } else {
            logger.warn("Failed to send approval email to user: {}", user.getEmail());
        }

        return emailSent;
    }

    @Transactional
    public boolean rejectRequest(Long requestId, String adminEmail) {
        GymOwnerRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been reviewed");
        }

        User user = request.getUser();

        // DO NOT remove PENDING_GYM_OWNER authority
        // DO NOT add USER authority
        // User keeps PENDING_GYM_OWNER role so they can reapply

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(admin);

        requestRepository.save(request);

        // Send rejection email using session locale
        Locale currentLocale = LocaleContextHolder.getLocale();
        String language = currentLocale.getLanguage();
        boolean emailSent = emailService.sendGymOwnerRejectionEmail(
                request.getUser().getEmail(),
                request.getUser().getFirstName(),
                request.getGymName() != null ? request.getGymName() : "your gym",
                language
        );

        if (emailSent) {
            logger.info("Rejection email sent to user: {} in {} language", request.getUser().getEmail(), language);
        } else {
            logger.warn("Failed to send rejection email to user: {}", request.getUser().getEmail());
        }

        return emailSent;
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

        GymOwnerRequest request = new GymOwnerRequest();
        request.setUser(user);
        request.setGymName(gymName);
        request.setGymAddress(gymAddress);
        request.setCity(city);
        request.setPostalCode(postalCode);
        request.setPhoneNumber(phoneNumber);
        request.setGymDescription(gymDescription);
        request.setRequestMessage(gymDescription);
        request.setStatus(RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        requestRepository.save(request);

        logger.info("Gym owner request submitted by user: {}", userEmail);
    }
}
