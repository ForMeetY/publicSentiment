package com.xbx.peoplepoll.controller;

import com.github.pagehelper.PageHelper;
import com.xbx.peoplepoll.pojo.AiAnalysisDTO;
import com.xbx.peoplepoll.pojo.PageResult;
import com.xbx.peoplepoll.pojo.Result;
import com.xbx.peoplepoll.pojo.UserGmmProfile;
import com.xbx.peoplepoll.service.UserInfoService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author X
 * @date 2026/5/31 17:52
 */
/*
* 解决用户画像的接口请求
* */
@RestController
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;

    // 分页查询接口 前端传递页码和每页显示的条数
    @GetMapping("/userInfo")
    public Result getUserInfo(Integer page, Integer pageSize){
        // 做分页

        PageResult<UserGmmProfile> userGmmProfiles = userInfoService.getAllUserInfo(page, pageSize);

        return Result.success(userGmmProfiles);
    }

    // 按user_id 查询
    @GetMapping("/userInfo/{userID}")
    public Result getUserInfoById(@PathVariable String userID) {
        System.out.println("用户ID是"+userID);
        return Result.success(userInfoService.getUserInfoById(userID));
    }

    // 在 UserInfoController 中添加
    @GetMapping("/userInfo/ai-analysis/{userID}")
    public Result getAiAnalysis(@PathVariable String userID) {
        // 调用 Service 层，Service 负责封装 Prompt、调用 DeepSeek 并返回结果
        AiAnalysisDTO aiAnalysisDTO = userInfoService.getAiAnalysisResult(userID);
        return Result.success(aiAnalysisDTO);
    }



}
