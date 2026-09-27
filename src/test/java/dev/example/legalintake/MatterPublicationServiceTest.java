package dev.example.legalintake;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MatterPublicationServiceTest {
    @Test void flaggedMatterIsQuarantinedAndNeverDelivered() {
        FakeMedia media = new FakeMedia(true);
        var service = new MatterPublicationService(media, Clock.fixed(Instant.parse("2026-09-25T08:00:00Z"), ZoneOffset.UTC));
        var receipt = service.screen(new MatterPublicationService.MatterIntake("matter-104", "Signed retainer", LocalDate.parse("2026-09-27")),
                new byte[] {1, 2, 3}, "retainer.png", "image/png");
        assertEquals(MatterPublicationService.PublicationState.QUARANTINED, receipt.state());
        assertEquals("CONTACT_CLIENT", receipt.followUp().action());
        assertFalse(media.uploaded); assertNull(receipt.deliveryImage());
    }
    private static final class FakeMedia implements MatterPublicationService.MediaScreen {
        private final boolean flagged; boolean uploaded;
        FakeMedia(boolean flagged) { this.flagged = flagged; }
        public MatterPublicationService.ModerationDecision moderate(String c, byte[] i, String t) { return new MatterPublicationService.ModerationDecision(flagged); }
        public String upload(byte[] i, String f, String r) { uploaded = true; return "image-1"; }
        public String resize(String i, String r) { return "delivery-1"; }
    }
}
