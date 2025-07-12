package com.secureshare.securefiles.file;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private LocalDateTime expiry;

    @ManyToOne
    @JoinColumn(name = "file_id")
    private FileEntity file;

    private String password; // optional, can be null
}
