package com.nocountry.conversionflow.conversionflow_api.domain.entity;

import com.nocountry.conversionflow.conversionflow_api.domain.enums.LeadStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Tracking
    @Column(name = "gclid")
    private String gclid;

    @Column(name = "fbclid")
    private String fbclid;

    @Column(name = "fbp")
    private String fbp;

    @Column(name = "fbc")
    private String fbc;

    @Column(name = "utm_source")
    private String utmSource;

    @Column(name = "utm_campaign")
    private String utmCampaign;

    // Conversão
    @Column(name = "converted_at")
    private OffsetDateTime convertedAt;

    @Column(name = "converted_amount", precision = 15, scale = 2)
    private BigDecimal convertedAmount;

    protected Lead() { }

    public Lead(String externalId, String email) {
        this.externalId = externalId;
        this.email = email;
        this.status = LeadStatus.NEW;
        this.createdAt = OffsetDateTime.now();
    }

    // ===== Domain methods =====

    public void startCheckout() {
        if (this.status == LeadStatus.NEW) {
            this.status = LeadStatus.CHECKOUT_STARTED;
        }
    }

    public void markAsWon(BigDecimal amount) {
        this.status = LeadStatus.WON;
        this.convertedAt = OffsetDateTime.now();
        this.convertedAmount = amount;
    }

    public boolean isWon() {
        return this.status == LeadStatus.WON;
    }

    public void updateTracking(
            String gclid,
            String fbclid,
            String fbp,
            String fbc,
            String utmSource,
            String utmCampaign
    ) {
        // Non-destructive merge: keep existing non-blank values and fill only missing ones.
        this.gclid = mergeNonDestructive(this.gclid, gclid);
        this.fbclid = mergeNonDestructive(this.fbclid, fbclid);
        this.fbp = mergeNonDestructive(this.fbp, fbp);
        this.fbc = mergeNonDestructive(this.fbc, fbc);
        this.utmSource = mergeNonDestructive(this.utmSource, utmSource);
        this.utmCampaign = mergeNonDestructive(this.utmCampaign, utmCampaign);
    }

    private String mergeNonDestructive(String currentValue, String incomingValue) {
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        if (incomingValue == null || incomingValue.isBlank()) {
            return currentValue;
        }
        return incomingValue;
    }

    // ===== Getters =====

    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getEmail() { return email; }
    public LeadStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public String getGclid() { return gclid; }
    public String getFbclid() { return fbclid; }
    public String getFbp() { return fbp; }
    public String getFbc() { return fbc; }
    public String getUtmSource() { return utmSource; }
    public String getUtmCampaign() { return utmCampaign; }

    public OffsetDateTime getConvertedAt() { return convertedAt; }
    public BigDecimal getConvertedAmount() { return convertedAmount; }

    public void setStatus(LeadStatus status) { this.status = status; }
}
