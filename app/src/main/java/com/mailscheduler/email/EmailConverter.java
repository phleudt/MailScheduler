package com.mailscheduler.email;

import com.google.api.services.gmail.model.Message;
import com.mailscheduler.model.Email;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import java.util.logging.Logger;

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

            if (email.getAttachments() == null || email.getAttachments().isEmpty()) {
                mimeMessage.setText(email.getBody());
            } else {
                Multipart multipart = getMultipart(email);

                mimeMessage.setContent(multipart);
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);

            String encodedEmail = Base64.getEncoder().encodeToString(buffer.toByteArray());
            Message message = new Message();
            message.setRaw(encodedEmail);

            message.setThreadId(email.getThreadId());

            return message;
        } catch (MessagingException | IOException e) {
            throw new ConversionException("Failed to convert email object to message object: " + e.getMessage(), e);
        }
    }

    private static Multipart getMultipart(Email email) throws MessagingException, IOException {
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(email.getBody());

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);

        // Add each attachment
        for (String filePath : email.getAttachments()) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(filePath);
            multipart.addBodyPart(attachmentPart);
        }
        return multipart;
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