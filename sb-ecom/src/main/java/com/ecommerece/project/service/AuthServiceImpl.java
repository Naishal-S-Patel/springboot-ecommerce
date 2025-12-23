package com.ecommerece.project.service;

import com.ecommerece.project.model.AppRole;
import com.ecommerece.project.model.Role;
import com.ecommerece.project.model.User;
import com.ecommerece.project.payload.AuthenticationResult;
import com.ecommerece.project.payload.UserDTO;
import com.ecommerece.project.payload.UserResponse;
import com.ecommerece.project.repositories.RoleRepository;
import com.ecommerece.project.repositories.UserRepository;
import com.ecommerece.project.security.jwt.JwtUtils;
import com.ecommerece.project.security.request.LoginRequest;
import com.ecommerece.project.security.request.SignUpRequest;
import com.ecommerece.project.security.response.MessageResponse;
import com.ecommerece.project.security.response.UserInfoResponse;
import com.ecommerece.project.security.services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public AuthenticationResult loginUser(LoginRequest loginRequest) {
        Authentication authentication;

            authentication=authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie=jwtUtils.generateJwtCookie(userDetails);
        List<String> roles=userDetails.getAuthorities().stream().
                map(item->item.getAuthority()).toList();
        UserInfoResponse userInfoResponse =new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(),jwtCookie.toString(),roles,userDetails.getEmail());
        return new AuthenticationResult(userInfoResponse,jwtCookie);
    }

    @Override
    public ResponseEntity<MessageResponse> registerUser(SignUpRequest signUpRequest) {
        if(userRepository.existsByUserName(signUpRequest.getUsername())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken"));
        }
        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already taken"));
        }
        User user =new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword())
        );
        Set<String> strRoles=signUpRequest.getRole();
        Set<Role> roles=new HashSet<>();

        if(strRoles==null){
            Role userRole=roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(()-> new RuntimeException("Error: role is not found"));
            roles.add(userRole);
        }
        else{
            // admin-- ROLE_ADMIN
//            seller --> ROLE_SELLER
            strRoles.forEach(role->{
                switch (role){
                    case "admin":
                        Role adminRole=roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(()-> new RuntimeException("Error: role is not found"));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role sellerRole=roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(()-> new RuntimeException("Error: role is not found"));
                        roles.add(sellerRole);
                        break;
                    default:
                        Role userRole=roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(()-> new RuntimeException("Error: role is not found"));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles=userDetails.getAuthorities().stream().
                map(item->item.getAuthority()).toList();
        UserInfoResponse userInfoResponse =new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(),roles);
        return userInfoResponse;
    }

    @Override
    public ResponseCookie logoutUser() {
        return jwtUtils.getCleanJwtCookie();
    }

    @Override
    public UserResponse getAllSellers(Pageable pageDetails) {
        Page<User> allUsers=userRepository.findByRoleName(AppRole.ROLE_SELLER,pageDetails);
        List<UserDTO> userDTOS=allUsers.getContent().stream()
                .map(p->modelMapper.map(p,UserDTO.class)).toList();
        UserResponse userResponse=new UserResponse();
        userResponse.setContent(userDTOS);
        userResponse.setPageNumber(allUsers.getNumber());
        userResponse.setPageSize(allUsers.getSize());
        userResponse.setTotalElements(allUsers.getTotalElements());
        userResponse.setTotalPages(allUsers.getTotalPages());
        userResponse.setLastPage(allUsers.isLast());
        return userResponse;
    }
}
