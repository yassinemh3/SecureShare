package com.secureshare.securefiles.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedFileRepository extends JpaRepository<SharedFile, Long> {
    Optional<SharedFile> findByToken(String token);
    void deleteByFile(FileEntity file);
}
