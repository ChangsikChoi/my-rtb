package com.example.ad_manager.mapper;

import com.example.ad_manager.entity.CampaignEntity;
import com.example.ad_manager.entity.CreativeEntity;
import com.example.ad_manager.entity.TargetEntity;
import com.example.ad_manager.model.dto.CampaignCreateReqDto;
import com.example.ad_manager.model.dto.CampaignCreateResDto;
import com.example.ad_manager.model.dto.CreativeCreateResDto;
import com.example.ad_manager.model.dto.TargetCreateResDto;
import com.example.ad_manager.model.request.CampaignCreateRequest;
import com.example.ad_manager.model.dto.CreativeCreateReqDto;
import com.example.ad_manager.model.dto.TargetCreateReqDto;
import com.example.ad_manager.model.response.CampaignCreateResponse;
import com.example.ad_manager.model.response.CreativeCreateResponse;
import com.example.ad_manager.model.response.TargetCreateResponse;
import com.example.ad_manager.redis.CampaignRedisEntity;
import com.example.ad_manager.redis.CreativeRedisEntity;
import com.example.ad_manager.redis.TargetRedisEntity;
import com.example.ad_manager.utils.MicroConverter;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class CampaignMapper {

  public CampaignCreateReqDto requestToDto(CampaignCreateRequest request, boolean active,
      String owner) {
    return CampaignCreateReqDto.builder()
        .name(request.name())
        .targetCpm(request.targetCpm())
        .budget(request.budget())
        .startDate(request.startDate())
        .endDate(request.endDate())
        .target(TargetCreateReqDto.builder()
            .os(request.target().os())
            .country(request.target().country())
            .minAge(request.target().minAge())
            .maxAge(request.target().maxAge())
            .build())
        .creative(CreativeCreateReqDto.builder()
            .name(request.creative().name())
            .imageUrl(request.creative().imageUrl())
            .clickUrl(request.creative().clickUrl())
            .width(request.creative().width())
            .height(request.creative().height())
            .build())
        .active(active)
        .owner(owner)
        .build();
  }

  public CampaignEntity dtoToEntity(CampaignCreateReqDto dto) {
    return CampaignEntity.builder()
        .name(dto.name())
        .targetCpm(dto.targetCpm())
        .budget(dto.budget())
        .startDate(dto.startDate())
        .endDate(dto.endDate())
        .active(dto.active())
        .owner(dto.owner())
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

  public CampaignCreateResDto entityToDto(CampaignEntity entity) {
    return CampaignCreateResDto.builder()
        .id(entity.getId())
        .name(entity.getName())
        .targetCpm(entity.getTargetCpm())
        .budget(entity.getBudget())
        .startDate(entity.getStartDate())
        .endDate(entity.getEndDate())
        .active(entity.isActive())
        .owner(entity.getOwner())
        .target(TargetCreateResDto.builder()
            .id(entity.getTarget().getId())
            .os(entity.getTarget().getOs())
            .country(entity.getTarget().getCountry())
            .minAge(entity.getTarget().getMinAge())
            .maxAge(entity.getTarget().getMaxAge())
            .createdAt(entity.getTarget().getCreatedAt())
            .updatedAt(entity.getTarget().getUpdatedAt())
            .build())
        .creative(CreativeCreateResDto.builder()
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

  public CampaignCreateResponse dtoToResponse(CampaignCreateResDto dto) {
    return CampaignCreateResponse.builder()
        .id(dto.id())
        .name(dto.name())
        .targetCpm(dto.targetCpm())
        .budget(dto.budget())
        .startDate(dto.startDate())
        .endDate(dto.endDate())
        .active(dto.active())
        .owner(dto.owner())
        .target(TargetCreateResponse.builder()
            .id(dto.target().id())
            .os(dto.target().os())
            .country(dto.target().country())
            .minAge(dto.target().minAge())
            .maxAge(dto.target().maxAge())
            .createdAt(dto.target().createdAt())
            .updatedAt(dto.target().updatedAt())
            .build())
        .creative(CreativeCreateResponse.builder()
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
