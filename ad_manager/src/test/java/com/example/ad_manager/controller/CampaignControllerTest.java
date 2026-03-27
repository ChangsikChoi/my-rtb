package com.example.ad_manager.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ad_manager.exception.DuplicateCampaignNameException;
import com.example.ad_manager.exception.GlobalExceptionHandler;
import com.example.ad_manager.mapper.CampaignMapper;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.model.dto.CreativeCreateResDto;
import com.example.ad_manager.model.dto.TargetCreateResDto;
import com.example.ad_manager.service.CampaignService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        .thenReturn(CampaignCreateResDto.builder()
            .id("campaign-1")
            .name("test-campaign-success")
            .targetCpm(BigDecimal.valueOf(10.5))
            .budget(BigDecimal.valueOf(100.0))
            .startDate(LocalDateTime.of(2026, 3, 23, 0, 0, 0))
            .endDate(LocalDateTime.of(2026, 3, 24, 23, 59, 59))
            .active(true)
            .owner("test")
            .target(TargetCreateResDto.builder()
                .id("target-1")
                .os("Android")
                .country("KR")
                .minAge(20)
                .maxAge(40)
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
                .build())
            .creative(CreativeCreateResDto.builder()
                .id("creative-1")
                .name("test-creative-success")
                .imageUrl("https://example.com/a.png")
                .clickUrl("https://example.com")
                .width(300)
                .height(250)
                .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
                .build())
            .createdAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
            .updatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0))
            .build());

    mockMvc.perform(post("/api/campaigns")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
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
        .andExpect(jsonPath("$.errors[*].message", hasItem("name is required")))
        .andExpect(jsonPath("$.errors[*].message", hasItem("targetCpm must be positive")))
        .andExpect(jsonPath("$.errors[*].message", hasItem("budget must be positive")))
        .andExpect(jsonPath("$.errors[*].message",
            hasItem("startDate must be before or equal to endDate")))
        .andExpect(jsonPath("$.errors[*].message",
            hasItem("target.minAge must be less than or equal to target.maxAge")))
        .andExpect(jsonPath("$.errors[*].message", hasItem("creative.name is required")))
        .andExpect(jsonPath("$.errors[*].message", hasItem("creative.imageUrl is required")))
        .andExpect(jsonPath("$.errors[*].message", hasItem("creative.clickUrl is required")));
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
}
