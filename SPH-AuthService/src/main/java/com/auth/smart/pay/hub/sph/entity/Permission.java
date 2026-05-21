package com.auth.smart.pay.hub.sph.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String code;
    
    @Column(length = 255)
    private String description;
}