package com.secureshare.securefiles.file;
import java.util.List;

import com.secureshare.securefiles.file.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findAllByUploadedBy(String uploadedBy);
}

