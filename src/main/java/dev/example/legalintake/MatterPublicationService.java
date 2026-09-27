package dev.example.legalintake;

import java.time.Clock;
import java.time.LocalDate;

public final class MatterPublicationService {
    private final MediaScreen media;
    private final Clock clock;

    public MatterPublicationService(MediaScreen media, Clock clock) { this.media = media; this.clock = clock; }

    public PublicationReceipt screen(MatterIntake intake, byte[] image, String filename, String mediaType) {
        ModerationDecision moderation = media.moderate(intake.caption(), image, mediaType);
        if (moderation.flagged()) {
            return new PublicationReceipt(intake.matterId(), PublicationState.QUARANTINED, null,
                    followUp(intake), "Counsel review required before publication");
        }
        String uploadedImage = media.upload(image, filename, intake.matterId());
        String deliveryImage = media.resize(uploadedImage, intake.matterId());
        return new PublicationReceipt(intake.matterId(), PublicationState.READY_FOR_SIGNED_DELIVERY,
                deliveryImage, followUp(intake), "Screened image prepared for signed document delivery");
    }

    private FollowUp followUp(MatterIntake intake) {
        boolean dueSoon = !intake.followUpDeadline().isAfter(LocalDate.now(clock).plusDays(3));
        return new FollowUp(intake.followUpDeadline(), dueSoon ? "CONTACT_CLIENT" : "WAITING");
    }

    public interface MediaScreen {
        ModerationDecision moderate(String caption, byte[] image, String mediaType);
        String upload(byte[] image, String filename, String requestId);
        String resize(String image, String requestId);
    }
    public record MatterIntake(String matterId, String caption, LocalDate followUpDeadline) {}
    public record ModerationDecision(boolean flagged) {}
    public record FollowUp(LocalDate deadline, String action) {}
    public record PublicationReceipt(String matterId, PublicationState state, String deliveryImage,
                                     FollowUp followUp, String nextStep) {}
    public enum PublicationState { QUARANTINED, READY_FOR_SIGNED_DELIVERY }
}
