package com.mailscheduler.application.email.scheduling;

import com.mailscheduler.domain.model.recipient.Recipient;
import com.mailscheduler.domain.model.schedule.PlanWithTemplate;

import java.util.*;

public class PlanWithTemplatesRecipientsMap {
    private final Map<PlanWithTemplate, List<Recipient>> planToRecipientsMap;

    public PlanWithTemplatesRecipientsMap() {
        this.planToRecipientsMap = new HashMap<>();
    }

    public PlanWithTemplatesRecipientsMap(List<PlanWithTemplate> planWithTemplates) {
        this.planToRecipientsMap = new HashMap<>();
        for (PlanWithTemplate planWithTemplate : planWithTemplates) {
            this.planToRecipientsMap.put(planWithTemplate, new ArrayList<>());
        }
    }

    public void addRecipient(PlanWithTemplate plan, Recipient recipient) {
        planToRecipientsMap.computeIfAbsent(plan, k -> new ArrayList<>()).add(recipient);
    }

    public void addRecipients(PlanWithTemplate plan, List<Recipient> recipients) {
        planToRecipientsMap.computeIfAbsent(plan, k -> new ArrayList<>()).addAll(recipients);
    }

    public List<Recipient> getRecipientsForPlan(PlanWithTemplate plan) {
        return planToRecipientsMap.getOrDefault(plan, List.of());
    }

    public Map<PlanWithTemplate, List<Recipient>> getMapping() {
        return Map.copyOf(planToRecipientsMap);
    }

    public boolean isEmpty() {
        return planToRecipientsMap.isEmpty() ||
                planToRecipientsMap.keySet().stream().
                        allMatch(plan -> plan.getStepsWithTemplates().isEmpty());
    }
}