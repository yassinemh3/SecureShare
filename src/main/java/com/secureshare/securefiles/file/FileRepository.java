package com.secureshare.securefiles.file;

import com.secureshare.securefiles.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileRepository extends
        JpaRepository<FileEntity, Long>,
        JpaSpecificationExecutor<FileEntity> {

    // Basic user-scoped queries
    List<FileEntity> findByUser(User user);
    List<FileEntity> findByUserAndOriginalFilenameContainingIgnoreCase(User user, String filename);

    // Advanced search methods
    @Query("SELECT f FROM FileEntity f WHERE " +
            "f.user = :user AND " +
            "(:filename IS NULL OR LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :filename, '%'))) AND " +
            "(:contentType IS NULL OR f.contentType = :contentType) AND " +
            "(:minSize IS NULL OR f.size >= :minSize) AND " +
            "(:maxSize IS NULL OR f.size <= :maxSize) AND " +
            "(:startDate IS NULL OR f.uploadedAt >= :startDate) AND " +
            "(:endDate IS NULL OR f.uploadedAt <= :endDate)")
    List<FileEntity> advancedSearch(
            @Param("user") User user,
            @Param("filename") String filename,
            @Param("contentType") String contentType,
            @Param("minSize") Long minSize,
            @Param("maxSize") Long maxSize,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Statistics queries (for dashboard)
    @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.user = :user")
    long countByUser(@Param("user") User user);

    @Query("SELECT SUM(f.size) FROM FileEntity f WHERE f.user = :user")
    long sumStorageUsedByUser(@Param("user") User user);

    // For cleanup operations
    List<FileEntity> findByUploadedAtBefore(LocalDateTime cutoffDate);
}