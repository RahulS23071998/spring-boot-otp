package com.starter.springboot.security;


import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.domain.User;
import com.starter.springboot.exceptions.UserNotActivatedException;
import com.starter.springboot.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    private final UserRepository userRepository;

    public UserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String login)
    {
        log.debug("Authenticating {}", login);
        String lowercaseLogin = login.toLowerCase();
        Optional<User> userFromDatabase = userRepository.findByUsername(lowercaseLogin);
        return userFromDatabase.map(user -> {
            if (Objects.isNull(user.getEnabled()) || !user.getEnabled()) {
                throw new UserNotActivatedException("User " + lowercaseLogin + ApplicationConstants.USER_NOT_ACTIVATED_MESSAGE);
            }
            List<GrantedAuthority> grantedAuthorities = List.of(new SimpleGrantedAuthority(user.getRole().getName()));

            return new org.springframework.security.core.userdetails.User(lowercaseLogin,
                    user.getPassword(),
                    grantedAuthorities);
        }).orElseThrow(() -> new UsernameNotFoundException("User " + lowercaseLogin + ApplicationConstants.USER_NOT_FOUND_DATABASE_MESSAGE));
    }
}
