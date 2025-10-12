package com.starter.springboot.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.starter.springboot.constants.DatabaseConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;


@Entity
@Table(name = DatabaseConstants.AUTHORITY_TABLE)
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = DatabaseConstants.AUTHORITY_NAME_COLUMN, length = DatabaseConstants.AUTHORITY_NAME_MAX_LENGTH, nullable = false, unique = true)
    @NotNull
    private String name;

    @Column(name = DatabaseConstants.AUTHORITY_DESCRIPTION_COLUMN)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
