package com.cism.backend.service.users;

import com.cism.backend.repository.admin.CreateStallRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cism.backend.repository.users.RegisterRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CreateStallRepository createStallRepository;
    private final RegisterRepository registerRepository;

    public UserDetailsServiceImpl(RegisterRepository registerRepository, CreateStallRepository createStallRepository) {
        this.registerRepository = registerRepository;
        this.createStallRepository = createStallRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
       var user = registerRepository.findByEmail(identifier);
       if (user.isPresent()) {
           return user.get();
       }

       var stall = createStallRepository.findByLicence(identifier);
       if (stall.isPresent()){
        return stall.get();
       }

       throw new UsernameNotFoundException("User not found with identifier: " + identifier);
    }
}
