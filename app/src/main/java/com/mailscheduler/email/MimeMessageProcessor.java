package com.mailscheduler.email;

import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.apache.commons.codec.binary.Base64;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MimeMessageProcessor {
    private String htmlContent;
    private Map<String, byte[]> inlineImages;
    private Map<String, String> contentIds;

    public MimeMessageProcessor() {
        this.inlineImages = new HashMap<>();
        this.contentIds = new HashMap<>();
    }

    public void processMessage(Message message) throws IOException, MessagingException {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }

        // Check if we have raw content
        String raw = message.getRaw();
        if (raw == null) {
            // If raw content is not available, try to get it from the message parts
            processMessageParts(message.getPayload());
        } else {
            // Process raw MIME message
            MimeMessage mimeMessage = rawToMimeMessage(message);
            processMimeParts(mimeMessage);
        }
        processInlineImageReferences();
    }

    private void processMessageParts(MessagePart payload) throws IOException, MessagingException {
        if (payload == null) {
            throw new IllegalArgumentException("Message payload cannot be null");
        }

        String mimeType = payload.getMimeType();
        if (mimeType == null) {
            return;
        }

        if (mimeType.equals("text/html")) {
            // Decode the body data
            String data = payload.getBody().getData();
            if (data != null) {
                this.htmlContent = new String(Base64.decodeBase64(data), "UTF-8");
            }
        } else if (mimeType.startsWith("multipart/")) {
            List<MessagePart> parts = payload.getParts();
            if (parts != null) {
                for (MessagePart part : parts) {
                    processMessageParts(part);
                }
            }
        } else if (mimeType.startsWith("image/")) {
            processInlineImagePart(payload);
        }
    }

    private void processInlineImagePart(MessagePart part) throws IOException {
        MessagePartBody body = part.getBody();
        if (body != null && body.getAttachmentId() != null) {
            String filename = part.getFilename();
            String contentId = part.getHeaders().stream()
                    .filter(header -> header.getName().equalsIgnoreCase("Content-ID"))
                    .map(MessagePartHeader::getValue)
                    .findFirst()
                    .map(id -> id.replaceAll("[<>]", ""))
                    .orElse(null);

            if (contentId != null && filename != null) {
                // Store the attachment data
                byte[] attachmentData = Base64.decodeBase64(body.getData());
                this.inlineImages.put(filename, attachmentData);
                this.contentIds.put(contentId, filename);
            }
        }
    }

    private MimeMessage rawToMimeMessage(Message message) throws IOException, MessagingException {
        String raw = message.getRaw();
        if (raw == null) {
            throw new IOException("Raw message content is not available");
        }

        byte[] emailBytes = Base64.decodeBase64(raw);
        if (emailBytes == null) {
            throw new IOException("Failed to decode raw message content");
        }

        Session session = Session.getDefaultInstance(new Properties(), null);
        return new MimeMessage(session, new ByteArrayInputStream(emailBytes));
    }

    private void processMimeParts(MimeMessage mimeMessage) throws MessagingException, IOException {
        processMessagePart(mimeMessage);
    }

    private void processMessagePart(javax.mail.Part part) throws MessagingException, IOException {
        String contentType = part.getContentType();
        if (contentType.contains("multipart/")) {
            javax.mail.Multipart multipart = (javax.mail.Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                processMessagePart(multipart.getBodyPart(i));
            }
        } else if (contentType.contains("text/html")) {
            this.htmlContent = (String) part.getContent();
        } else if (contentType.contains("image/")) {
            processInlineImage(part);
        }
    }

    private void processInlineImage(javax.mail.Part part) throws MessagingException, IOException {
        String[] contentId = part.getHeader("Content-ID");
        if (contentId != null && contentId.length > 0) {
            String cid = contentId[0].replaceAll("[<>]", "");
            String filename = part.getFileName();
            this.inlineImages.put(filename, getContent(part));
            this.contentIds.put(cid, filename);
        }
    }

    private byte[] getContent(javax.mail.Part part) throws IOException, MessagingException {
        if (part.getContent() instanceof ByteArrayInputStream) {
            ByteArrayInputStream bis = (ByteArrayInputStream) part.getContent();
            byte[] bytes = new byte[bis.available()];
            bis.read(bytes);
            return bytes;
        }
        return null;
    }

    private void processInlineImageReferences() {
        Pattern cidPattern = Pattern.compile("src=\"cid:([^\"]+)\"");
        Matcher matcher = cidPattern.matcher(htmlContent);
        StringBuffer newHtml = new StringBuffer();

        while (matcher.find()) {
            String cid = matcher.group(1);
            String filename = contentIds.get(cid);
            if (filename != null) {
                matcher.appendReplacement(newHtml, "src=\"{IMG:" + filename + "}\"");
            }
        }
        matcher.appendTail(newHtml);
        this.htmlContent = newHtml.toString();
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public Map<String, byte[]> getInlineImages() {
        return inlineImages;
    }
}