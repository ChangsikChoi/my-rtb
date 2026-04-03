package com.example.ad_manager.exception;

public class DuplicateCampaignNameException extends ApiException {

  public DuplicateCampaignNameException(String campaignName) {
    super("DUPLICATE_CAMPAIGN_NAME", "campaign name already exists: " + campaignName);
  }
}
