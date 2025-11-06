package com.lyl.domain.vo;

import com.lyl.exception.IMException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> implements Serializable {
    /**
     * 明细数据
     */
    private List<T> data;

    /**
     * 总数
     */
    private long total;

    public static <T, R> PageVO<R> convert(PageVO<T> source, Function<? super T, ? extends R> mapper) {
        if (Objects.isNull(source) || CollectionUtils.isEmpty(source.getData())) {
            return new PageVO<>(Collections.emptyList(), 0);
        }
        if (Objects.isNull(mapper)) {
            throw new IMException("转换函数不能为空");
        }
        List<R> mapped = source.getData().stream().map(mapper).collect(Collectors.toList());
        return new PageVO<>(mapped, source.getTotal());
    }
}
