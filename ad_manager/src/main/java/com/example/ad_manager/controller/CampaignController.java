package com.example.ad_manager.controller;


import com.example.ad_manager.model.CampaignCreateReqDto;
import com.example.ad_manager.model.CampaignCreateRequest;
import com.example.ad_manager.model.CampaignCreateResponse;
import com.example.ad_manager.service.CampaignService;
import lombok.RequiredArgsConstructor;
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

    @PostMapping
    public ResponseEntity<CampaignCreateResponse> createCampaign(@RequestBody CampaignCreateRequest request) {
        //TODO: get user name from security context
        String userName = "test";

        CampaignCreateReqDto campaignCreateReqDto = CampaignCreateReqDto.requestToDto(request, true, userName);
        CampaignCreateResponse campaignCreateResponse = campaignService.createCampaign(campaignCreateReqDto);

        return ResponseEntity.ok(campaignCreateResponse);
    }
}
