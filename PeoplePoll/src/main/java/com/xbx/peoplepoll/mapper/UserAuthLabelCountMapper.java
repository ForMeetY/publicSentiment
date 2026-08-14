package com.xbx.peoplepoll.mapper;


import com.xbx.peoplepoll.pojo.UserAuthLabelCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface UserAuthLabelCountMapper {

    @Select("select * from user_auth_label_count")
    List<UserAuthLabelCount> selectAll();
}
