package de.othr.traintogether.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String appUrl;
    private final String senderEmail;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.url}") String appUrl,
                        @Value("${spring.mail.username}") String senderEmail,
                        TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.appUrl = appUrl;
        this.senderEmail = senderEmail;
        this.templateEngine = templateEngine;
        logger.info("EmailService initialized. Emails will be sent from Gmail authenticated account: {}", senderEmail);
        logger.info("Application URL for email links: {}", appUrl);
    }

    public boolean sendGymOwnerApprovalEmail(String toEmail, String firstName, String gymName, String language) {
        try {
            String subject = getApprovalSubject(language);
            String htmlContent = renderApprovalTemplate(firstName, gymName, language);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Approval email sent successfully to {} in {} language", toEmail, language);
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send approval email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending approval email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    public boolean sendGymOwnerRejectionEmail(String toEmail, String firstName, String gymName, String language) {
        try {
            String subject = getRejectionSubject(language);
            String htmlContent = renderRejectionTemplate(firstName, gymName, language);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Rejection email sent successfully to {} in {} language", toEmail, language);
            return true;
        } catch (MessagingException e) {
            logger.error("Failed to send rejection email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending rejection email to {}: {}", toEmail, e.getMessage(), e);
            return false;
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
        context.setVariable("loginUrl", appUrl + "/login");
        context.setVariable("email", senderEmail);

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