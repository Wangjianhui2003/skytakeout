package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    public OrderTask(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("检查支付超时订单,{}",new Date());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        //查询15分钟前还未支付的订单
        List<Orders> orders = orderMapper.getByStatusAndTime(Orders.PENDING_PAYMENT,time);

        if(orders != null && !orders.isEmpty()){
            orders.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("支付超时");

                orderMapper.update(order);
            });
        }

    }

    /**
     * 已派送的订单处理
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveredOrder(){
        log.info("处理已派送的订单,{}",new Date());

        LocalDateTime time = LocalDateTime.now().plusHours(-1);
        List<Orders> orders = orderMapper.getByStatusAndTime(Orders.DELIVERY_IN_PROGRESS, time);

        if(orders != null && !orders.isEmpty()){
            orders.forEach(order ->{
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }
}
