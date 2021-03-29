package study.querydsl.controller;

import lombok.Getter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HellocController {

    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
}
