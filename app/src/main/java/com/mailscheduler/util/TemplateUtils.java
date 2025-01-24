package com.mailscheduler.util;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class TemplateUtils {
    public static String extractSubject(Message message) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return "NO SUBJECT";
        }

        String subject =  message.getPayload().getHeaders().stream()
                .filter(header -> "Subject".equalsIgnoreCase(header.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("NO SUBJECT");
        if (subject.isEmpty()) return "NO SUBJECT";
        return subject;
    }

    public static String extractBody(Message message) {
        if (message.getPayload() == null) {
            return "NO BODY";
        }

        // Handle single-part body
        if (message.getPayload().getBody() != null && message.getPayload().getBody().getSize() > 0) {
            byte[] bodyBytes = message.getPayload().getBody().decodeData();
            if (bodyBytes != null) {
                String body = new String(bodyBytes);
                if (body.isEmpty()) return "NO BODY";
                return body;
            }
        }

        // Handle multi-part body
        if (message.getPayload().getParts() != null) {
            for (MessagePart part : message.getPayload().getParts()) {
                if (part.getBody() != null && part.getBody().getSize() > 0) {
                    byte[] partBodyBytes = part.getBody().decodeData();
                    if (partBodyBytes != null) {
                        String body =  new String(partBodyBytes);
                        if (body.isEmpty()) return "NO BODY";
                        return body;
                    }
                }
            }
        }

        return "NO BODY";
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
