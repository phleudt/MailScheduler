package com.mailscheduler.common.util;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.apache.commons.codec.binary.Base64;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TemplateUtils {
    private static final Logger LOGGER = Logger.getLogger(TemplateUtils.class.getName());

    public static String extractSubject(Message message) {
        try {
            if (message.getRaw() != null) {
                byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                Session session = Session.getDefaultInstance(new Properties(), null);
                MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));
                String subject = mimeMessage.getSubject();
                if (subject != null && !subject.isEmpty()) {
                    return subject;
                }
            }
        } catch (MessagingException e) {
            LOGGER.warning("Failed to extract subject from raw message: " + e.getMessage());
        }

        // Fallback to payload headers if raw processing fails
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            String subject = message.getPayload().getHeaders().stream()
                    .filter(header -> "Subject".equalsIgnoreCase(header.getName()))
                    .map(MessagePartHeader::getValue)
                    .findFirst()
                    .orElse(null);

            if (subject != null && !subject.isEmpty()) {
                return subject;
            }
        }

        return "NO SUBJECT";
    }

    public static String extractBody(Message message) {
        try {
            // First try to get the body from raw message if available
            if (message.getRaw() != null) {
                byte[] emailBytes = Base64.decodeBase64(message.getRaw());
                Session session = Session.getDefaultInstance(new Properties(), null);
                MimeMessage mimeMessage = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

                return getTextFromMimeMessage(mimeMessage);
            }

            // Fallback to payload processing
            if (message.getPayload() == null) {
                return "NO BODY";
            }

            // Try to find HTML content first
            String htmlContent = findPartByMimeType(message.getPayload(), "text/html");
            if (htmlContent != null) {
                return htmlContent;
            }

            // Fallback to plain text if no HTML found
            String plainContent = findPartByMimeType(message.getPayload(), "text/plain");
            if (plainContent != null) {
                return plainContent;
            }

            // Last resort: try to get content directly from payload body
            if (message.getPayload().getBody() != null && message.getPayload().getBody().getData() != null) {
                byte[] bodyBytes = Base64.decodeBase64(message.getPayload().getBody().getData());
                if (bodyBytes != null) {
                    return new String(bodyBytes);
                }
            }

            return "NO BODY";
        } catch (Exception e) {
            LOGGER.warning("Failed to extract body: " + e.getMessage());
            return "NO BODY";
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

    public static Set<String> extractKeys(char[] placeholderSymbols, String string) {
        Set<String> keys = new HashSet<>();
        int start = 0;
        while ((start = string.indexOf(placeholderSymbols[0], start)) != -1) {
            int end = string.indexOf(placeholderSymbols[1], start);
            if (end == -1) {
                System.err.println("Error in extractKeys method: No closing symbol found");
                break;
            }
            String key = string.substring(start + 1, end);
            keys.add(key);
            start = end + 1;
        }
        return keys;
    }

    public static Map<String, String> setToMap(Set<String> set) {
        return set.stream().collect(Collectors.toMap(key -> key, key -> ""));
    }

    public static boolean validateTemplate(String template, char[] placeholderSymbols) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        if (placeholderSymbols == null) {
            throw new IllegalArgumentException("Placeholder symbols cannot be null");
        }
        if (placeholderSymbols.length != 2) {
            throw new IllegalArgumentException("Placeholder symbols must have exactly two characters");
        }

        Stack<Character> stack = new Stack<>();
        for (int i = 0; i < template.length(); i++) {
            if (template.charAt(i) == placeholderSymbols[0]) {
                stack.push(placeholderSymbols[0]);
            } else if (template.charAt(i) == placeholderSymbols[1]) {
                if (stack.isEmpty()) {
                    return false;
                }
                stack.pop();
            }
        }
        return stack.isEmpty();
    }
}
