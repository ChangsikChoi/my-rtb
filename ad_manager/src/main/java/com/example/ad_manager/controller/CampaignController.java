package com.example.ad_manager.controller;

import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.response.CampaignResponse;
import com.example.ad_manager.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  public ResponseEntity<CampaignResponse> createCampaign(
      @Valid @RequestBody CampaignCreateRequest request) {
    CampaignCreateRequestDto campaignCreateReqDto = campaignMapper.createRequestToDto(request);

    CampaignResponseDto campaignResponseDto = campaignService.createCampaign(campaignCreateReqDto);
    CampaignResponse campaignResponse = campaignMapper.responseDtoToResponse(campaignResponseDto);

    return ResponseEntity.status(HttpStatus.CREATED).body(campaignResponse);
  }

  @PatchMapping("/{campaignId}/activate")
  public ResponseEntity<CampaignResponse> activateCampaign(
      @PathVariable String campaignId) {
    CampaignResponseDto campaignResponseDto = campaignService.activateCampaign(campaignId);
    CampaignResponse campaignResponse = campaignMapper.responseDtoToResponse(campaignResponseDto);

    return ResponseEntity.ok(campaignResponse);
  }

  @PatchMapping("/{campaignId}/deactivate")
  public ResponseEntity<CampaignResponse> deactivateCampaign(
      @PathVariable String campaignId) {
    CampaignResponseDto campaignResponseDto = campaignService.deactivateCampaign(campaignId);
    CampaignResponse campaignResponse = campaignMapper.responseDtoToResponse(campaignResponseDto);

    return ResponseEntity.ok(campaignResponse);
  }
}
