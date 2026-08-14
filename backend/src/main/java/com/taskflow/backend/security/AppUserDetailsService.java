package com.taskflow.backend.security;

import com.taskflow.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(AuthenticatedUser::new)
                // Deliberately vague: the same message is used for a bad password,
                // so the endpoint does not reveal which usernames exist.
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
    }
}
