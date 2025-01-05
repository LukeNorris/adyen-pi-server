package com.example.adyenpiserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    // Basic endpoint to test with Postman
    @GetMapping("/ping")
    public String ping() {
        return "Pong!";
    }

    // A sample POST endpoint to demonstrate receiving JSON
    @PostMapping("/test")
    public String testEndpoint(@RequestBody TestRequest request) {
        // Just echo back something
        return "Hello, " + request.name() + "! You sent value: " + request.value();
    }

    // Record class for request data
    // (Alternatively, use a regular POJO with getters/setters)
    public record TestRequest(String name, int value) {}
}
