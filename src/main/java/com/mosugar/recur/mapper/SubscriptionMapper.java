package com.mosugar.recur.mapper;

import com.mosugar.recur.dto.request.SubscriptionRequest;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.entity.Subscription;
import com.mosugar.recur.util.BillingUtil;
import org.mapstruct.*;

import java.time.LocalDate;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        imports = {BillingUtil.class, LocalDate.class})
public interface SubscriptionMapper {

    @Mapping(target = "nextPaymentDate",
             expression = "java(BillingUtil.nextPaymentDate(subscription.getFirstPaymentDate(), subscription.getCycle(), LocalDate.now()))")
    SubscriptionResponse toResponse(Subscription subscription);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Subscription toEntity(SubscriptionRequest request);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(SubscriptionRequest request, @MappingTarget Subscription entity);
}
