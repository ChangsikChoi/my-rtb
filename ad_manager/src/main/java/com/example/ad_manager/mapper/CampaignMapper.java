package com.example.ad_manager.mapper;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CreativeEntity;
import com.example.ad_manager.entity.TargetEntity;
import com.example.ad_manager.model.dto.CampaignCreateRequestDto;
import com.example.ad_manager.model.dto.CampaignResponseDto;
import com.example.ad_manager.model.dto.CreativeCreateRequestDto;
import com.example.ad_manager.model.dto.CreativeResponseDto;
import com.example.ad_manager.model.dto.TargetCreateRequestDto;
import com.example.ad_manager.model.dto.TargetResponseDto;
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.response.CampaignResponse;
import com.example.ad_manager.model.response.CreativeResponse;
import com.example.ad_manager.model.response.TargetResponse;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CreativeRedisEntity;
import com.example.ad_manager.redis.TargetRedisEntity;
import com.example.ad_manager.utils.MicroConverter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapper {

  public CampaignCreateRequestDto createRequestToDto(CampaignCreateRequest request) {
    return CampaignCreateRequestDto.builder()
        .name(request.name())
        .targetCpm(request.targetCpm())
        .budget(request.budget())
        .startDate(request.startDate().atStartOfDay())
        .endDate(request.endDate().atTime(LocalTime.of(23, 59, 59)))
        .target(TargetCreateRequestDto.builder()
            .os(request.target().os())
            .country(request.target().country())
            .minAge(request.target().minAge())
            .maxAge(request.target().maxAge())
            .build())
        .creative(CreativeCreateRequestDto.builder()
            .name(request.creative().name())
            .imageUrl(request.creative().imageUrl())
            .clickUrl(request.creative().clickUrl())
            .width(request.creative().width())
            .height(request.creative().height())
            .build())
        .build();
  }

  public CampaignEntity dtoToEntity(CampaignCreateRequestDto dto) {
    return CampaignEntity.builder()
        .name(dto.name())
        .targetCpm(dto.targetCpm())
        .budget(dto.budget())
        .startDate(dto.startDate())
        .endDate(dto.endDate())
        .target(TargetEntity.builder()
            .os(dto.target().os())
            .country(dto.target().country())
            .minAge(dto.target().minAge())
            .maxAge(dto.target().maxAge())
            .build())
        .creative(CreativeEntity.builder()
            .name(dto.creative().name())
            .imageUrl(dto.creative().imageUrl())
            .clickUrl(dto.creative().clickUrl())
            .width(dto.creative().width())
            .height(dto.creative().height())
            .build())
        .build();
  }

  public CampaignRedisEntity entityToRedisEntity(CampaignEntity entity) {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    return CampaignRedisEntity.builder()
        .id(entity.getId())
        .name(entity.getName())
        .targetCpmMicro(MicroConverter.toMicro(entity.getTargetCpm()))
        .budgetMicro(MicroConverter.toMicro(entity.getBudget()))
        .remainingBudgetMicro(MicroConverter.toMicro(entity.getBudget()))
        .startDate(entity.getStartDate().format(dateTimeFormatter))
        .endDate(entity.getEndDate().format(dateTimeFormatter))
        .target(TargetRedisEntity.builder()
            .os(entity.getTarget().getOs())
            .country(entity.getTarget().getCountry())
            .minAge(entity.getTarget().getMinAge())
            .maxAge(entity.getTarget().getMaxAge())
            .build())
        .creative(CreativeRedisEntity.builder()
            .id(entity.getCreative().getId())
            .imageUrl(entity.getCreative().getImageUrl())
            .clickUrl(entity.getCreative().getClickUrl())
            .width(entity.getCreative().getWidth())
            .height(entity.getCreative().getHeight())
            .build())
        .build();
  }

  public CampaignResponseDto entityToResponseDto(CampaignEntity entity) {
    return CampaignResponseDto.builder()
        .id(entity.getId())
        .name(entity.getName())
        .targetCpm(entity.getTargetCpm())
        .budget(entity.getBudget())
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .active(entity.isActive())
        .target(TargetResponseDto.builder()
            .id(entity.getTarget().getId())
            .os(entity.getTarget().getOs())
            .country(entity.getTarget().getCountry())
            .minAge(entity.getTarget().getMinAge())
            .maxAge(entity.getTarget().getMaxAge())
            .createdAt(entity.getTarget().getCreatedAt())
            .updatedAt(entity.getTarget().getUpdatedAt())
            .build())
        .creative(CreativeResponseDto.builder()
            .id(entity.getCreative().getId())
            .name(entity.getCreative().getName())
            .imageUrl(entity.getCreative().getImageUrl())
            .clickUrl(entity.getCreative().getClickUrl())
            .width(entity.getCreative().getWidth())
            .height(entity.getCreative().getHeight())
            .createdAt(entity.getCreative().getCreatedAt())
            .updatedAt(entity.getCreative().getUpdatedAt())
            .build())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public CampaignResponse responseDtoToResponse(CampaignResponseDto dto) {
    return CampaignResponse.builder()
        .id(dto.id())
        .name(dto.name())
        .targetCpm(dto.targetCpm())
        .budget(dto.budget())
        .startDate(dto.startDate().toLocalDate())
        .endDate(dto.endDate().toLocalDate())
        .active(dto.active())
        .target(TargetResponse.builder()
            .id(dto.target().id())
            .os(dto.target().os())
            .country(dto.target().country())
            .minAge(dto.target().minAge())
            .maxAge(dto.target().maxAge())
            .createdAt(dto.target().createdAt())
            .updatedAt(dto.target().updatedAt())
            .build())
        .creative(CreativeResponse.builder()
            .id(dto.creative().id())
            .name(dto.creative().name())
            .imageUrl(dto.creative().imageUrl())
            .clickUrl(dto.creative().clickUrl())
            .width(dto.creative().width())
            .height(dto.creative().height())
            .createdAt(dto.creative().createdAt())
            .updatedAt(dto.creative().updatedAt())
            .build())
        .createdAt(dto.createdAt())
        .updatedAt(dto.updatedAt())
        .build();
  }
}
