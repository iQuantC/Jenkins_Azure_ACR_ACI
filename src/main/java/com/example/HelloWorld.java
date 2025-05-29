package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class HelloWorld {
    
    public String getMessage() {
        return "Hello, welcome to iQuant YouTube Channel!";
    }
    
    @GetMapping("/")
    public String hello() {
        return getMessage();
    }
    
    public static void main(String[] args) {
        SpringApplication.run(HelloWorld.class, args);
    }
}