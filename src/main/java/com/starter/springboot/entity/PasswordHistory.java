package com.starter.springboot.entity;

import com.starter.springboot.constants.DatabaseConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "password_history")
@EntityListeners(AuditingEntityListener.class)
public class PasswordHistory extends BaseAuditedEntity implements BaseEntityWithId {

    @Id
    @Column(name = "password_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DatabaseConstants.USER_ID_COLUMN, nullable = false)
    private User user;

    @Column(name = DatabaseConstants.PASSWORD_COLUMN, length = DatabaseConstants.PASSWORD_MAX_LENGTH)
    @NotNull
    @Size(min = 4, max = DatabaseConstants.PASSWORD_MAX_LENGTH)
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
