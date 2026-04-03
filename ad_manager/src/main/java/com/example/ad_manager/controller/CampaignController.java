package com.example.ad_manager.controller;


import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.response.CampaignCreateResponse;
import jakarta.validation.Valid;
import com.example.ad_manager.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

  private final CampaignService campaignService;
  private final CampaignMapper campaignMapper;

  @PostMapping
  public ResponseEntity<CampaignCreateResponse> createCampaign(
      @Valid @RequestBody CampaignCreateRequest request) {
    //TODO: get user name from security context
    String userName = "test";

    CampaignCreateReqDto campaignCreateReqDto =
        campaignMapper.requestToDto(request, false, userName);

    CampaignCreateResDto campaignCreateResDto =
        campaignService.createCampaign(campaignCreateReqDto);

    CampaignCreateResponse campaignCreateResponse =
        campaignMapper.dtoToResponse(campaignCreateResDto);

    return ResponseEntity.ok(campaignCreateResponse);
  }

  @PatchMapping("/{campaignId}/activate")
  public ResponseEntity<CampaignCreateResponse> activateCampaign(
      @PathVariable String campaignId) {
    CampaignCreateResDto campaignCreateResDto = campaignService.activateCampaign(campaignId);
    CampaignCreateResponse campaignCreateResponse =
        campaignMapper.dtoToResponse(campaignCreateResDto);

    return ResponseEntity.ok(campaignCreateResponse);
  }

  @PatchMapping("/{campaignId}/deactivate")
  public ResponseEntity<CampaignCreateResponse> deactivateCampaign(
      @PathVariable String campaignId) {
    CampaignCreateResDto campaignCreateResDto = campaignService.deactivateCampaign(campaignId);
    CampaignCreateResponse campaignCreateResponse =
        campaignMapper.dtoToResponse(campaignCreateResDto);

    return ResponseEntity.ok(campaignCreateResponse);
  }
}
