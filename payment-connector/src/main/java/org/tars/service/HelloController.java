package org.tars.service;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tars.payment.dubbo.api.DemoService;

@RestController
@RequestMapping("/api/v1/demo")
public class HelloController {

    @DubboReference
    private DemoService demoService;

    @GetMapping("/hello")
    public String sayHello() {
        return demoService.sayHello("Hello World!");
    }
}
