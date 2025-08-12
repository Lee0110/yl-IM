package com.lyl.service.test;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lyl.domain.dto.TestDTO;
import com.lyl.domain.po.Test;
import com.lyl.domain.vo.TestVO;
import com.lyl.exception.OcsException;
import com.lyl.mapper.TestMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ${author}
 * @since 2025-08-12
 */
@Service
public class TestServiceImpl extends ServiceImpl<TestMapper, Test> implements ITestService {

    @Override
    public void add(TestDTO testDTO) {
        Test test = new Test();
        BeanUtils.copyProperties(testDTO, test);
        save(test);
    }

    @Override
    public TestVO get(Long id) {
        Test test = getById(id);
        if (Objects.isNull(test)) {
            throw new OcsException("不存在，id：" + id);
        }
        TestVO testVO = new TestVO();
        BeanUtils.copyProperties(test, testVO);
        return testVO;
    }
}
