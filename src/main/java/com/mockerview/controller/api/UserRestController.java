// package com.mockerview.controller.api;

// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.mockerview.dto.UserSearchResponse;
// import com.mockerview.service.UserService;

// import lombok.RequiredArgsConstructor;

// import java.util.List;

// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestParam;


// @RestController
// @RequestMapping("/api/users")
// @RequiredArgsConstructor
// public class UserRestController {
    
//     private final UserService userService;

//     @GetMapping("/search")
//     public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam("q") String keyword) {

//         List<UserSearchResponse> result = userService.searchUsers(keyword);

//         return ResponseEntity.ok(result);
//     }
    
// }
