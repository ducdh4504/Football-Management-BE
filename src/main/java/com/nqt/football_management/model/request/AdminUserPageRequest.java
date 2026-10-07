package com.nqt.football_management.model.request;


import com.nqt.football_management.constants.Role;
import com.nqt.football_management.constants.UserStatus;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AdminUserPageRequest extends PageRequest {
    private UserStatus status;
    private Role role;
}
