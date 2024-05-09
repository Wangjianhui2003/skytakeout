package com.sky.controller.admin;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api("菜品管理接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @PostMapping
    @ApiOperation("添加菜品")
    public Result addDish(@RequestBody DishDTO dishDTO){
        log.info("添加菜品,{}",dishDTO);
        dishService.addDishandFlavor(dishDTO);
        return Result.success();
    }

    /**
     *
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("page")
    @ApiOperation("菜品分页查询")
    public Result pageQuery(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询,{}",dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        PageResult page = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(page);

    }

    @DeleteMapping
    @ApiOperation("批量删除")
    @Transactional
    public Result deleteBatch(@RequestParam List<Long> ids){
        log.info("批量删除,{}",ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    @GetMapping("{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> queryById(@PathVariable Long id){
        log.info("根据id查询菜品,{}",id);
        DishVO dishVO = dishService.queryById(id);
        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("修改菜品")
    public Result reviseDish(@RequestBody DishDTO dishDTO){
        log.info("修改菜品,{}",dishDTO);
        dishService.reviseDish(dishDTO);
        return Result.success();
    }

}
