package com.starter.springboot.repository;

import com.starter.springboot.entity.Role;
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
@DisplayName("RoleRepository Tests")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();

        adminRole = createRole(1L, "ADMIN", "Administrator role");
        userRole = createRole(2L, "USER", "Regular user role");

        entityManager.persist(adminRole);
        entityManager.persist(userRole);
        entityManager.flush();
    }

    private Role createRole(Long id, String name, String description) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDescription(description);
        return role;
    }

    @Test
    @DisplayName("Should find role by name")
    void shouldFindRoleByName() {
        Optional<Role> found = roleRepository.findByName("ADMIN");

        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().getName());
        assertEquals("Administrator role", found.get().getDescription());
    }

    @Test
    @DisplayName("Should return empty when role not found by name")
    void shouldReturnEmptyWhenRoleNotFoundByName() {
        Optional<Role> found = roleRepository.findByName("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find role by id")
    void shouldFindRoleById() {
        Optional<Role> found = roleRepository.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("ADMIN", found.get().getName());
    }

    @Test
    @DisplayName("Should create and save role")
    void shouldCreateAndSaveRole() {
        Role newRole = createRole(3L, "MODERATOR", "Moderator role");
        Role savedRole = roleRepository.save(newRole);

        assertNotNull(savedRole.getId());
        assertEquals("MODERATOR", savedRole.getName());
        assertEquals("Moderator role", savedRole.getDescription());
    }

    @Test
    @DisplayName("Should update role description")
    void shouldUpdateRoleDescription() {
        Optional<Role> roleOptional = roleRepository.findByName("ADMIN");
        assertTrue(roleOptional.isPresent());

        Role role = roleOptional.get();
        role.setDescription("Updated administrator role");
        roleRepository.save(role);

        entityManager.flush();
        entityManager.clear();

        Optional<Role> updatedRole = roleRepository.findByName("ADMIN");
        assertTrue(updatedRole.isPresent());
        assertEquals("Updated administrator role", updatedRole.get().getDescription());
    }

    @Test
    @DisplayName("Should delete role by id")
    void shouldDeleteRoleById() {
        Long roleId = userRole.getId();
        roleRepository.deleteById(roleId);

        entityManager.flush();
        entityManager.clear();

        Optional<Role> deletedRole = roleRepository.findById(roleId);
        assertFalse(deletedRole.isPresent());
    }

    @Test
    @DisplayName("Should count total roles")
    void shouldCountTotalRoles() {
        long count = roleRepository.count();
        assertEquals(2, count);

        Role newRole = createRole(3L, "GUEST", "Guest role");
        roleRepository.save(newRole);
        entityManager.flush();

        count = roleRepository.count();
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should retrieve all roles")
    void shouldRetrieveAllRoles() {
        var allRoles = roleRepository.findAll();

        assertEquals(2, allRoles.size());
        assertTrue(allRoles.stream().anyMatch(r -> "ADMIN".equals(r.getName())));
        assertTrue(allRoles.stream().anyMatch(r -> "USER".equals(r.getName())));
    }
}
