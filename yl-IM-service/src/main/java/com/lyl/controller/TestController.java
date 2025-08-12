package com.lyl.controller;

import com.lyl.domain.dto.TestDTO;
import com.lyl.domain.vo.TestVO;
import com.lyl.service.test.ITestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("test")
public class TestController {
    @Resource
    private ITestService testService;

    @RequestMapping("hello")
    public String hello() {
        return "Hello, World!";
    }

    @PostMapping("add")
    public boolean add(@RequestBody @Validated TestDTO testDTO) {
        testService.add(testDTO);
        return true;
    }

    @GetMapping("get/{id}")
    public TestVO get(@PathVariable Long id) {
        log.info("Fetching test with id: {}", id);
        TestVO testVO = testService.get(id);
        log.info("Fetched test: {}", testVO);
        return testVO;
    }
}
