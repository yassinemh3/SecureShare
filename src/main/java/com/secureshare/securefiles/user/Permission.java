package com.secureshare.securefiles.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Permission {

    // File-related permissions
    FILE_READ("file:read"),
    FILE_UPLOAD("file:upload"),
    FILE_DOWNLOAD("file:download"),
    FILE_DELETE("file:delete"),
    FILE_SHARE("file:share"),
    FILE_SEARCH("file:search"),

    ADMIN_DASHBOARD("admin:dashboard"),
    ADMIN_READ("admin:read"),
    ADMIN_UPDATE("admin:update"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    ;

    @Getter
    private final String permission;
}
