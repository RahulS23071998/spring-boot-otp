package com.starter.springboot.rest.resources;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.rest.dto.UserResponseDTO;
import com.starter.springboot.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping(ApplicationConstants.API_BASE_PATH)
public class UserResource {

    private final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = ApplicationConstants.USERS_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserResponseDTO>> getUsers()
    {
        log.debug(ApplicationConstants.CLIENT_REST_REQUEST_MESSAGE);

        List<UserResponseDTO> userList = userService.findAllUsers().stream()
            .map(UserResponseDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(userList);
    }
}
