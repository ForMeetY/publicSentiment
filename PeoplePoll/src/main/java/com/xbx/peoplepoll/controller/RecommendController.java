package com.xbx.peoplepoll.controller;

import com.xbx.peoplepoll.pojo.Result;
import com.xbx.peoplepoll.pojo.UserRecommendation;
import com.xbx.peoplepoll.service.UserRecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * @author X
 * @date 2026/6/5 9:59
 */
/*
* 推荐系统
*
* */

@RestController
public class RecommendController {

    // 根据user_id查询其被推荐的内容
    @Autowired
    private UserRecommendService recommendService;

    // 根据 user_id 查询被推荐的内容
    @GetMapping("/recommend/list")
    public Result getRecommendContent(@RequestParam String userId) {
        // 调用 Service 层逻辑，查询视图 v_recommendations
        List<UserRecommendation> list = recommendService.getUserRecommention(userId);

        // 使用统一格式封装并返回
        return Result.success(list);
    }
}
