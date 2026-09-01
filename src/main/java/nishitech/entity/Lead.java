package nishitech.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String phoneNumber;
    private String email;
    private String source;          // META, GOOGLE, DIRECT
    private String vertical;        // REAL_ESTATE, HEALTHCARE, EDTECH, BFSI
    private String campaignName;
    private String city;
    private String assignedAgent;

    @Column(columnDefinition = "TEXT")
    private String rawLeadData;

    private Integer aiLeadScore;
    private String intentCategory;  // HOT, WARM, COLD, SPAM, DUPLICATE

    @Column(columnDefinition = "TEXT")
    private String aiAgentAdvice;

    private String status;          // NEW, IN_PROGRESS, CLOSED_WON, CLOSED_LOST
    private Double dealValue;

    private LocalDateTime createdAt;

    @PrePersist
    public void init() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "NEW";
    }
}