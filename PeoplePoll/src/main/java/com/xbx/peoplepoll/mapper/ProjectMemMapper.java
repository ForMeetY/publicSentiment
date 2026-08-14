package com.xbx.peoplepoll.mapper;


import com.xbx.peoplepoll.pojo.ProjectMem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectMemMapper {


    List<ProjectMem> getAll();
}
