package com.ecommerece.project.controller;
import com.ecommerece.project.config.AppConstants;
import com.ecommerece.project.payload.AuthenticationResult;
import com.ecommerece.project.security.request.LoginRequest;
import com.ecommerece.project.security.request.SignUpRequest;
import com.ecommerece.project.security.response.MessageResponse;
import com.ecommerece.project.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        AuthenticationResult authenticationResult=authService.loginUser(loginRequest);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, authenticationResult.getJwtCookie().toString()).
                body(authenticationResult.getUserInfoResponse());
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser( @Valid @RequestBody SignUpRequest signUpRequest){
         return authService.registerUser(signUpRequest);
    }

    @GetMapping("/username")
    public String currentUser(Authentication authentication){
        if(authentication!=null){
            return authentication.getName();
        }
        else{
            return "NULL";
        }
    }
    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication){

        return ResponseEntity.ok().body(authService.getCurrentUserDetails(authentication));
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signOutUser(){
        ResponseCookie cookie=authService.logoutUser();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).
                body(new MessageResponse("You have been signed out successfully"));
    }

    @GetMapping("/sellers")
    public ResponseEntity<?> getSellers(
            @RequestParam(name="pageNumber",defaultValue = AppConstants.PAGE_NUMBER,required = false)  Integer pageNumber
    ){
        Sort sortByAndOrder=Sort.by(AppConstants.SORT_USERS_BY).descending();
        Pageable pageDetails= PageRequest.of(pageNumber,Integer.parseInt(AppConstants.PAGE_SIZE),sortByAndOrder);
        return ResponseEntity.ok(authService.getAllSellers(pageDetails));
    }
}
