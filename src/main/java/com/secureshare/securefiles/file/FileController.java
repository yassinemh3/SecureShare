package com.secureshare.securefiles.file;

import com.secureshare.securefiles.file.FileEntity;
import com.secureshare.securefiles.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileService;

    @PostMapping("/upload")
    public ResponseEntity<FileEntity> upload(@RequestParam("file") MultipartFile file) throws Exception {
        FileEntity saved = fileService.saveFile(file);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<FileEntity>> listUserFiles() {
        List<FileEntity> files = fileService.getUserFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) throws Exception {
        FileEntity file = fileService.getFileMetadata(id);

        //Authorization: Check if the file belongs to the current user
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!file.getUploadedBy().equals(currentUserEmail)) {
            throw new AccessDeniedException("You're not allowed to access this file.");
        }

        byte[] data = fileService.getFileContent(file);

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream"; // default fallback
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(new ByteArrayResource(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        FileEntity file = fileService.getFileMetadata(id);

        // Get authenticated user email
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 🛡️ Allow only uploader or admin to delete
        if (!file.getUploadedBy().equals(currentUserEmail) &&
                !SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("You are not authorized to delete this file.");
        }

        fileService.deleteFile(file); // Delete metadata and physical file if needed
        return ResponseEntity.noContent().build();
    }

}
