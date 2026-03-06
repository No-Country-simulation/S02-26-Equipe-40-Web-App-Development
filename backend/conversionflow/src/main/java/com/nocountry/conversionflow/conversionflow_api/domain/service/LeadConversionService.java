package com.nocountry.conversionflow.conversionflow_api.domain.service;

import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LeadConversionService {

    public boolean markAsWon(Lead lead, BigDecimal amount) {
        if (lead.isWon()) {
            return false;
        }

        lead.markAsWon(amount);
        return true;
    }
}
