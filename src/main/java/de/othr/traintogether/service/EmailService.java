package de.othr.traintogether.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final Resend resend;
    private final String fromEmail;
    private final String appUrl;
    private final TemplateEngine templateEngine;

    public EmailService(@Value("${resend.api-key}") String apiKey,
                       @Value("${resend.from-email}") String fromEmail,
                       @Value("${app.url}") String appUrl,
                       TemplateEngine templateEngine) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.appUrl = appUrl;
        this.templateEngine = templateEngine;
        logger.info("EmailService initialized. Emails will be sent from: {}", fromEmail);
        logger.info("Application URL for email links: {}", appUrl);
    }

    public void sendGymOwnerApprovalEmail(String toEmail, String firstName, String gymName, String language) {
        try {
            String subject = getApprovalSubject(language);
            String htmlContent = renderApprovalTemplate(firstName, gymName, language);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            logger.info("Approval email sent successfully to {} in {} language. Email ID: {}",
                       toEmail, language, response.getId());
        } catch (ResendException e) {
            logger.error("Failed to send approval email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send approval email", e);
        }
    }

    public void sendGymOwnerRejectionEmail(String toEmail, String firstName, String gymName, String language) {
        try {
            String subject = getRejectionSubject(language);
            String htmlContent = renderRejectionTemplate(firstName, gymName, language);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            logger.info("Rejection email sent successfully to {} in {} language. Email ID: {}",
                       toEmail, language, response.getId());
        } catch (ResendException e) {
            logger.error("Failed to send rejection email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send rejection email", e);
        }
    }

    private String renderApprovalTemplate(String firstName, String gymName, String language) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("gymName", gymName);
        context.setVariable("loginUrl", appUrl + "/login");

        String templateName = "emails/gym-owner-approval-" + language;
        return templateEngine.process(templateName, context);
    }

    private String renderRejectionTemplate(String firstName, String gymName, String language) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("gymName", gymName);

        String templateName = "emails/gym-owner-rejection-" + language;
        return templateEngine.process(templateName, context);
    }

    private String getApprovalSubject(String language) {
        return "de".equals(language)
            ? "🎉 Deine Fitnessstudio-Besitzer-Anfrage wurde genehmigt!"
            : "🎉 Your Gym Owner Request Has Been Approved!";
    }

    private String getRejectionSubject(String language) {
        return "de".equals(language)
            ? "Update zu deiner Fitnessstudio-Besitzer-Anfrage"
            : "Update on Your Gym Owner Request";
    }
}

