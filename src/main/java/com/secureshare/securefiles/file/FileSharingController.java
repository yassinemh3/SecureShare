package com.secureshare.securefiles.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/share")
@RequiredArgsConstructor
public class FileSharingController {

    private final FileSharingService sharingService;
    private final FileStorageService fileStorageService;

    @PostMapping("/{fileId}")
    public ResponseEntity<String> shareFile(
            @PathVariable Long fileId,
            @RequestParam(required = false) String password,
            @RequestParam(defaultValue = "60") long expiryMinutes
    ) {
        String token = sharingService.generateShareLink(fileId, password, expiryMinutes);
        return ResponseEntity.ok("http://localhost:8080/api/v1/share/access/" + token);
    }

    @GetMapping("/access/{token}")
    public ResponseEntity<?> accessFile(
            @PathVariable String token,
            @RequestParam(required = false) String password
    ) throws Exception {
        return sharingService.getValidSharedFile(token, password)
                .map(shared -> {
                    try {
                        FileEntity file = shared.getFile();
                        byte[] content = fileStorageService.getFileContent(file);
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(file.getContentType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                                .body(new ByteArrayResource(content));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body("File read error");
                    }
                })
                .orElse(ResponseEntity.status(403).body("Invalid or expired token"));
    }
}
