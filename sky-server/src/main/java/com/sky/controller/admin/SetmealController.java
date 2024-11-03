package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping()
    @ApiOperation("新增套餐")
    public Result addSetmeal(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐,{}",setmealDTO);
        setmealService.addSetmeal(setmealDTO);
        return Result.success();

    }

    @PostMapping("page")
    @ApiOperation("分页查询")
    public Result<PageResult> pageQuery(@RequestBody SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询,{}",setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping()
    @ApiOperation("删除套餐")
    public Result delete(@RequestBody List<Long> ids){
        log.info("删除套餐,{}",ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    @PutMapping()
    @ApiOperation("修改套餐")
    public Result update(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐,{}",setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    @PostMapping("status/{status}")
    @ApiOperation("起售停售套餐")
    public Result changeStatus(Long id,@PathVariable Integer status){
        log.info("起售停售套餐,{},{}",id,status);
        setmealService.changeStatus(id,status);
        return Result.success();
    }

    @GetMapping("{id}")
    @ApiOperation("根据id获得套餐(用来回显)")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("id获取套餐信息",id);
        SetmealVO setmealVO = setmealService.getSetmealById(id);
        return Result.success(setmealVO);
    }

}