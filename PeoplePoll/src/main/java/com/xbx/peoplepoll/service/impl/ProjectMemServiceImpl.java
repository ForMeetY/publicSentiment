package com.xbx.peoplepoll.service.impl;

import com.xbx.peoplepoll.mapper.ProjectMemMapper;
import com.xbx.peoplepoll.pojo.ProjectMem;
import com.xbx.peoplepoll.service.ProjectMemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author X
 * @date 2026/4/23 22:27
 */

@Service
public class ProjectMemServiceImpl implements ProjectMemService {
    @Autowired
    private ProjectMemMapper projectMemMapper;
    @Override
    public List<ProjectMem> getAll() {
        List<ProjectMem> projectMems = projectMemMapper.getAll();
        return projectMems;
    }
}
