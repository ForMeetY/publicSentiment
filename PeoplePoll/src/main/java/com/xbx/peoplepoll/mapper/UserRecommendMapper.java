package com.xbx.peoplepoll.mapper;


import com.xbx.peoplepoll.pojo.UserRecommendation;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
@Mapper
public interface UserRecommendMapper {
    // 按id查询
    List<UserRecommendation> getById(String id);
}
