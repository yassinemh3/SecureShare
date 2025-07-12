package com.secureshare.securefiles.file;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;
    private String storedFilename;
    private String contentType;
    private long size;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
