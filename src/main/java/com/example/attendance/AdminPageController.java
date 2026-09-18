package com.example.attendance;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminPageController {

    @GetMapping(
            value = "/admin-login.html",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<Resource> adminLoginPage() {

        Resource resource =
                new ClassPathResource("static/admin-login.html");

        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}

