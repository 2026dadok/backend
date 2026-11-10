package dongsim.dadok.library.shared.controller;

import dongsim.dadok.library.shared.domain.dto.*;
import dongsim.dadok.library.shared.service.SharedReadingService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shared-readings")
@Validated
public class SharedReadingController {

    private final SharedReadingService sharedReadingService;

    public SharedReadingController(SharedReadingService sharedReadingService) {
        this.sharedReadingService = sharedReadingService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateSharedReadingRequest request
    ) {
        Long sharedReadingId = sharedReadingService.create(requireUser(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sharedReadingId", sharedReadingId));
    }

    @GetMapping
    public ResponseEntity<List<SharedReadingResponse>> getMine(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(sharedReadingService.getMine(requireUser(userId)));
    }

    @GetMapping("/{sharedReadingId}")
    public ResponseEntity<SharedReadingResponse> getDetail(
            @AuthenticationPrincipal String userId,
            @PathVariable Long sharedReadingId
    ) {
        return ResponseEntity.ok(sharedReadingService.getDetail(requireUser(userId), sharedReadingId));
    }

    @PutMapping("/{sharedReadingId}/progress")
    public ResponseEntity<SharedReadingResponse> updateProgress(
            @AuthenticationPrincipal String userId,
            @PathVariable Long sharedReadingId,
            @Valid @RequestBody UpdateProgressRequest request
    ) {
        return ResponseEntity.ok(sharedReadingService.updateProgress(requireUser(userId), sharedReadingId, request));
    }

    @PostMapping("/{sharedReadingId}/reflections")
    public ResponseEntity<Map<String, Long>> createReflection(
            @AuthenticationPrincipal String userId,
            @PathVariable Long sharedReadingId,
            @Valid @RequestBody CreateReflectionRequest request
    ) {
        Long reflectionId = sharedReadingService.createReflection(requireUser(userId), sharedReadingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("reflectionId", reflectionId));
    }

    @GetMapping("/{sharedReadingId}/reflections")
    public ResponseEntity<List<ReflectionResponse>> getReflections(
            @AuthenticationPrincipal String userId,
            @PathVariable Long sharedReadingId
    ) {
        return ResponseEntity.ok(sharedReadingService.getReflections(requireUser(userId), sharedReadingId));
    }

    private String requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }
}
