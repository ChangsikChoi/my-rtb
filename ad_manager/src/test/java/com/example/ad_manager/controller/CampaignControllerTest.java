package com.example.ad_manager.controller;

import static com.example.ad_manager.fixture.CampaignTestFixtures.campaignResponse;
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
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

  private MockMvc mockMvc;

  @Mock
  private CampaignService campaignService;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(
            new CampaignController(campaignService, new CampaignMapper()))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setMessageConverters(new MappingJackson2HttpMessageConverter())
        .setValidator(validator)
        .build();
  }

  @Test
  void givenLocalDateRequest_whenCreateCampaign_thenReturnOk() throws Exception {
    String requestJson = """
        {
          "name": "test-campaign-success",
          "targetCpm": 10.5,
          "budget": 100.0,
          "startDate": "2026-03-23",
          "endDate": "2026-03-24",
          "target": {
            "os": "Android",
            "country": "KR",
            "minAge": 20,
            "maxAge": 40
          },
          "creative": {
            "name": "test-creative-success",
            "imageUrl": "https://example.com/a.png",
            "clickUrl": "https://example.com",
            "width": 300,
            "height": 250
          }
        }
        """;

    when(campaignService.createCampaign(any()))
        .thenReturn(campaignResponse("campaign-1", false));

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.startDate").value("2026-03-23"))
        .andExpect(jsonPath("$.endDate").value("2026-03-24"));
  }

  @Test
  void givenInvalidRequest_whenCreateCampaign_thenReturnBadRequest() throws Exception {
    String requestJson = """
        {
          "name": "",
          "targetCpm": -10,
          "budget": 0,
          "startDate": "2026-03-24",
          "endDate": "2026-03-23",
          "target": {
            "minAge": 30,
            "maxAge": 20
          },
          "creative": {
            "name": "",
            "imageUrl": "",
            "clickUrl": "",
            "width": -1,
            "height": -1
          }
        }
        """;

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
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
    String requestJson = """
        {
          "name": "test-campaign-duplicate",
          "targetCpm": 10.5,
          "budget": 100.0,
          "startDate": "2026-03-23",
          "endDate": "2026-03-24",
          "target": {},
          "creative": {
            "name": "test-creative-duplicate",
            "imageUrl": "https://example.com/a.png",
            "clickUrl": "https://example.com"
          }
        }
        """;

    when(campaignService.createCampaign(any()))
        .thenThrow(new DuplicateCampaignNameException("test-campaign-duplicate"));

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DUPLICATE_CAMPAIGN_NAME"))
        .andExpect(
            jsonPath("$.message").value("campaign name already exists: test-campaign-duplicate"));
  }

  @Test
  void givenInactiveCampaign_whenActivateCampaign_thenReturnOk() throws Exception {
    when(campaignService.activateCampaign("campaign-1"))
        .thenReturn(campaignResponse("campaign-1", true));

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
    when(campaignService.deactivateCampaign("campaign-1"))
        .thenReturn(campaignResponse("campaign-1", false));

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
}
