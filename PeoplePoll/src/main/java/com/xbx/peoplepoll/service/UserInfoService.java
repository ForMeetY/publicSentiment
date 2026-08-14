package com.xbx.peoplepoll.service;

import com.xbx.peoplepoll.pojo.AiAnalysisDTO;
import com.xbx.peoplepoll.pojo.PageResult;
import com.xbx.peoplepoll.pojo.UserGmmProfile;
import java.util.List;
/*
* 用户画像服务
* */
public interface UserInfoService {

    // 请求全部
    PageResult<UserGmmProfile> getAllUserInfo(Integer page, Integer pageSize);


    UserGmmProfile getUserInfoById(String userID);

    AiAnalysisDTO getAiAnalysisResult(String userID);
}
