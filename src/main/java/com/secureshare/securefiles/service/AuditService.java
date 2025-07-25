package com.secureshare.securefiles.service;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {
    public void logUpload(User user, FileEntity file) {
        log.info("File upload - User: {}, FileID: {}, Filename: {}, Size: {}",
                user.getUsername(),
                file.getId(),
                file.getOriginalFilename(),
                file.getSize());
    }

    public void logDownload(User user, FileEntity file) {
        log.info("File download - User: {}, FileID: {}",
                user.getUsername(),
                file.getId());
    }

    public void logDeletion(User user, FileEntity file) {
        log.info("File deletion - User: {}, FileID: {}",
                user.getUsername(),
                file.getId());
    }
}