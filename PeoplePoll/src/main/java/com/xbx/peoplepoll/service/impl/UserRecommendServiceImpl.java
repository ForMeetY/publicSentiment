package com.xbx.peoplepoll.service.impl;

import com.xbx.peoplepoll.mapper.UserRecommendMapper;
import com.xbx.peoplepoll.pojo.UserRecommendation;
import com.xbx.peoplepoll.service.UserRecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author X
 * @date 2026/6/5 10:51
 */

@Service
public class UserRecommendServiceImpl implements UserRecommendService {
    @Autowired
    private UserRecommendMapper userRecommendMapper;
    @Override
    public List<UserRecommendation> getUserRecommention(String userID) {
        List<UserRecommendation> list = userRecommendMapper.getById(userID);
        return list;
    }
}
