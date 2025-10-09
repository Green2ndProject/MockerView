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

            // } catch (IllegalArgumentException e) {
            //     // 비밀번호 불일치 등의 경우 401 or 403 반환
            //     log.warn("탈퇴 실패 - 비밀번호 불일치: {}", username);
            //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            // } catch(UsernameNotFoundException e){
            //     log.error("탈퇴 실패 - 사용자를 찾을 수 없음: {}", username);
            //     return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            // } catch (Exception e){
            //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            // }
    }

}
