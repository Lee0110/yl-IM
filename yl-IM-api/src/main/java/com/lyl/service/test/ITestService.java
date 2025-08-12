package com.lyl.service.test;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lyl.domain.dto.TestDTO;
import com.lyl.domain.po.Test;
import com.lyl.domain.vo.TestVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ${author}
 * @since 2025-08-12
 */
public interface ITestService extends IService<Test> {

    void add(TestDTO testDTO);

    TestVO get(Long id);
}
