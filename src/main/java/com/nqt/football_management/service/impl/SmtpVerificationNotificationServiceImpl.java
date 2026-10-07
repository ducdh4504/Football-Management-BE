package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.EmailVerificationProperties;
import com.nqt.football_management.service.VerificationNotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SmtpVerificationNotificationServiceImpl implements VerificationNotificationService {
    private static final String TEMPLATE_NAME = "email/email-verification-otp";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailVerificationProperties properties;

    @Override
    public void sendEmailVerificationOtp(String email, String otp, Duration ttl) {
        Context context = new Context(Locale.forLanguageTag("vi-VN"));
        context.setVariable("otp", otp);
        context.setVariable("ttlMinutes", Math.max(1, ttl.toMinutes()));
        context.setVariable("currentYear", Year.now().getValue());
        String html = templateEngine.process(TEMPLATE_NAME, context);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            if (properties.getMailFrom() != null && !properties.getMailFrom().isBlank()) {
                helper.setFrom(properties.getMailFrom());
            }
            helper.setTo(email);
            helper.setSubject("Mã xác thực tài khoản Football Management");
            helper.setText(plainText(otp, ttl), html);
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new MailPreparationException("Could not prepare verification email", exception);
        }
    }

    private String plainText(String otp, Duration ttl) {
        return "Mã xác thực Football Management của bạn là: " + otp + System.lineSeparator()
                + System.lineSeparator()
                + "Mã có hiệu lực trong " + Math.max(1, ttl.toMinutes()) + " phút."
                + System.lineSeparator()
                + "Không chia sẻ mã này với bất kỳ ai.";
    }
}
