package com.xbx.peoplepoll.service;


import com.xbx.peoplepoll.pojo.UserRecommendation;
import org.springframework.stereotype.Service;
import java.util.List;

public interface UserRecommendService {

    // 根据user_id查询其被推荐
    public List<UserRecommendation> getUserRecommention(String userID);
}
