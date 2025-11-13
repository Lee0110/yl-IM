package com.lyl.service.test;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lyl.service.test.dto.TestDTO;
import com.lyl.service.test.po.Test;
import com.lyl.service.test.vo.TestVO;


public interface ITestService extends IService<Test> {

    void add(TestDTO testDTO);

    TestVO get(Long id);
}
