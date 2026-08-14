package com.xbx.peoplepoll.mapper;


import com.xbx.peoplepoll.pojo.InteractionCountByLabel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper
public interface InteractionCountByLabelMapper {

    @Select("select * from interaction_count_by_label")
    List<InteractionCountByLabel> selectAll();
}
