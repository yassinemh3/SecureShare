package com.secureshare.securefiles.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Entity
@Table(name = "shared_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 512)
    private String token;

    private Instant expiry;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(length = 512)
    @JsonIgnore
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_user_id")
    @ToString.Exclude
    @JsonIgnore
    private User sharedBy;

    @Column(name = "is_active", nullable = false)
    @Builder.Default // This is crucial
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (this.expiry == null) {
            this.expiry = Instant.now().plusSeconds(TimeUnit.HOURS.toSeconds(24));
        }
    }

    public boolean isExpired() {
        return expiry != null && Instant.now().isAfter(expiry);
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}