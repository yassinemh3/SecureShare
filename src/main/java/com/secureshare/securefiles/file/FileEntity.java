package com.secureshare.securefiles.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secureshare.securefiles.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore  // Prevent serialization of the user
    private User user;

    @OneToMany(
            mappedBy = "file",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    @ToString.Exclude
    @JsonIgnore
    private List<SharedFile> sharedFiles = new ArrayList<>();

    // Helper method to manage bidirectional relationship
    public void addSharedFile(SharedFile sharedFile) {
        sharedFiles.add(sharedFile);
        sharedFile.setFile(this);
    }

    public void removeSharedFile(SharedFile sharedFile) {
        sharedFiles.remove(sharedFile);
        sharedFile.setFile(null);
    }
}