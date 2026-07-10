package com.dbp.uripet.events.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendHtmlEmail(
            String to,
            String subject,
            String templateName,
            Context context
    ) {
        try {
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("HTML email sent successfully to: " + to);

        } catch (Exception e) {
            System.err.println("Error sending HTML email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendEmail(
            String to,
            String subject,
            String text
    ) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            System.out.println("Plain text email sent successfully to: " + to);

        } catch (Exception e) {
            System.err.println("Error sending plain text email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}