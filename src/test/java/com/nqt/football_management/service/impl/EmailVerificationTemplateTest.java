package com.nqt.football_management.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTemplateTest {
    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    @Test
    void rendersModernOtpEmailWithoutExecutableScript() {
        Context context = new Context();
        context.setVariable("otp", "004271");
        context.setVariable("ttlMinutes", 5);
        context.setVariable("currentYear", 2026);

        String html = templateEngine.process("email/email-verification-otp", context);

        assertThat(html)
                .contains("004271")
                .contains("Mã hết hạn sau <span style=\"color:#047857; font-weight:800;\">5</span> phút")
                .contains("#059669")
                .contains("Chọn hoặc nhấn giữ mã để sao chép")
                .doesNotContain("${otp}")
                .doesNotContainIgnoringCase("<script");
    }
}
