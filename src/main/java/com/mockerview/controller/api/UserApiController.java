package com.mockerview.controller.api;

import java.security.Principal;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockerview.dto.CustomUserDetails;
import com.mockerview.dto.WithdrawRequest;
import com.mockerview.dto.WithdrawResponse;
import com.mockerview.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserApiController {

    private UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawResponse> withdrawUser(
        @RequestBody WithdrawRequest withdrawRequest, 
        @AuthenticationPrincipal CustomUserDetails userDetails ){

            log.info("탈퇴 요청 Controller 진입: {}", userDetails.getUsername());

            String username = userDetails.getUsername();
            String password = withdrawRequest.getPassword();
            String reason   = withdrawRequest.getReason();

           // try {
                userService.withdraw(username, password, reason);    

                log.info("탈퇴 처리 Service 성공: {}", username);
                
                WithdrawResponse withdrawResponse = new WithdrawResponse("success", "/auth/logout");
                // 200 ok
                return ResponseEntity.ok(withdrawResponse);

    }

}
