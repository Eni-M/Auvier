package com.auvier.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE

    @Column(nullable = false)
    private String entityType; // Product, Category, User, Order, Variant

    private Long entityId;

    private String entityName;

    // Detailed description of the change (e.g., "Created category 'Shirts' as a subcategory of 'Clothing'")
    @Column(length = 500)
    private String description;

    // For updates: what fields changed (e.g., "name: 'Old Name' → 'New Name', price: '$10' → '$15'")
    @Column(length = 2000)
    private String changesDetail;

    // Previous values as JSON for auditing (optional, for rollback capability)
    @Column(columnDefinition = "TEXT")
    private String previousValues;

    // New values as JSON for auditing
    @Column(columnDefinition = "TEXT")
    private String newValues;

    @Column(nullable = false)
    private String adminUsername;

    // Admin's display name or full name
    private String adminDisplayName;

    @Column(length = 1000)
    private String details; // Legacy field, still used for simple notes

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String ipAddress;

    // User agent for additional security tracking
    @Column(length = 500)
    private String userAgent;

    // Session ID for tracking related actions
    private String sessionId;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
