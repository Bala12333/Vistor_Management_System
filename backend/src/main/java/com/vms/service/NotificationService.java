package com.vms.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Value("${sendgrid.api-key:dummy-key}")
    private String sendGridApiKey;

    @Value("${app.email.from:noreply@vms.com}")
    private String fromEmail;

    public void sendEmail(String toEmail, String subject, String body) {
        if ("dummy-key".equals(sendGridApiKey)) {
            log.info("SendGrid is disabled. Simulating email to {}: Subject={}, Body={}", toEmail, subject, body);
            return;
        }

        Email from = new Email(fromEmail);
        Email to = new Email(toEmail);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("SendGrid email sent with status code: {}", response.getStatusCode());
        } catch (IOException ex) {
            log.error("Failed to send email to {}", toEmail, ex);
        }
    }
}
