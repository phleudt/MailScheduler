package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.email.Email;
import com.mailscheduler.domain.model.email.EmailMetadata;
import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.common.base.EntityData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RecipientScheduledEmailsMap(Map<EntityId<Recipient>, List<EntityData<Email, EmailMetadata>>> map) {

    public static RecipientScheduledEmailsMap empty() {
        return new RecipientScheduledEmailsMap(new HashMap<>());
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void add(EntityId<Recipient> id, EntityData<Email, EmailMetadata> email) {
        map.computeIfAbsent(id, k -> new ArrayList<>()).add(email);
    }

    public void addAll(EntityId<Recipient> id, List<EntityData<Email, EmailMetadata>> emails) {
        map.computeIfAbsent(id, k -> new ArrayList<>()).addAll(emails);
    }

    public void putAll(RecipientScheduledEmailsMap recipientScheduledEmailsMap) {
        recipientScheduledEmailsMap.map.forEach(this::addAll);
    }
}
