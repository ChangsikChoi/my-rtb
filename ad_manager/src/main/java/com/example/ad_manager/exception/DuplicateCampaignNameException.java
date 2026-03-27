package com.example.ad_manager.exception;

public class DuplicateCampaignNameException extends RuntimeException {

  public DuplicateCampaignNameException(String campaignName) {
    super("campaign name already exists: " + campaignName);
  }
}
