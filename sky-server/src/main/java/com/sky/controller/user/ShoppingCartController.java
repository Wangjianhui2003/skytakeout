package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "购物车相关接口")
@Slf4j
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {
    @Autowired
    ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result addItemToCart(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加物品到购物车,{}",shoppingCartDTO);
        shoppingCartService.addItemToCart(shoppingCartDTO);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("显示购物车物品")
    public Result<List<ShoppingCart>> showCartItems(){
        log.info("查询当前用户购物车物品,{}", BaseContext.getCurrentId());
        List<ShoppingCart> items = shoppingCartService.listCartItems();
        return Result.success(items);
    }

    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result cleanCart(){
        log.info("清空购物车");
        shoppingCartService.cleanCart();
        return Result.success();
    }

}

