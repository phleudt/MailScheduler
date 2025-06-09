package com.mailscheduler.util;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.mailscheduler.domain.model.common.vo.email.Body;
import com.mailscheduler.domain.model.common.vo.email.Subject;
import org.apache.commons.codec.binary.Base64;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.mail.Multipart;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class TemplateUtil {

    public static String extractRecipient(Message message) {
        if (message.getRaw() != null) {
            try {
                byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                Session session = Session.getDefaultInstance(new Properties(), null);
                MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));
                javax.mail.Address[] recipients = mimeMessage.getRecipients(javax.mail.Message.RecipientType.TO);
                if (recipients != null && recipients.length > 0) {
                    return java.util.Arrays.stream(recipients)
                            .map(javax.mail.Address::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
                }
            } catch (Exception e) {
                // Optionally log or handle exception
            }
        }
        return "";
    }
    public static Subject extractSubject(Message message) {
        try {
            if (message.getRaw() != null) {
                byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                Session session = Session.getDefaultInstance(new Properties(), null);
                MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));
                String subject = mimeMessage.getSubject();
                if (subject != null && !subject.isEmpty()) {
                    return Subject.of(subject);
                }
            }
        } catch (MessagingException e) {
        }

        // Fallback to payload headers if raw processing fails
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            String subject = message.getPayload().getHeaders().stream()
                    .filter(header -> "Subject".equalsIgnoreCase(header.getName()))
                    .map(MessagePartHeader::getValue)
                    .findFirst()
                    .orElse(null);

            if (subject != null && !subject.isEmpty()) {
                return Subject.of(subject);
            }
        }

        return Subject.of("NO SUBJECT");
    }

    public static Body extractBody(Message message) {
        try {
            // First try to get the body from raw message if available
            if (message.getRaw() != null) {
                byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                Session session = Session.getDefaultInstance(new Properties(), null);
                MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

                return Body.of(getTextFromMimeMessage(mimeMessage));
            }

            // Fallback to payload processing
            if (message.getPayload() == null) {
                return Body.of("NO BODY");
            }

            // Try to find HTML content first
            String htmlContent = findPartByMimeType(message.getPayload(), "text/html");
            if (htmlContent != null) {
                return Body.of(htmlContent);
            }

            // Fallback to plain text if no HTML found
            String plainContent = findPartByMimeType(message.getPayload(), "text/plain");
            if (plainContent != null) {
                return Body.of(plainContent);
            }

            // Last resort: try to get content directly from payload body
            if (message.getPayload().getBody() != null && message.getPayload().getBody().getData() != null) {
                byte[] bodyBytes = Base64.decodeBase64(message.getPayload().getBody().getData());
                if (bodyBytes != null) {
                    return Body.of(new String(bodyBytes));
                }
            }

            return Body.of("NO BODY");
        } catch (Exception e) {
            return Body.of("NO BODY");
        }
    }


    private static String getTextFromMimeMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        Object content = mimeMessage.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof Multipart) {
            return getTextFromMultipart((Multipart) content);
        }
        return "NO BODY";
    }

    private static String getTextFromMultipart(Multipart multipart) throws MessagingException, IOException {
        // First try to find HTML content
        for (int i = 0; i < multipart.getCount(); i++) {
            javax.mail.BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/html")) {
                return bodyPart.getContent().toString();
            }

            // Handle nested multipart
            if (bodyPart.isMimeType("multipart/*")) {
                String text = getTextFromMultipart((Multipart) bodyPart.getContent());
                if (!text.equals("NO BODY")) {
                    return text;
                }
            }
        }

        // If no HTML found, try to find plain text
        for (int i = 0; i < multipart.getCount(); i++) {
            javax.mail.BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return bodyPart.getContent().toString();
            }
        }

        return "NO BODY";
    }

    private static String findPartByMimeType(MessagePart part, String mimeType) {
        // Check current part
        if (part.getMimeType().equalsIgnoreCase(mimeType)) {
            if (part.getBody() != null && part.getBody().getData() != null) {
                byte[] bodyBytes = Base64.decodeBase64(part.getBody().getData());
                if (bodyBytes != null) {
                    return new String(bodyBytes);
                }
            }
        }

        // Check child parts
        if (part.getParts() != null) {
            for (MessagePart childPart : part.getParts()) {
                String result = findPartByMimeType(childPart, mimeType);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }
}
