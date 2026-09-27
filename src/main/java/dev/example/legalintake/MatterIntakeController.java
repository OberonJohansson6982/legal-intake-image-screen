package dev.example.legalintake;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
final class MatterIntakeController {
    private final MatterPublicationService service;
    MatterIntakeController(MatterPublicationService service) { this.service = service; }

    @PostMapping("/matters/screen")
    MatterPublicationService.PublicationReceipt screen(@RequestParam String matterId, @RequestParam String caption,
            @RequestParam LocalDate followUpDeadline, @RequestPart MultipartFile image) throws Exception {
        return service.screen(new MatterPublicationService.MatterIntake(matterId, caption, followUpDeadline),
                image.getBytes(), image.getOriginalFilename(), image.getContentType());
    }
    @ExceptionHandler(InfraiMediaGateway.InfraiRejection.class)
    ResponseEntity<Map<String, String>> rejected(InfraiMediaGateway.InfraiRejection error) {
        int status = error.status >= 400 && error.status < 500 ? error.status : 502;
        return ResponseEntity.status(status).body(Map.of("code", error.code, "message", error.getMessage()));
    }
}
