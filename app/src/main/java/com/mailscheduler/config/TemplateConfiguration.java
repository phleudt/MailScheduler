package com.mailscheduler.config;

import com.mailscheduler.model.SpreadsheetReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateConfiguration {
    private static final Map<String, SpreadsheetReference> DEFAULT_CONTACT_COLUMNS = Map.of(
            "domain", SpreadsheetReference.ofColumn("A"),
            "emailAddress", SpreadsheetReference.ofColumn("B"),
            "name", SpreadsheetReference.ofColumn("C"),
            "gender", SpreadsheetReference.ofColumn("D"),
            "phoneNumber", SpreadsheetReference.ofColumn("S"),
            "initialEmailDate", SpreadsheetReference.ofColumn("I")
    );

    private static final Map<String, SpreadsheetReference> DEFAULT_MARK_EMAIL_COLUMNS = Map.of(
            "INITIAL", SpreadsheetReference.ofColumn("J"),
            "FOLLOW_UP_1", SpreadsheetReference.ofColumn("L"),
            "FOLLOW_UP_2", SpreadsheetReference.ofColumn("N"),
            "FOLLOW_UP_3", SpreadsheetReference.ofColumn("P"),
            "FOLLOW_UP_4", SpreadsheetReference.ofColumn("R")
    );

    private static final Map<String, SpreadsheetReference> DEFAULT_MARK_SCHEDULE_COLUMNS = Map.of(
            "INITIAL", SpreadsheetReference.ofColumn("I"),
            "FOLLOW_UP_1", SpreadsheetReference.ofColumn("K"),
            "FOLLOW_UP_2", SpreadsheetReference.ofColumn("M"),
            "FOLLOW_UP_3", SpreadsheetReference.ofColumn("O"),
            "FOLLOW_UP_4", SpreadsheetReference.ofColumn("Q")
    );

    public static Configuration createTemplateConfiguration(String spreadsheetId, boolean saveMode, List<SendingCriterion> sendingCriteria, String defaultSender) {
        return new Configuration.Builder()
                .contactColumns(new HashMap<>(DEFAULT_CONTACT_COLUMNS))
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
