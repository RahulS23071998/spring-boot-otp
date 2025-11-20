package com.starter.springboot.repository;

import com.starter.springboot.entity.Authority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false"
})
@DisplayName("AuthorityRepository Tests")
class AuthorityRepositoryTest {

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Authority createAuthority;
    private Authority deleteAuthority;
    private Authority readAuthority;

    @BeforeEach
    void setUp() {
        authorityRepository.deleteAll();

        createAuthority = createAuthorityEntity(1L, "CREATE_USER", "Permission to create users");
        deleteAuthority = createAuthorityEntity(2L, "DELETE_USER", "Permission to delete users");
        readAuthority = createAuthorityEntity(3L, "READ_USER", "Permission to read users");

        entityManager.persist(createAuthority);
        entityManager.persist(deleteAuthority);
        entityManager.persist(readAuthority);
        entityManager.flush();
    }

    private Authority createAuthorityEntity(Long id, String name, String description) {
        Authority authority = new Authority();
        authority.setId(id);
        authority.setName(name);
        authority.setDescription(description);
        return authority;
    }

    @Test
    @DisplayName("Should find authority by name")
    void shouldFindAuthorityByName() {
        Optional<Authority> found = authorityRepository.findByName("CREATE_USER");

        assertTrue(found.isPresent());
        assertEquals("CREATE_USER", found.get().getName());
        assertEquals("Permission to create users", found.get().getDescription());
    }

    @Test
    @DisplayName("Should return empty when authority not found by name")
    void shouldReturnEmptyWhenAuthorityNotFoundByName() {
        Optional<Authority> found = authorityRepository.findByName("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find authority by id")
    void shouldFindAuthorityById() {
        Optional<Authority> found = authorityRepository.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("CREATE_USER", found.get().getName());
    }

    @Test
    @DisplayName("Should create and save authority")
    void shouldCreateAndSaveAuthority() {
        Authority newAuthority = createAuthorityEntity(4L, "UPDATE_USER", "Permission to update users");
        Authority savedAuthority = authorityRepository.save(newAuthority);

        assertNotNull(savedAuthority.getId());
        assertEquals("UPDATE_USER", savedAuthority.getName());
        assertEquals("Permission to update users", savedAuthority.getDescription());
    }

    @Test
    @DisplayName("Should update authority description")
    void shouldUpdateAuthorityDescription() {
        Optional<Authority> authorityOptional = authorityRepository.findByName("CREATE_USER");
        assertTrue(authorityOptional.isPresent());

        Authority authority = authorityOptional.get();
        authority.setDescription("Updated permission to create new users");
        authorityRepository.save(authority);

        entityManager.flush();
        entityManager.clear();

        Optional<Authority> updatedAuthority = authorityRepository.findByName("CREATE_USER");
        assertTrue(updatedAuthority.isPresent());
        assertEquals("Updated permission to create new users", updatedAuthority.get().getDescription());
    }

    @Test
    @DisplayName("Should delete authority by id")
    void shouldDeleteAuthorityById() {
        Long authorityId = deleteAuthority.getId();
        authorityRepository.deleteById(authorityId);

        entityManager.flush();
        entityManager.clear();

        Optional<Authority> deletedAuthority = authorityRepository.findById(authorityId);
        assertFalse(deletedAuthority.isPresent());
    }

    @Test
    @DisplayName("Should count total authorities")
    void shouldCountTotalAuthorities() {
        long count = authorityRepository.count();
        assertEquals(3, count);

        Authority newAuthority = createAuthorityEntity(4L, "MANAGE_ROLES", "Permission to manage roles");
        authorityRepository.save(newAuthority);
        entityManager.flush();

        count = authorityRepository.count();
        assertEquals(4, count);
    }

    @Test
    @DisplayName("Should retrieve all authorities")
    void shouldRetrieveAllAuthorities() {
        var allAuthorities = authorityRepository.findAll();

        assertEquals(3, allAuthorities.size());
        assertTrue(allAuthorities.stream().anyMatch(a -> "CREATE_USER".equals(a.getName())));
        assertTrue(allAuthorities.stream().anyMatch(a -> "DELETE_USER".equals(a.getName())));
        assertTrue(allAuthorities.stream().anyMatch(a -> "READ_USER".equals(a.getName())));
    }
}
