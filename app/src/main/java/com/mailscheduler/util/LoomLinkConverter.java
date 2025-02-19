package com.mailscheduler.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class LoomLinkConverter {
    private static final Logger LOGGER = Logger.getLogger(LoomLinkConverter.class.getName());

    // Patterns to extract information from the Loom embed code
    private static final Pattern LOOM_LINK_PATTERN = Pattern.compile("href=\"(https://www\\.loom\\.com/share/[a-zA-Z0-9]+)\"");
    private static final Pattern LOOM_TITLE_PATTERN = Pattern.compile("<p>(.*?)</p>");
    private static final Pattern LOOM_THUMBNAIL_PATTERN = Pattern.compile("src=\"(https://cdn\\.loom\\.com/sessions/thumbnails/[^\"]+)\"");

        /**
         * Represents a parsed Loom video with all its components
         */
        private record LoomVideo(String shareUrl, String title, String thumbnailUrl) {
    }

    /**
     * Parses a Loom embed code to extract all necessary components
     * @param embedCode The full Loom embed HTML code
     * @return A LoomVideo object containing all parsed components, or null if parsing failed
     */
    private static LoomVideo parseLoomEmbedCode(String embedCode) {
        if (embedCode == null || embedCode.isEmpty()) {
            return null;
        }

        try {
            // Extract share URL
            Matcher linkMatcher = LOOM_LINK_PATTERN.matcher(embedCode);
            String shareUrl = linkMatcher.find() ? linkMatcher.group(1) : null;

            // Extract title
            Matcher titleMatcher = LOOM_TITLE_PATTERN.matcher(embedCode);
            String title = titleMatcher.find() ? titleMatcher.group(1) : "Video jetzt ansehen";

            // Replace "Watch Video" with "Video jetzt ansehen"
            title = title.replaceFirst("Watch Video", "Video jetzt ansehen");

            // Extract thumbnail URL
            Matcher thumbnailMatcher = LOOM_THUMBNAIL_PATTERN.matcher(embedCode);
            String thumbnailUrl = thumbnailMatcher.find() ? thumbnailMatcher.group(1) : null;

            if (shareUrl != null && thumbnailUrl != null) {
                return new LoomVideo(shareUrl, title, thumbnailUrl);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to parse Loom embed code: " + e.getMessage());
        }

        return null;
    }

    /**
     * Converts a LoomVideo object to HTML embed code
     * @param video The LoomVideo object containing all necessary components
     * @return The HTML embed code
     */
    private static String generateHtmlEmbed(LoomVideo video) {
        if (video == null) {
            return "";
        }

        return String.format(
                "<div>\n" +
                        "    <a href=\"%s\">\n" +
                        "      <p>%s</p>\n" +
                        "    </a>\n" +
                        "    <a href=\"%s\">\n" +
                        "      <img style=\"max-width:300px;\" src=\"%s\">\n" +
                        "    </a>\n" +
                        "</div>",
                video.shareUrl(),
                video.title(),
                video.shareUrl(),
                video.thumbnailUrl()
        );
    }

    /**
     * Generates a text-only share link section for the Loom video
     * @param video The LoomVideo object containing the share URL
     * @return Formatted text with the share link on new lines
     */
    private static String appendShareLinkSection(LoomVideo video) {
        if (video == null) return "";
        // Use explicit line break characters to ensure proper line breaks in HTML
        return String.format("<br/>Hier auch noch mal der Link:<br/>%s", video.shareUrl());
    }

    /**
     * Processes email body content and replaces all Loom embed codes with properly formatted ones
     * @param content The email body content
     * @return The processed content
     */
    public static String processEmailContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        LOGGER.info("Processing email content for Loom embeds");

        // Look for div elements containing Loom embeds
        Pattern divPattern = Pattern.compile("<div>\\s*.*?loom\\.com.*?</div>", Pattern.DOTALL);
        Matcher divMatcher = divPattern.matcher(content);
        StringBuilder result = new StringBuilder();

        while (divMatcher.find()) {
            String embedCode = divMatcher.group();
            LoomVideo video = parseLoomEmbedCode(embedCode);
            String replacement = video != null ?
                    generateHtmlEmbed(video) :
                    embedCode; // Keep original if parsing fails

            divMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));

            // Add link to the loom video
            result.append(appendShareLinkSection(video));
        }
        divMatcher.appendTail(result);

        return result.toString();
    }
}
