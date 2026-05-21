package com.notification.smart.pay.hub.sph.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public boolean sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@smartpayhub.com");
            mailSender.send(message);
            log.info("Simple email sent to: {}", to);
            return true;
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            return false;
        }
    }

    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@smartpayhub.com");
            mailSender.send(message);
            log.info("HTML email sent to: {}", to);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            return false;
        }
    }

    public boolean sendBulkEmails(java.util.List<String> recipients, String subject, String content) {
        try {
            for (String recipient : recipients) {
                sendSimpleEmail(recipient, subject, content);
            }
            log.info("Bulk emails sent to {} recipients", recipients.size());
            return true;
        } catch (Exception e) {
            log.error("Bulk email send failed", e);
            return false;
        }
    }
}