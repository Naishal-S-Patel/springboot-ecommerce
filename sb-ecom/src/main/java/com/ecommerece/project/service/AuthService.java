package com.ecommerece.project.service;


import com.ecommerece.project.payload.AuthenticationResult;
import com.ecommerece.project.payload.UserResponse;
import com.ecommerece.project.security.request.LoginRequest;
import com.ecommerece.project.security.request.SignUpRequest;
import com.ecommerece.project.security.response.MessageResponse;
import com.ecommerece.project.security.response.UserInfoResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface AuthService {

    AuthenticationResult loginUser(LoginRequest loginRequest);

    ResponseEntity<MessageResponse> registerUser(@Valid SignUpRequest signUpRequest);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    ResponseCookie logoutUser();

    UserResponse getAllSellers(Pageable pageDetails);
}
