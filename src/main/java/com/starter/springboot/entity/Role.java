package com.starter.springboot.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.listener.CustomAuditingListener;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = DatabaseConstants.ROLE_TABLE)
@DynamicInsert
@DynamicUpdate
@EntityListeners({CustomAuditingListener.class, IdGeneratorEntityListener.class})
public class Role extends BaseAuditedEntity implements BaseEntityWithId {

    @Id
    private Long id;

    @NotNull
    @Size(max = DatabaseConstants.ROLE_NAME_MAX_LENGTH)
    @Column(name = DatabaseConstants.ROLE_NAME_COLUMN, nullable = false, length = DatabaseConstants.ROLE_NAME_MAX_LENGTH)
    private String name;

    @Size(max = DatabaseConstants.DESCRIPTION_MAX_LENGTH)
    @Column(name = DatabaseConstants.ROLE_DESCRIPTION_COLUMN, length = DatabaseConstants.DESCRIPTION_MAX_LENGTH)
    private String description;

    @JsonIgnore
    @OneToMany(mappedBy = DatabaseConstants.ROLE_MAPPING_FIELD)
    private Set<User> users = new HashSet<>();

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

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
