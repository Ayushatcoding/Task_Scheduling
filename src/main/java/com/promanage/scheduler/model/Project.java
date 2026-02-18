package com.promanage.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity // Tells Hibernate: "This class maps to a DB table"
@Table(name = "projects")
@Data // Lombok: Generates Getters, Setters, toString automatically
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private Integer deadline; // 1=Mon, 5=Fri

    private BigDecimal revenue;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.PENDING;
}

// Create this Enum in the same package or separately
