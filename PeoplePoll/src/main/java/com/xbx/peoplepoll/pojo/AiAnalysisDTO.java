package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author X
 * @date 2026/6/3 9:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAnalysisDTO  {
    private String userTag;          // 用户核心人设
    private String behaviorAnalysis; // 行为分析
    private String operationSuggestion; // 运营建议
}
