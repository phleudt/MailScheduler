package com.mailscheduler.config;

import com.mailscheduler.model.SpreadsheetReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateConfiguration {
    private static final Map<String, SpreadsheetReference> DEFAULT_RECIPIENT_COLUMNS = Map.of(
            "domain", SpreadsheetReference.ofColumn("A"),
            "emailAddress", SpreadsheetReference.ofColumn("B"),
            "name", SpreadsheetReference.ofColumn("C"),
            "salutation", SpreadsheetReference.ofColumn("D"),
            "phoneNumber", SpreadsheetReference.ofColumn("T"),
            "initialEmailDate", SpreadsheetReference.ofColumn("J")
    );

    private static final Map<String, SpreadsheetReference> DEFAULT_MARK_EMAIL_COLUMNS = Map.of(
            "INITIAL", SpreadsheetReference.ofColumn("K"),
            "FOLLOW_UP_1", SpreadsheetReference.ofColumn("M"),
            "FOLLOW_UP_2", SpreadsheetReference.ofColumn("O"),
            "FOLLOW_UP_3", SpreadsheetReference.ofColumn("Q"),
            "FOLLOW_UP_4", SpreadsheetReference.ofColumn("S")
    );

    private static final Map<String, SpreadsheetReference> DEFAULT_MARK_SCHEDULE_COLUMNS = Map.of(
            "INITIAL", SpreadsheetReference.ofColumn("J"),
            "FOLLOW_UP_1", SpreadsheetReference.ofColumn("L"),
            "FOLLOW_UP_2", SpreadsheetReference.ofColumn("N"),
            "FOLLOW_UP_3", SpreadsheetReference.ofColumn("P"),
            "FOLLOW_UP_4", SpreadsheetReference.ofColumn("R")
    );

    public static Configuration createTemplateConfiguration(String spreadsheetId, boolean saveMode, List<SendingCriterion> sendingCriteria, String defaultSender) {
        return new Configuration.Builder()
                .recipientColumns(new HashMap<>(DEFAULT_RECIPIENT_COLUMNS))
                .markEmailColumns(new HashMap<>(DEFAULT_MARK_EMAIL_COLUMNS))
                .markSchedulesForEmailColumns(new HashMap<>(DEFAULT_MARK_SCHEDULE_COLUMNS))
                .spreadsheetId(spreadsheetId)
                .saveMode(saveMode)
                .numberOfFollowUps(4)
                .setSendingCriteria(sendingCriteria)
                .setDefaultSender(defaultSender)
                .build();
    }
}
