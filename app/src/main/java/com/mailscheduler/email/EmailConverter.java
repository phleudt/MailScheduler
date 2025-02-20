package com.mailscheduler.email;

import com.fasterxml.jackson.databind.deser.DataFormatReaders;
import com.google.api.services.gmail.model.Message;
import com.mailscheduler.Main;
import com.mailscheduler.model.Email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailConverter {
    private static final Logger LOGGER = Logger.getLogger(EmailConverter.class.getName());

    public static Message convertEmailToMessage(Email email) throws ConversionException {
        try {
            LOGGER.info("Converting Email to Message: " + email);
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(email.getSender()));
            mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(email.getRecipientEmail()));
            mimeMessage.setSubject(email.getSubject());

            // Process the body content to handle Loom embeds
            String processedBody = processEmailContent(email.getBody());

            // Create multipart message
            Multipart multipart = new MimeMultipart("alternative");

            // Add plain text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(stripHtml(processedBody), "utf-8");
            multipart.addBodyPart(textPart);

            // Add HTML part
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(processedBody, "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);

            // Add attachments if any
            if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
                for (String filePath : email.getAttachments()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(filePath);
                    multipart.addBodyPart(attachmentPart);
                }
            }

            mimeMessage.setContent(multipart);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);

            String encodedEmail = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
            Message message = new Message();
            message.setRaw(encodedEmail);

            if (email.getThreadId() != null) {
                message.setThreadId(email.getThreadId());
            }

            return message;
        } catch (MessagingException | IOException e) {
            throw new ConversionException("Failed to convert email object to message object: " + e.getMessage(), e);
        }
    }

    private static String processEmailContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        Pattern loomEmbededPattern = Pattern.compile(
                "<div>\\s*" +
                        "<a href=\"https://www\\.loom\\.com/share/[a-zA-Z0-9]+\">\\s*" +
                        "<p>.*?</p>\\s*" +
                        "</a>\\s*" +
                        "<a href=\"https://www\\.loom\\.com/share/[a-zA-Z0-9]+\">\\s*" +
                        "<img style=\"max-width:300px;\" src=\"https://cdn\\.loom\\.com/sessions/thumbnails/[a-zA-Z0-9]+-[a-zA-Z0-9]+-full-play\\.gif\">\\s*" +
                        "</a>\\s*" +
                        "</div>",
                Pattern.DOTALL
        );
        Matcher embedMatcher = loomEmbededPattern.matcher(content);

        // If no Loom video is found, return original content
        if (!embedMatcher.find()) {
            return content;
        }

        // Process the found Loom video and replace it in the content
        String processedVideo = processLoomVideo(embedMatcher.group());
        return embedMatcher.replaceFirst(Matcher.quoteReplacement(processedVideo));
    }

    private static String processLoomVideo(String embedCode) {
        Pattern loomLinkPattern = Pattern.compile("href=\"(https://www\\.loom\\.com/share/[a-zA-Z0-9]+)\"");
        Matcher linkMatcher = loomLinkPattern.matcher(embedCode);

        if (!linkMatcher.find()) {
            throw new IllegalArgumentException("Invalid Loom embed code: Missing share URL");
        }

        String shareUrl = linkMatcher.group(1);


        // Extract and process the title
        Pattern loomTitlePattern = Pattern.compile("<p>(.*?)</p>");
        Matcher titleMatcher = loomTitlePattern.matcher(embedCode);
        String title = titleMatcher.find()
                ? titleMatcher.group(1).replace("Watch Video", "Video jetzt ansehen")
                : "Ideen für Ihr Online-Marketing - Video jetzt ansehen";

        // Construct the new embed code
        return String.format(
                "<div>\n" +
                        "    <a href=\"%s\">\n" +
                        "        <p>%s</p>\n" +
                        "    </a>\n" +
                        "    <a href=\"%s\">\n" +
                        "        <img style=\"max-width:300px;\" src=\"https://cdn.loom.com/sessions/thumbnails/%s-full-play.gif\">\n" +
                        "    </a>\n" +
                        "    <br/><br/>Hier auch noch mal der Link:<br/><a href=\"%s\">%s</a>\n" +
                        "</div>",
                shareUrl,
                title,
                shareUrl,
                extractThumbnailId(embedCode),
                shareUrl,
                shareUrl
        );

    }

    private static String extractThumbnailId(String embedCode) {
        Pattern thumbnailPattern = Pattern.compile("thumbnails/([a-zA-Z0-9]+-[a-zA-Z0-9]+)-full-play\\.gif");
        Matcher thumbnailMatcher = thumbnailPattern.matcher(embedCode);
        if (!thumbnailMatcher.find()) {
            throw new IllegalArgumentException("Invalid Loom embed code: Missing thumbnail ID");
        }
        return thumbnailMatcher.group(1);
    }

    /**
     * Strips HTML tags from text to create plain text version
     */
    private static String stripHtml(String html) {
        return html.replaceAll("<[^>]*>", "")           // Remove HTML tags
                .replaceAll("&nbsp;", " ")            // Replace &nbsp; with space
                .replaceAll("\\s+", " ")              // Replace multiple spaces with single space
                .replaceAll("(?m)^\\s+$", "")         // Remove empty lines
                .trim();                              // Trim leading/trailing whitespace
    }

    public static class ConversionException extends Exception {
        public ConversionException(String message, Throwable throwable) {
            super(message, throwable);
        }

        public ConversionException(Throwable throwable) {
            super(throwable);
        }
    }
}