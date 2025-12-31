package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.dto.PaginatedResponse;
import com.starter.springboot.dto.UserResponseDTO;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.service.IPaginationService;
import com.starter.springboot.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserResource Tests")
class UserResourceTest {

    @Mock
    private IUserService userService;

    @Mock
    private IPaginationService paginationService;

    @InjectMocks
    private UserResource userResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(userResource)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
        
        // Setup sample User entity
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("test.user");
        sampleUser.setFirstName("Test");
        sampleUser.setLastName("User");
        sampleUser.setEmail("test.user@example.com");
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should return paginated users")
    void getUsers_ShouldReturnPaginatedResponse() throws Exception {
        // Arrange
        List<User> users = Collections.singletonList(sampleUser);
        Page<User> page = new PageImpl<>(users);
        
        when(paginationService.createPageableWithSort(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Pageable.unpaged());
        when(userService.findAllUsers(any(Pageable.class))).thenReturn(page);
        
        when(paginationService.getCurrentPage(any())).thenReturn(0);
        when(paginationService.getPageSize(any())).thenReturn(20);
        when(paginationService.getTotalElements(any())).thenReturn(1L);
        when(paginationService.getTotalPages(any())).thenReturn(1);
        when(paginationService.isFirst(any())).thenReturn(true);
        when(paginationService.isLast(any())).thenReturn(true);
        when(paginationService.hasNext(any())).thenReturn(false);
        when(paginationService.hasPrevious(any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/users")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "id")
                .param("sortDirection", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].username", is("test.user")))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(paginationService).createPageableWithSort(0, 20, "id", "asc");
        verify(userService).findAllUsers(any(Pageable.class));
    }
}
