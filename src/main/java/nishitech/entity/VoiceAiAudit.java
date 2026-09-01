package nishitech.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_ai_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceAiAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelId;
    private String agentName;
    private String customerPhone;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String sentiment;       // POSITIVE, NEUTRAL, NEGATIVE
    private Integer qualityScore;   // 1 to 100

    @Column(columnDefinition = "TEXT")
    private String objectionsDetected;

    private LocalDateTime auditedAt;

    @PrePersist
    public void init() {
        this.auditedAt = LocalDateTime.now();
    }
}