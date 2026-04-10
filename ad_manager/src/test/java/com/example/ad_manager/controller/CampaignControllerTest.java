package com.example.ad_manager.controller;

import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignCreateRequest;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignRequest;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignResponse;
import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignHttpResponse;
import static com.example.ad_manager.fixture.CampaignTestFixtures.invalidCampaignCreateRequest;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ad_manager.exception.CampaignNotFoundException;
import com.example.ad_manager.exception.CampaignRedisSyncException;
import com.example.ad_manager.exception.CampaignStateConflictException;
import com.example.ad_manager.exception.CampaignTransactionException;
import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.exception.GlobalExceptionHandler;
import com.example.ad_manager.mapper.web.CampaignWebMapper;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.response.CampaignResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.ad_manager.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CampaignController.class)
@ContextConfiguration(classes = CampaignControllerTest.TestApplication.class)
class CampaignControllerTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import({CampaignController.class, GlobalExceptionHandler.class})
  static class TestApplication {
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CampaignService campaignService;

  @MockitoBean
  private CampaignWebMapper campaignWebMapper;

  @Test
  void givenLocalDateRequest_whenCreateCampaign_thenReturnOk() throws Exception {
    CampaignCreateRequestDto requestDto = campaignRequest("test-campaign-success");
    CampaignResponseDto responseDto = campaignResponse("campaign-1", false);
    CampaignResponse response = campaignHttpResponse("campaign-1", false);

    when(campaignWebMapper.createRequestToDto(any())).thenReturn(requestDto);
    when(campaignService.createCampaign(requestDto)).thenReturn(responseDto);
    when(campaignWebMapper.responseDtoToResponse(responseDto)).thenReturn(response);

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(campaignCreateRequest("test-campaign-success"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.startDate").value("2026-03-23"))
        .andExpect(jsonPath("$.endDate").value("2026-03-24"));
  }

  @Test
  void givenInvalidRequest_whenCreateCampaign_thenReturnBadRequest() throws Exception {
    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(invalidCampaignCreateRequest())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.validationErrors[*].message", hasItem("name is required")))
        .andExpect(jsonPath("$.validationErrors[*].message", hasItem("targetCpm must be positive")))
        .andExpect(jsonPath("$.validationErrors[*].message", hasItem("budget must be positive")))
        .andExpect(jsonPath("$.validationErrors[*].message",
            hasItem("startDate must be before or equal to endDate")))
        .andExpect(jsonPath("$.validationErrors[*].message",
            hasItem("target.minAge must be less than or equal to target.maxAge")))
        .andExpect(jsonPath("$.validationErrors[*].message", hasItem("creative.name is required")))
        .andExpect(jsonPath("$.validationErrors[*].message", hasItem("creative.imageUrl is required")))
        .andExpect(jsonPath("$.validationErrors[*].message", hasItem("creative.clickUrl is required")));
  }

  @Test
  void givenMalformedRequestBody_whenCreateCampaign_thenReturnBadRequest() throws Exception {
    String requestJson = """
        {
          "name": "test-campaign-malformed",
          "targetCpm": 10.5,
          "budget": 100.0,
          "startDate": "2026-03-23T00:00:00",
          "endDate": "2026-03-24",
          "target": {},
          "creative": {
            "name": "test-creative-malformed",
            "imageUrl": "https://example.com/a.png",
            "clickUrl": "https://example.com"
          }
        }
        """;

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
        .andExpect(jsonPath("$.message").value("Request body is invalid or malformed"));
  }

  @Test
  void givenDuplicateCampaignName_whenCreateCampaign_thenReturnConflict() throws Exception {
    when(campaignWebMapper.createRequestToDto(any()))
        .thenReturn(campaignRequest("test-campaign-duplicate"));

    when(campaignService.createCampaign(any()))
        .thenThrow(new DuplicateCampaignNameException("test-campaign-duplicate"));

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(campaignCreateRequest("test-campaign-duplicate"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_CAMPAIGN_NAME"))
        .andExpect(
            jsonPath("$.message").value("campaign name already exists: test-campaign-duplicate"));
  }

  @Test
  void givenInactiveCampaign_whenActivateCampaign_thenReturnOk() throws Exception {
    CampaignResponseDto responseDto = campaignResponse("campaign-1", true);
    CampaignResponse response = campaignHttpResponse("campaign-1", true);

    when(campaignService.activateCampaign("campaign-1")).thenReturn(responseDto);
    when(campaignWebMapper.responseDtoToResponse(responseDto)).thenReturn(response);

    mockMvc.perform(patch("/api/campaigns/campaign-1/activate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("campaign-1"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void givenMissingCampaign_whenActivateCampaign_thenReturnNotFound() throws Exception {
    when(campaignService.activateCampaign("campaign-missing"))
        .thenThrow(new CampaignNotFoundException("campaign-missing"));

    mockMvc.perform(patch("/api/campaigns/campaign-missing/activate"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("campaign not found: campaign-missing"));
  }

  @Test
  void givenAlreadyActiveCampaign_whenActivateCampaign_thenReturnConflict() throws Exception {
    when(campaignService.activateCampaign("campaign-1"))
        .thenThrow(CampaignStateConflictException.alreadyActive("campaign-1"));

    mockMvc.perform(patch("/api/campaigns/campaign-1/activate"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_ALREADY_ACTIVE"))
        .andExpect(jsonPath("$.message").value("campaign is already active: campaign-1"));
  }

  @Test
  void givenRedisSyncFailure_whenActivateCampaign_thenReturnInternalServerError() throws Exception {
    when(campaignService.activateCampaign("campaign-1"))
        .thenThrow(CampaignRedisSyncException.activationFailed(new RuntimeException("redis down")));

    mockMvc.perform(patch("/api/campaigns/campaign-1/activate"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_REDIS_SYNC_FAILED"))
        .andExpect(jsonPath("$.message").value("campaign activation failed during redis sync"));
  }

  @Test
  void givenTransactionFailure_whenActivateCampaign_thenReturnInternalServerError() throws Exception {
    when(campaignService.activateCampaign("campaign-1"))
        .thenThrow(CampaignTransactionException.activationFailed());

    mockMvc.perform(patch("/api/campaigns/campaign-1/activate"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_TRANSACTION_FAILED"))
        .andExpect(
            jsonPath("$.message").value("campaign activation failed during database transaction"));
  }

  @Test
  void givenActiveCampaign_whenDeactivateCampaign_thenReturnOk() throws Exception {
    CampaignResponseDto responseDto = campaignResponse("campaign-1", false);
    CampaignResponse response = campaignHttpResponse("campaign-1", false);

    when(campaignService.deactivateCampaign("campaign-1")).thenReturn(responseDto);
    when(campaignWebMapper.responseDtoToResponse(responseDto)).thenReturn(response);

    mockMvc.perform(patch("/api/campaigns/campaign-1/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("campaign-1"))
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void givenMissingCampaign_whenDeactivateCampaign_thenReturnNotFound() throws Exception {
    when(campaignService.deactivateCampaign("campaign-missing"))
        .thenThrow(new CampaignNotFoundException("campaign-missing"));

    mockMvc.perform(patch("/api/campaigns/campaign-missing/deactivate"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("campaign not found: campaign-missing"));
  }

  @Test
  void givenAlreadyInactiveCampaign_whenDeactivateCampaign_thenReturnConflict() throws Exception {
    when(campaignService.deactivateCampaign("campaign-1"))
        .thenThrow(CampaignStateConflictException.alreadyInactive("campaign-1"));

    mockMvc.perform(patch("/api/campaigns/campaign-1/deactivate"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_ALREADY_INACTIVE"))
        .andExpect(jsonPath("$.message").value("campaign is already inactive: campaign-1"));
  }

  @Test
  void givenRedisSyncFailure_whenDeactivateCampaign_thenReturnInternalServerError() throws Exception {
    when(campaignService.deactivateCampaign("campaign-1"))
        .thenThrow(CampaignRedisSyncException.deactivationFailed(new RuntimeException("redis down")));

    mockMvc.perform(patch("/api/campaigns/campaign-1/deactivate"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_REDIS_SYNC_FAILED"))
        .andExpect(jsonPath("$.message").value("campaign deactivation failed during redis sync"));
  }

  @Test
  void givenTransactionFailure_whenDeactivateCampaign_thenReturnInternalServerError() throws Exception {
    when(campaignService.deactivateCampaign("campaign-1"))
        .thenThrow(CampaignTransactionException.deactivationFailed());

    mockMvc.perform(patch("/api/campaigns/campaign-1/deactivate"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("CAMPAIGN_TRANSACTION_FAILED"))
        .andExpect(
            jsonPath("$.message").value("campaign deactivation failed during database transaction"));
  }

  private String toJson(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }
}
