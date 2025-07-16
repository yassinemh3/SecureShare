package com.secureshare.securefiles.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.secureshare.securefiles.user.Permission.*;

@RequiredArgsConstructor
public enum Role {

        USER(Set.of(
                FILE_UPLOAD,
                FILE_DOWNLOAD,
                FILE_DELETE,
                FILE_SHARE
        )),
        ADMIN(Set.of(
                FILE_UPLOAD,
                FILE_DOWNLOAD,
                FILE_DELETE,
                FILE_SHARE,
                ADMIN_DASHBOARD,
                ADMIN_READ,
                ADMIN_CREATE,
                ADMIN_UPDATE,
                ADMIN_DELETE
        ));

        @Getter
        private final Set<Permission> permissions;

        public List<SimpleGrantedAuthority> getAuthorities() {
                var authorities = getPermissions()
                                .stream()
                                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                                .collect(Collectors.toList());
                authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
                return authorities;
        }
}
