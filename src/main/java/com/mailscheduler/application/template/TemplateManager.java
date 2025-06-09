package com.mailscheduler.application.template;

import com.mailscheduler.domain.repository.TemplateRepository;

public class TemplateManager {
    TemplateRepository templateRepository;

    public TemplateManager(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }


}
