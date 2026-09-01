package nishitech.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelId;
    private String callerNumber;
    private String recordingFileName;
    private LocalDateTime initiatedAt;

    @PrePersist
    public void init() {
        this.initiatedAt = LocalDateTime.now();
    }
}