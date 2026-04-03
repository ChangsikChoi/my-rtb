package com.example.ad_manager.exception;

public class CampaignStateConflictException extends ApiException {

  private CampaignStateConflictException(String code, String message) {
    super(code, message);
  }

  public static CampaignStateConflictException alreadyActive(String campaignId) {
    return new CampaignStateConflictException(
        "CAMPAIGN_ALREADY_ACTIVE",
        "campaign is already active: " + campaignId
    );
  }

  public static CampaignStateConflictException alreadyInactive(String campaignId) {
    return new CampaignStateConflictException(
        "CAMPAIGN_ALREADY_INACTIVE",
        "campaign is already inactive: " + campaignId
    );
  }

}
