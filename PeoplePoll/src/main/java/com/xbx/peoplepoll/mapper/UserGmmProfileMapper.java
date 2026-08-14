package com.xbx.peoplepoll.mapper;

import com.xbx.peoplepoll.pojo.UserGmmProfile;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/*
* 用户画像表mapper类
* */
@Mapper
public interface UserGmmProfileMapper {

    // 查询全部
    List<UserGmmProfile> getAllUserInfo();

    // 按user_id 查询
    UserGmmProfile getUserInfoById(String userId);
}
