package com.example.ad_manager.exception;

public class CampaignNotFoundException extends ApiException {

  public CampaignNotFoundException(String campaignId) {
    super("CAMPAIGN_NOT_FOUND", "campaign not found: " + campaignId);
  }
}
