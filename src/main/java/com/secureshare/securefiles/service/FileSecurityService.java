package com.secureshare.securefiles.service;

import com.secureshare.securefiles.file.FileRepository;
import com.secureshare.securefiles.user.User;
import org.springframework.stereotype.Service;

@Service
public class FileSecurityService {

    private final FileRepository fileRepository;

    public FileSecurityService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Checks if a user can access a specific file
     * @param fileId ID of the file to check
     * @param user The user attempting access
     * @return true if user is owner or has been shared the file
     */
    public boolean canAccessFile(Long fileId, User user) {
        if (user == null) return false;

        return fileRepository.findById(fileId)
                .map(file -> file.getUser().getId().equals(user.getId()) ||
                        file.getSharedFiles().stream()
                                .anyMatch(sf -> sf.getSharedBy().getId().equals(user.getId())))
                .orElse(false);
    }

    /**
     * Checks if a user can delete a specific file
     * @param fileId ID of the file to check
     * @param user The user attempting deletion
     * @return true only if user is the owner of the file
     */
    public boolean canDeleteFile(Long fileId, User user) {
        if (user == null) return false;

        return fileRepository.findById(fileId)
                .map(file -> file.getUser().getId().equals(user.getId()))
                .orElse(false);
    }
}