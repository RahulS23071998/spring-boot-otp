package com.starter.springboot.repository;

import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
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
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Role testRole;
    private Authority testAuthority;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testRole = createRole(1L, "ADMIN", "Administrator Role");
        entityManager.persist(testRole);

        testAuthority = createAuthority(1L, "CREATE_USER", "Create User Authority");
        entityManager.persist(testAuthority);

        testUser = createUser(1L, "testuser", "password123", "John", "Smith", "john@example.com", true, UserStatus.ACTIVE, testRole, testAuthority);
        entityManager.persist(testUser);

        entityManager.flush();
    }

    private Role createRole(Long id, String name, String description) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDescription(description);
        return role;
    }

    private Authority createAuthority(Long id, String name, String description) {
        Authority authority = new Authority();
        authority.setId(id);
        authority.setName(name);
        authority.setDescription(description);
        return authority;
    }

    private User createUser(Long id, String username, String password, String firstName, String lastName, String email, Boolean enabled, UserStatus status, Role role, Authority authority) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEnabled(enabled);
        user.setStatus(status);
        user.setRole(role);
        user.setAuthority(authority);
        user.setIsOtpRequired(false);
        return user;
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        Optional<User> found = userRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("john@example.com", found.get().getEmail());
        assertEquals("John", found.get().getFirstName());
    }

    @Test
    @DisplayName("Should return empty when user not found by username")
    void shouldReturnEmptyWhenUserNotFoundByUsername() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should create and retrieve user by id")
    void shouldCreateAndRetrieveUserById() {
        Optional<User> found = userRepository.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("John", found.get().getFirstName());
        assertEquals("Smith", found.get().getLastName());
    }

    @Test
    @DisplayName("Should save user with all properties")
    void shouldSaveUserWithAllProperties() {
        User newUser = createUser(2L, "newuser", "password456", "Jane", "Smith", "jane@example.com", true, UserStatus.ACTIVE, testRole, testAuthority);
        User savedUser = userRepository.save(newUser);

        assertNotNull(savedUser.getId());
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("jane@example.com", savedUser.getEmail());
    }

    @Test
    @DisplayName("Should update user password")
    void shouldUpdateUserPassword() {
        Optional<User> userOptional = userRepository.findByUsername("testuser");
        assertTrue(userOptional.isPresent());

        User user = userOptional.get();
        user.setPassword("newpassword123");
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        Optional<User> updatedUser = userRepository.findByUsername("testuser");
        assertTrue(updatedUser.isPresent());
        assertEquals("newpassword123", updatedUser.get().getPassword());
    }

    @Test
    @DisplayName("Should set OTP required for user")
    void shouldSetOtpRequiredForUser() {
        Optional<User> userOptional = userRepository.findByUsername("testuser");
        assertTrue(userOptional.isPresent());

        User user = userOptional.get();
        user.setIsOtpRequired(true);
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        Optional<User> updatedUser = userRepository.findByUsername("testuser");
        assertTrue(updatedUser.isPresent());
        assertTrue(updatedUser.get().getIsOtpRequired());
    }

    @Test
    @DisplayName("Should delete user by id")
    void shouldDeleteUserById() {
        Long userId = testUser.getId();
        userRepository.deleteById(userId);

        entityManager.flush();
        entityManager.clear();

        Optional<User> deletedUser = userRepository.findById(userId);
        assertFalse(deletedUser.isPresent());
    }

    @Test
    @DisplayName("Should count total users")
    void shouldCountTotalUsers() {
        User anotherUser = createUser(2L, "anotheruser", "password789", "Bobby", "Johnson", "bob@example.com", true, UserStatus.ACTIVE, testRole, testAuthority);
        userRepository.save(anotherUser);

        entityManager.flush();

        long count = userRepository.count();
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should find user by Google ID")
    void shouldFindUserByGoogleId() {
        testUser.setGoogleId("google-id-12345");
        userRepository.save(testUser);
        entityManager.flush();

        Optional<User> found = userRepository.findByGoogleId("google-id-12345");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("john@example.com", found.get().getEmail());
        assertEquals("google-id-12345", found.get().getGoogleId());
    }

    @Test
    @DisplayName("Should return empty when user not found by Google ID")
    void shouldReturnEmptyWhenUserNotFoundByGoogleId() {
        Optional<User> found = userRepository.findByGoogleId("non-existent-google-id");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find correct user when multiple users have different Google IDs")
    void shouldFindCorrectUserWithMultipleGoogleIds() {
        testUser.setGoogleId("google-id-111");
        userRepository.save(testUser);

        User secondUser = createUser(2L, "anotheruser", "password456", "Jane", "Smith", "jane@example.com", true, UserStatus.ACTIVE, testRole, testAuthority);
        secondUser.setGoogleId("google-id-222");
        userRepository.save(secondUser);

        entityManager.flush();

        Optional<User> found = userRepository.findByGoogleId("google-id-222");

        assertTrue(found.isPresent());
        assertEquals("anotheruser", found.get().getUsername());
        assertEquals("jane@example.com", found.get().getEmail());
    }
}
