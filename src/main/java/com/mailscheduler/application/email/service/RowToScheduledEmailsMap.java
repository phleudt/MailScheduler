package com.mailscheduler.application.email.service;

import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.util.List;
import java.util.Map;

public record RowToScheduledEmailsMap(
        Map<Integer, List<EntityData<Email, EmailMetadata>>> scheduledEmailsMap
) {
}
