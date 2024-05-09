package com.sky.service;

import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface DishService {
    DishVO queryById(Long id);

    void deleteBatch(List<Long> ids);

    public void addDishandFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void reviseDish(DishDTO dishDTO);
}
