package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.template.PlaceholderManager;
import com.mailscheduler.domain.common.spreadsheet.SpreadsheetReference;
import com.mailscheduler.application.spreadsheet.SpreadsheetService;
import com.mailscheduler.common.exception.PlaceholderException;

public class PlaceholderResolver {
    private final SpreadsheetService spreadsheetService;

    public PlaceholderResolver(SpreadsheetService spreadsheetService) {
        this.spreadsheetService = spreadsheetService;
    }

    public PlaceholderManager resolvePlaceholders(
            PlaceholderManager manager,
            SpreadsheetReference row
    ) throws PlaceholderException {
        // Implementation moved from original EmailSchedulingService
        // Handles placeholder resolution using spreadsheet service

    }
}