package com.nqt.football_management.mapper;

import com.nqt.football_management.constants.Gender;
import com.nqt.football_management.entity.User;
import com.nqt.football_management.model.request.AdminUserPageRequest;
import com.nqt.football_management.model.request.RegisterRequest;
import com.nqt.football_management.model.response.AdminUserResponse;

import com.nqt.football_management.model.response.UserResponse;
import org.mapstruct.*;

import java.util.Locale;

@Mapper(componentModel = "spring")
public interface UserMapper {

    AdminUserResponse toAdminResponse(User user);



    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderFromString")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);




    @Mapping(target = "accessToken", ignore = true)
    UserResponse toResponse(User user);

    //update
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            ignoreByDefault = true
    )
    //nullValuePropertyMappingStrategy là khi update những fields cần thay đổi sẽ bị thay đổi còn những fields còn lại thì bỏ qua
    User updateEntity(AdminUserPageRequest request, @MappingTarget User user);

    @Named("genderFromString")
    default Gender genderFromString(String gender) {
        if (gender == null) {
            return null;
        }
        return Gender.valueOf(gender.trim().toUpperCase(Locale.ROOT));
    }
}
