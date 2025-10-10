package com.starter.springboot.config;

import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.repositories.AuthorityRepository;
import com.starter.springboot.repositories.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DefaultDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataInitializer.class);

    @Bean
    public ApplicationRunner defaultRoleInitializer(RoleRepository roleRepository,
                                                    AuthorityRepository authorityRepository) {
        return args -> initializeDefaults(roleRepository, authorityRepository);
    }

    @Transactional
    void initializeDefaults(RoleRepository roleRepository, AuthorityRepository authorityRepository) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    log.info("Creating default role ROLE_USER");
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    role.setDescription("Default application user role");
                    return roleRepository.save(role);
                });

        authorityRepository.findByName("USER").orElseGet(() -> {
            log.info("Creating default authority USER");
            Authority authority = new Authority();
            authority.setName("USER");
            authority.setDescription("Default authority for standard users");
            return authorityRepository.save(authority);
        });
    }
}