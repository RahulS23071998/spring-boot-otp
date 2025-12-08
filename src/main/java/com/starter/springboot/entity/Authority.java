package com.starter.springboot.entity;


import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.listener.CustomAuditingListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;


@Entity
@Table(name = DatabaseConstants.AUTHORITY_TABLE)
@EntityListeners({CustomAuditingListener.class, IdGeneratorEntityListener.class})
public class Authority extends BaseAuditedEntity implements BaseEntityWithId {

    @Id
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
