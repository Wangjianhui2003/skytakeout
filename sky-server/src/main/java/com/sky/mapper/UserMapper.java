package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    User getByOpenId(String openId);

    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long id);
}
