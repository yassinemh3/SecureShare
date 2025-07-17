package com.secureshare.securefiles.file;

import com.secureshare.securefiles.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedFileRepository extends JpaRepository<SharedFile, Long> {

    Optional<SharedFile> findByToken(String token);

    Page<SharedFile> findBySharedBy(User user, Pageable pageable);

    boolean existsByTokenAndSharedBy(String token, User user);

    void deleteByFile(FileEntity file);

}