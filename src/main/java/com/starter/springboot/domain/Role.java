package com.starter.springboot.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.starter.springboot.constants.DatabaseConstants;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = DatabaseConstants.ROLE_TABLE)
@DynamicInsert
@DynamicUpdate
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = DatabaseConstants.ROLE_NAME_COLUMN, nullable = false)
    private String name;

    @Column(name = DatabaseConstants.ROLE_DESCRIPTION_COLUMN)
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
