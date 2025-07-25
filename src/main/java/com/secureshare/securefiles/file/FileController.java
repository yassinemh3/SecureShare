package com.secureshare.securefiles.file;

import com.secureshare.securefiles.dto.FileResponseDTO;
import com.secureshare.securefiles.service.FileValidator;
import com.secureshare.securefiles.user.User;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.xml.bind.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
// For Specifications
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

// For pagination
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

// For date handling
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileService;
    private final FileRepository fileRepository;
    private final FileValidator fileValidator;

    @PostMapping("/upload")
    @RateLimiter(name = "fileUpload", fallbackMethod = "uploadRateLimitExceeded")
    @PreAuthorize("hasAuthority('file:upload')")
    public ResponseEntity<FileResponseDTO> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws ValidationException {

        fileValidator.validate(file);
        FileEntity saved = fileService.saveFile(file, user);
        return ResponseEntity.ok(FileResponseDTO.fromEntity(saved));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('file:read')")
    public ResponseEntity<List<FileResponseDTO>> listUserFiles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        List<FileResponseDTO> files = fileRepository.findByUser(user).stream()
                .map(FileResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@fileSecurityService.canAccessFile(#id, authentication.principal)")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        FileEntity file = fileRepository.findById(id)
                .orElseThrow(() -> new FileStorageService.FileNotFoundException(id));

        byte[] content = fileService.getFileContent(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(new ByteArrayResource(content));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@fileSecurityService.canDeleteFile(#id, authentication.principal)")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        try {
            fileService.deleteFile(id, user);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            throw new AccessDeniedException(e.getMessage());
        } catch (FileStorageService.FileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('file:search')")
    public ResponseEntity<List<FileResponseDTO>> searchFiles(
            @RequestParam(required = false) String filename,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        User user = getCurrentUser();

        Page<FileEntity> files = fileRepository.findAll(
                createSearchSpecification(user, filename, contentType, minSize, maxSize, startDate, endDate),
                pageable
        );

        return ResponseEntity.ok(
                files.getContent()
                        .stream()
                        .map(FileResponseDTO::fromEntity)
                        .toList()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private Specification<FileEntity> createSearchSpecification(
            User user,
            String filename,
            String contentType,
            Long minSize,
            Long maxSize,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory user filter
            predicates.add(cb.equal(root.get("user"), user));

            if (filename != null && !filename.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("originalFilename")),
                        "%" + filename.toLowerCase() + "%"
                ));
            }

            if (contentType != null && !contentType.isBlank()) {
                predicates.add(cb.equal(root.get("contentType"), contentType));
            }

            if (minSize != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("size"), minSize));
            }

            if (maxSize != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("size"), maxSize));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("uploadedAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("uploadedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public ResponseEntity<String> uploadRateLimitExceeded(MultipartFile file, Exception ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Upload rate limit exceeded. Please try again later.");
    }
}