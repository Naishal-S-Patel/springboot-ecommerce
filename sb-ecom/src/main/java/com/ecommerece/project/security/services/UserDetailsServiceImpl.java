package com.ecommerece.project.security.services;

import com.ecommerece.project.model.User;
import com.ecommerece.project.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// when a user attempt to login spring security will call this class and look for their roles in db
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=userRepository.findByUserName(username).
                orElseThrow(()-> new UsernameNotFoundException("user not found with username :"+username));
        return UserDetailsImpl.build(user);
    }
}
