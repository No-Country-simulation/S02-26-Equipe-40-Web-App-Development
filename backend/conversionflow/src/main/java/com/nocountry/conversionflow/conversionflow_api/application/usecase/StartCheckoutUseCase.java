package com.nocountry.conversionflow.conversionflow_api.application.usecase;

import com.nocountry.conversionflow.conversionflow_api.application.exception.AppConfigurationException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.InvalidInputException;
import com.nocountry.conversionflow.conversionflow_api.application.exception.LeadNotFoundException;
import com.nocountry.conversionflow.conversionflow_api.config.properties.StripeProperties;
import com.nocountry.conversionflow.conversionflow_api.domain.entity.Lead;
import com.nocountry.conversionflow.conversionflow_api.domain.repository.LeadRepository;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripeCheckoutSession;
import com.nocountry.conversionflow.conversionflow_api.infrastructure.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Component
public class StartCheckoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartCheckoutUseCase.class);

    private final StripeProperties stripeProperties;
    private final LeadRepository leadRepository;
    private final StripeClient stripeClient;

    public StartCheckoutUseCase(
            StripeProperties stripeProperties,
            LeadRepository leadRepository,
            StripeClient stripeClient
    ) {
        this.stripeProperties = stripeProperties;
        this.leadRepository = leadRepository;
        this.stripeClient = stripeClient;
    }

    @Transactional
    public String execute(
            Long leadId,
            String plan,
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) {
        log.info("usecase.startCheckout.start leadId={} plan={}", leadId, plan);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new LeadNotFoundException("Lead not found: " + leadId));

        lead.updateTracking(gclid, fbclid, fbp, fbc, utmSource, utmCampaign);
        lead.startCheckout();
        leadRepository.save(lead);

        String normalizedPlan = plan.trim().toLowerCase(Locale.ROOT);
        String priceId = resolvePriceId(normalizedPlan);

        StripeCheckoutSession checkoutSession = stripeClient.createCheckoutSession(
                ensureSessionIdPlaceholder(stripeProperties.getSuccessUrl()),
                stripeProperties.getCancelUrl(),
                priceId,
                Map.of(
                        "leadId", String.valueOf(lead.getId()),
                        "plan", normalizedPlan
                )
        );
        if (checkoutSession.url() == null || checkoutSession.url().isBlank()) {
            throw new AppConfigurationException("Stripe did not return checkout URL");
        }

        log.info("usecase.startCheckout.success leadId={} plan={} sessionId={}",
                leadId, normalizedPlan, checkoutSession.sessionId());
        return checkoutSession.url();
    }

    private String ensureSessionIdPlaceholder(String successUrl) {
        String placeholder = "{CHECKOUT_SESSION_ID}";
        String param = "session_id=" + placeholder;
        if (successUrl == null || successUrl.isBlank()) {
            throw new AppConfigurationException("stripe.success-url is not configured");
        }
        if (successUrl.contains(param)) {
            return successUrl;
        }
        return successUrl + (successUrl.contains("?") ? "&" : "?") + param;
    }

    private String resolvePriceId(String normalizedPlan) {
        Map<String, String> prices = stripeProperties.getPrices();
        if (prices == null || prices.isEmpty()) {
            throw new AppConfigurationException("stripe.prices is not configured");
        }

        String priceId = prices.get(normalizedPlan);
        if (priceId == null || priceId.isBlank()) {
            throw new InvalidInputException("No Stripe priceId configured for plan: " + normalizedPlan);
        }

        return priceId;
    }
}
