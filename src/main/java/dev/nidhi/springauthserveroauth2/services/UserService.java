package dev.nidhi.springauthserveroauth2.services;

import dev.nidhi.springauthserveroauth2.dtos.SignUpRequest;
import dev.nidhi.springauthserveroauth2.entities.UserEntity;
import dev.nidhi.springauthserveroauth2.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void signup(SignUpRequest signUpRequest){
        if(userRepository.existsByEmail(signUpRequest.email())){
            throw new IllegalArgumentException("Email already exists");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(signUpRequest.email());
        userEntity.setPassword(passwordEncoder.encode(signUpRequest.password()));
        userEntity.setEnabled(true);
        userEntity.setRole("USER");
        userRepository.save(userEntity);
    }
}
