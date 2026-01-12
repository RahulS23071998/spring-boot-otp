package com.starter.springboot.repository;

import com.starter.springboot.entity.Role;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    String ROLES_CACHE = "roles";

    @Cacheable(cacheNames = ROLES_CACHE)
    Optional<Role> findByName(String name);

    @Override
    @Cacheable(cacheNames = ROLES_CACHE)
    Optional<Role> findById(Long id);
}