package com.mailscheduler.application.synchronization.spreadsheet;

import com.mailscheduler.domain.model.recipient.Recipient;

import java.util.List;

public record RecipientSpreadsheetEntry(
        List<Recipient> recipients,
        int spreadsheetRow
) {
}
