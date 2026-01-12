package com.starter.springboot.repository;

import com.starter.springboot.entity.Authority;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Long>, JpaSpecificationExecutor<Authority> {

    String AUTHORITIES_CACHE = "authorities";

    @Cacheable(cacheNames = AUTHORITIES_CACHE)
    Optional<Authority> findByName(String name);

    @Override
    @Cacheable(cacheNames = AUTHORITIES_CACHE)
    Optional<Authority> findById(Long id);
}