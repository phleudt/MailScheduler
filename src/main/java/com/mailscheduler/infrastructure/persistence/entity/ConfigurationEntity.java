package com.mailscheduler.infrastructure.persistence.entity;

import com.mailscheduler.domain.model.common.vo.email.EmailAddress;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationEntity extends TableEntity {
    private final String spreadsheetId;
    private final EmailAddress senderEmailAddress;
    private final Boolean saveMode;
    private final String sendingCriteriaColumn;
    private final List<ColumnMappingEntity> columnMappings;

    public ConfigurationEntity(Long id, String spreadsheetId, String senderEmailAddress,
                               Boolean saveMode, String sendingCriteriaColumn) {
        setId(id);
        this.spreadsheetId = spreadsheetId;
        this.senderEmailAddress = new EmailAddress(senderEmailAddress);
        this.saveMode = saveMode;
        this.sendingCriteriaColumn = sendingCriteriaColumn;
        this.columnMappings = new ArrayList<>();
    }

    public ConfigurationEntity(Long id, String spreadsheetId, String senderEmailAddress,
                               Boolean saveMode, String sendingCriteriaColumn, List<ColumnMappingEntity> columnMappings) {
        setId(id);
        this.spreadsheetId = spreadsheetId;
        this.senderEmailAddress = new EmailAddress(senderEmailAddress);
        this.saveMode = saveMode;
        this.sendingCriteriaColumn = sendingCriteriaColumn;
        this.columnMappings = columnMappings;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public EmailAddress getSenderEmailAddress() {
        return senderEmailAddress;
    }

    public Boolean getSaveMode() {
        return saveMode;
    }

    public String getSendingCriteriaColumn() {
        return sendingCriteriaColumn;
    }

    public List<ColumnMappingEntity> getColumnMappings() {
        return columnMappings;
    }
}
