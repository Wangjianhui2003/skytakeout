package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;

@Service
public class UserServiceImp implements UserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    UserMapper userMapper;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    WeChatProperties weChatProperties;
    /**
     * 通过openid查询user表,返回User对象
     * @param userLoginDTO
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String openid = getOpenId(userLoginDTO.getCode());
        User user = userMapper.getUser(openid);

        if(user == null){
            user = User.builder()
                .openid(openid)
                .createTime(LocalDateTime.now())
                .build();
            //此处主键回显
            userMapper.insert(user);
        }

        return user;
    }

    public String getOpenId(String code){
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");

        String s = HttpClientUtil.doGet(WX_LOGIN, map);
        JSONObject json = JSON.parseObject(s);
        String openid = json.getString("openid");
        return openid;
    }
}
