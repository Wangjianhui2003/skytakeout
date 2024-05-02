package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    public void deleteBatch(List<Long> ids) {
        for(Long dishId : ids){
            Dish dish = dishMapper.getById(dishId);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        List<Long> setMealRelated = setmealDishMapper.queryByDishId(ids);
        if(setMealRelated != null && setMealRelated.size() != 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        for(Long dishId : ids){
            dishMapper.deleteById(ids);
            dishFlavorMapper.deleteByDishId(ids);
//        }



    }

    @Transactional
    public void addDishandFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();

        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);

        Long id = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();

        if(flavors != null && flavors.size() != 0){
            flavors.forEach(item -> item.setDishId(id));
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page);
    }
}
