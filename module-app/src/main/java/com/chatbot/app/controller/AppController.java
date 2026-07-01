package com.chatbot.app.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chatbot.app.dto.AppCreateRequest;
import com.chatbot.app.dto.AppProjectPayload;
import com.chatbot.app.dto.AppUpdateRequest;
import com.chatbot.app.entity.App;
import com.chatbot.app.service.AppCoverService;
import com.chatbot.app.service.AppDeployService;
import com.chatbot.app.service.AppPackageService;
import com.chatbot.app.service.AppService;
import com.chatbot.app.service.VisualEditService;
import com.chatbot.app.dto.VisualEditRequest;
import com.chatbot.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;
    private final AppCoverService appCoverService;
    private final AppDeployService appDeployService;
    private final AppPackageService appPackageService;
    private final VisualEditService visualEditService;

    @PostMapping
    public ApiResponse<App> create(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody AppCreateRequest request) {
        return ApiResponse.ok(appService.create(token, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<App> update(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody AppUpdateRequest request) {
        return ApiResponse.ok(appService.update(token, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        appService.delete(token, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<App> getById(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        return ApiResponse.ok(appService.getById(token, id));
    }

    @GetMapping("/mine")
    public ApiResponse<Page<App>> listMine(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(appService.listMine(token, page, size));
    }

    @GetMapping("/featured")
    public ApiResponse<List<App>> listFeatured(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(appService.listFeatured(limit));
    }

    @GetMapping("/admin/all")
    public ApiResponse<Page<App>> listAllAdmin(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(appService.listAllAdmin(token, page, size));
    }

    @PostMapping("/{id}/featured")
    public ApiResponse<App> setFeatured(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        return ApiResponse.ok(appService.setFeatured(token, id, Boolean.TRUE.equals(body.get("featured"))));
    }

    @PostMapping("/{id}/project")
    public ApiResponse<App> saveProject(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody AppProjectPayload payload) {
        return ApiResponse.ok(appService.saveProject(token, id, payload));
    }

    @PostMapping("/{id}/cover")
    public ApiResponse<App> generateCover(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        return ApiResponse.ok(appCoverService.generateCover(token, id));
    }

    @GetMapping("/{id}/cover/image")
    public Mono<ResponseEntity<Resource>> coverImage(
            @PathVariable Long id,
            @RequestParam(required = false) String type) {
        return Mono.fromCallable(() -> {
            Path file = appCoverService.getCoverFile(id);
            Resource resource = new FileSystemResource(file);
            MediaType mediaType = "png".equalsIgnoreCase(type)
                    || file.getFileName().toString().endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : MediaType.parseMediaType("image/svg+xml");
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);
        });
    }

    @PostMapping("/{id}/visual-edit")
    public ApiResponse<App> visualEdit(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id,
            @RequestBody VisualEditRequest request) {
        return ApiResponse.ok(visualEditService.edit(token, id, request));
    }

    @PostMapping("/{id}/deploy")
    public ApiResponse<App> deploy(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        return ApiResponse.ok(appDeployService.deploy(token, id));
    }

    @GetMapping(value = {"/preview/{id}", "/preview/{id}/"})
    public Mono<ResponseEntity<Resource>> previewRoot(@PathVariable Long id) {
        return previewFile(id, "index.html");
    }

    @GetMapping("/preview/{id}/{*filepath}")
    public Mono<ResponseEntity<Resource>> previewPath(
            @PathVariable Long id,
            @PathVariable("filepath") String filepath) {
        String path = filepath == null || filepath.isBlank() ? "index.html" : filepath;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return previewFile(id, path);
    }

    @GetMapping("/{id}/download")
    public Mono<ResponseEntity<byte[]>> download(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Long id) {
        return Mono.fromCallable(() -> {
            App app = appService.getById(token, id);
            byte[] zip = appPackageService.downloadZip(token, id);
            String filename = AppPackageService.safeZipName(app.getName()) + ".zip";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zip);
        });
    }

    private Mono<ResponseEntity<Resource>> previewFile(Long id, String relativePath) {
        return Mono.fromCallable(() -> {
            Path file = appDeployService.resolvePreviewFile(id, relativePath);
            Resource resource = new FileSystemResource(file);
            MediaType mediaType = guessMediaType(file.getFileName().toString());
            return ResponseEntity.ok().contentType(mediaType).body(resource);
        });
    }

    private MediaType guessMediaType(String filename) {
        if (filename.endsWith(".html")) return MediaType.TEXT_HTML;
        if (filename.endsWith(".css")) return MediaType.parseMediaType("text/css");
        if (filename.endsWith(".js")) return MediaType.parseMediaType("application/javascript");
        if (filename.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
