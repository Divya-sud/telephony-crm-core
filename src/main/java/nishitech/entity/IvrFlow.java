package nishitech.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ivr_flows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IvrFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String flowName;
    private String dnisNumber; // Phone number mapped to this flow

    @Column(columnDefinition = "TEXT")
    private String flowJson;   // JSON array of nodes: [{step: 1, type: "PLAY", media: "welcome"}, {step: 2, type: "MENU", options: {"1": "SALES", "2": "SUPPORT"}}]

    private boolean active;
}