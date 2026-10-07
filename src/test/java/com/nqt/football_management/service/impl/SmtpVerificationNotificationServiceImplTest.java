package com.nqt.football_management.service.impl;

import com.nqt.football_management.config.EmailVerificationProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpVerificationNotificationServiceImplTest {
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;

    private EmailVerificationProperties properties;
    private SmtpVerificationNotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new EmailVerificationProperties();
        properties.setMailFrom("no-reply@example.invalid");
        service = new SmtpVerificationNotificationServiceImpl(
                mailSender,
                templateEngine,
                properties
        );
    }

    @Test
    void sendsMultipartEmailWithRenderedHtmlAndPlainTextFallback() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("email/email-verification-otp"),
                org.mockito.ArgumentMatchers.any(Context.class)))
                .thenReturn("<html><body>rendered-verification-template</body></html>");

        service.sendEmailVerificationOtp(
                "client@example.invalid",
                "004271",
                Duration.ofMinutes(5)
        );

        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("Mã xác thực tài khoản Football Management");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("client@example.invalid");
        assertThat(message.getContentType()).startsWith("multipart/mixed");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/email-verification-otp"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("otp")).isEqualTo("004271");
        assertThat(contextCaptor.getValue().getVariable("ttlMinutes")).isEqualTo(5L);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        String rawEmail = output.toString(StandardCharsets.UTF_8);
        assertThat(rawEmail)
                .contains("text/plain")
                .contains("text/html")
                .contains("rendered-verification-template")
                .doesNotContain("${otp}");
    }
}
