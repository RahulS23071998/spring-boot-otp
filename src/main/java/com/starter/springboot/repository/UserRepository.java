package com.starter.springboot.repository;

import com.starter.springboot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {"role", "authority"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"role", "authority"})
    Optional<User> findByGoogleId(String googleId);

    @Override
    @EntityGraph(attributePaths = {"role", "authority"})
    Page<User> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"role", "authority"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "500"))
    @Query("select u from User u")
    @EntityGraph(attributePaths = {"role", "authority"})
    Stream<User> streamAll();
}
