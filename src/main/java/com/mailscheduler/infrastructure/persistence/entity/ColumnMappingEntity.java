package com.mailscheduler.infrastructure.persistence.entity;

import com.mailscheduler.domain.model.common.vo.spreadsheet.ColumnMapping;

public class ColumnMappingEntity extends TableEntity {
    private final Long configId;
    private final String type;
    private final String columnName;
    private final String columnReference;

    public ColumnMappingEntity(Long id, Long configId, String type, String columnName, String columnReference) {
        setId(id);
        this.configId = configId;
        this.type = type;
        this.columnName = columnName;
        this.columnReference = columnReference;
    }

    public Long getConfigId() {
        return configId;
    }

    public String getType() {
        return type;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getColumnReference() {
        return columnReference;
    }

    public ColumnMapping.MappingType getMappingType() {
        return ColumnMapping.MappingType.valueOf(type);
    }
}