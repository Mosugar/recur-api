package com.mosugar.recur.mapper;

import com.mosugar.recur.dto.response.UserResponse;
import com.mosugar.recur.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    UserResponse toResponse(User user);
}
