package com.secureshare.securefiles.file;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.secureshare.securefiles.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_files") // Explicit table name
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

    @Column(length = 512) // Space for encrypted password
    @JsonIgnore // Never expose in responses
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore // Prevent circular references
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_user_id")
    @ToString.Exclude
    @JsonIgnore
    private User sharedBy;

    // Helper methods
    public boolean isExpired() {
        return expiry != null && Instant.now().isAfter(expiry);
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}