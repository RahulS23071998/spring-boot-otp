package com.starter.springboot.repository;

import com.starter.springboot.entity.PasswordHistory;
import com.starter.springboot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findByUserOrderByCreatedDateDesc(User user);
}
