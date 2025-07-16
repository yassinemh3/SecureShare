package com.secureshare.securefiles.file;

import com.secureshare.securefiles.dto.FileResponseDTO;
import com.secureshare.securefiles.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileService;
    private final FileRepository fileRepository;

    @PostMapping("/upload")
    public ResponseEntity<FileResponseDTO> upload(@RequestParam("file") MultipartFile file) {
        try {
            FileEntity saved = fileService.saveFile(file);
            return ResponseEntity.ok(FileResponseDTO.fromEntity(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<FileResponseDTO>> listUserFiles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        List<FileResponseDTO> files = fileRepository.findByUser(user).stream()
                .map(FileResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(files);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id) {
        try {
            FileEntity file = fileRepository.findById(id)
                    .orElseThrow(() -> new FileStorageService.FileNotFoundException(id));

            // Authorization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();

            if (!file.getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You're not allowed to access this file");
            }

            byte[] data = fileService.getFileContent(file);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .body(new ByteArrayResource(data));
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        try {
            fileService.deleteFile(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}