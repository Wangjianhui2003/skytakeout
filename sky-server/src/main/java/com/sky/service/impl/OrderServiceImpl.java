package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    //websocekt服务端
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO orderSubmitDTO) {
        //异常情况-地址簿空
        AddressBook addressBook = addressBookMapper.getById(orderSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //TODO:为什么用shoppingcart查
        //购物车空
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if(list == null || list.isEmpty()){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造order并插入
        Orders order = new Orders();
        BeanUtils.copyProperties(orderSubmitDTO,order);

        order.setUserId(userId);
        order.setAddress(addressBook.getDetail());
        order.setPhone(addressBook.getPhone());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);

        //主键回显
        orderMapper.insert(order);

        //批量插入order_detail
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        for(ShoppingCart shoppingCart1 : list){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart1,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetails.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetails);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //返回vo
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderAmount(order.getAmount())
                .orderNumber(order.getNumber())
                .orderTime(order.getOrderTime())
                .build();

        return  orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单号查询当前用户的订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通知客户端 来单 type = 1
        HashMap<String,Object> hashMap = new HashMap<String,Object>();
        hashMap.put("type",1);
        hashMap.put("orderId",orders.getId()); //订单id
        hashMap.put("content","订单号:" + outTradeNo); //订单号

        String jsonString = JSON.toJSONString(hashMap);

        webSocketServer.sendToAllClient(jsonString);
    }

    @Override
    public PageResult pageQuery(int page, int pageSize, Integer status) {
        PageHelper.startPage(page,pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);

        ArrayList<OrderVO> orderVOList = new ArrayList<>();

        //查询detail,将order和detail放入orderVO列表里
        if(pages != null && pages.getTotal() > 0){
            for(Orders orders : pages.getResult()){
                Long id = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getlistById(id);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetails);
                orderVOList.add(orderVO);
            }
        }

        PageResult pageResult = new PageResult(pages.getTotal(),orderVOList);
        return pageResult;
    }

    @Override
    public OrderVO queryOrderDetail(Long id) {
        //获得Order和Order_Detail
        Orders order = orderMapper.getById(id);
        List<OrderDetail> orderDetails = orderDetailMapper.getlistById(id);

        //组装OrderVO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order,orderVO);
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    @Override
    public void cancelOrder(Long id) {
        Orders order = orderMapper.getById(id);

        //检查订单是否存在
        if(order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //接单后不能取消
        if(order.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //用于更新的order
        Orders order1 = new Orders();

        //已接单的要退款
        if(order.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            order1.setPayStatus(Orders.REFUND);
        }

        //更新
        order1.setId(id);
        order1.setStatus(Orders.CANCELLED);
        order1.setCancelTime(LocalDateTime.now());
        order1.setCancelReason("用户取消");

        orderMapper.update(order1);
    }

    @Override
    public void repeatOrder(Long id) {
        Long userId = BaseContext.getCurrentId();

        List<OrderDetail> orderDetails = orderDetailMapper.getlistById(id);

        List<ShoppingCart> cartItemList = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //忽略id属性
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setUserId(userId);

            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(cartItemList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> pages = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> voList = getVOList(pages.getResult());

        return new PageResult(pages.getTotal(),voList);
    }

    @Override
    public OrderStatisticsVO statistic() {

        Integer toBeConfirm = orderMapper.count(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.count(Orders.CONFIRMED);
        Integer delivery = orderMapper.count(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirm);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(delivery);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();

        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);

        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //获取order
        Long orderId = ordersRejectionDTO.getId();
        Orders order = orderMapper.getById(orderId);

        if(order != null && order.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //已付退款
        if(order.getPayStatus().equals(Orders.PAID)){
            //跳过 weChatPayUtil.refund调用，模拟给用户退款，直接更新数据库订单支付状态为 ”已退款 “
            log.info("已付退款,{}",orderId);
        }

        //更新order
        Orders orders = new Orders();

        orders.setPayStatus(Orders.REFUND);//修改订单支付状态为 ”已退款 “
        orders.setId(orderId);
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //跳过 weChatPayUtil.refund调用，模拟给用户退款，直接更新数据库订单支付状态为 ”已退款 “
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
            log.info("给订单{}退款", ordersDB.getNumber());
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setPayStatus(Orders.REFUND);//修改订单支付状态为 ”已退款 “

        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void reminder(Long id) {
        //查询订单
        Orders order = orderMapper.getById(id);

        //检查是否存在订单
        if(order == null) throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);

        //组装json字符串来发送
        HashMap<String, Object> map = new HashMap<>();
        map.put("type",2); //type=2催单
        map.put("orderId",id);
        map.put("content","订单号:" + order.getNumber());

        String jsonString = JSON.toJSONString(map);

        webSocketServer.sendToAllClient(jsonString);
    }

    public List<OrderVO> getVOList(List<Orders> ordersList){

        List<OrderVO> collect = ordersList.stream().map(order -> {
            //Detia
            List<OrderDetail> orderDetails = orderDetailMapper.getlistById(order.getId());

            //拼接detail里的菜品名和数量方便显示
            String dishStr = getDishStr(orderDetails);

            //组装VO
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order, orderVO);
            orderVO.setOrderDetailList(orderDetails);
            orderVO.setOrderDishes(dishStr);

            return orderVO;
        }).collect(Collectors.toList());

        return collect;
    }

    //拼接detail里的菜品名和数量方便显示
    public String getDishStr(List<OrderDetail> orderDetailList){
        List<String> StrList = orderDetailList.stream().map(detail -> {
            return detail.getName() + "*" + detail.getNumber() + ";";
        }).collect(Collectors.toList());

        String join = String.join("", StrList);

        return join;
    }


}
