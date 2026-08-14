import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import silhouette_score
from sklearn.mixture import GaussianMixture
from sklearn.preprocessing import StandardScaler
from sqlalchemy import create_engine

# 1. 建立 MySQL 连接
engine = create_engine(
    "mysql+pymysql://root:1234@master1:3306/weibo?charset=utf8mb4"
)
"""
 提取用于训练的特征
"""
# 2. 读取三张基础画像表
df_time = pd.read_sql(
    "SELECT user_id, screen_name, hourly_sentiment_stddev, night_post_ratio FROM user_time_sentiment_profile",
    engine,
)
df_region = pd.read_sql(
    "SELECT user_id, region_diversity_score, top_region_avg_score FROM user_region_sentiment_profile",
    engine,
)
df_auth = pd.read_sql(
    "SELECT user_id, user_authentication, sentiment_leverage, avg_model_confidence FROM user_auth_sentiment_profile",
    engine,
)

# 拼接特征成一张大宽表
df_time["user_id"] = df_time["user_id"].astype(str)
df_region["user_id"] = df_region["user_id"].astype(str)
df_auth["user_id"] = df_auth["user_id"].astype(str)

df_user = pd.merge(df_time, df_region, on="user_id", how="left")
df_user = pd.merge(df_user, df_auth, on="user_id", how="left")


# 定义 5 大核心特征
feature_cols = [
    "hourly_sentiment_stddev",  # 情绪波动：24小时内情感状态的标准差，反映网民情绪的起伏剧烈度与敏感期
    "night_post_ratio",         # 深夜/凌晨段发帖量占总发帖比例，用于捕捉“深夜活跃型”特定舆情群体
    "region_diversity_score",   # 发言 IP 属地的多样性得分，衡量账号的地域流动性或矩阵异常特征
    "top_region_avg_score",    # 地域 在最长驻、最核心地域的平均情感得分，是用户情感的基本底色
    "sentiment_leverage",       # 发帖引发他人互动(转发/评论)的情绪杠杆率/煽动能效，是识别大V的关键
]

df_user[feature_cols] = df_user[feature_cols].fillna(0.0)



# 增加官媒拦截
# 认证字段匹配
is_official_auth = df_user["user_authentication"].str.contains("官方媒体(蓝V)", na=False, regex=False)

# 提取出官媒
df_official_users = df_user[is_official_auth].copy()
# 剩余普通网民
df_normal_users = df_user[~is_official_auth].copy()

print(f"{len(df_official_users)} 个机构官媒账号，不参与GMM拟合。")


# 4. 剔除纯全0的
is_active = (
    (df_normal_users["hourly_sentiment_stddev"] > 0.001)
    | (df_normal_users["night_post_ratio"] > 0)
    | (df_normal_users["region_diversity_score"] > 0)
    | (df_normal_users["sentiment_leverage"] > 0)
)

df_active_users = df_normal_users[is_active].copy()
X_clean = df_active_users[feature_cols].copy()

print(
    f"总用户数: {len(df_user)} 除官媒后普通网民: {len(df_normal_users)} 最终参与GMM建模的活跃活人: {len(X_clean)}"
)

# 平滑对数变换 使得数据分布更平滑
X_clean["sentiment_leverage"] = np.log1p(X_clean["sentiment_leverage"])

# 标准化特征空间
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X_clean)

# 构建并训练高斯混合模型（GMM）
gmm = GaussianMixture(
    n_components=4,  # 分为4类
    covariance_type="diag", # 采用对角协方差矩阵，平衡模型复杂度和计算效率
    reg_covar=1e-1, # 避免零方差
    n_init=3,  # 随机初始化 三次
    random_state=42,
    max_iter=200, # 最大迭代次数
)
gmm.fit(X_scaled)

# 预测
X_clean["cluster_id"] = gmm.predict(X_scaled)
means = X_clean.groupby("cluster_id")[feature_cols].mean() # 每个群的中心

pd.set_option("display.max_columns", None)
pd.set_option("display.width", 1000)
print("\n纯净普通活人网民的类别特征均值中心: ")
print(means)

# 模型评估 轮廓系数 圈内的内聚度 圈外的离度
if len(X_scaled) > 10000:
    # 随机采样 10000 人
    sample_idx = np.random.choice(len(X_scaled), 10000, replace=False)
    # 计算轮廓系数
    score = silhouette_score(
        X_scaled[sample_idx], X_clean["cluster_id"].iloc[sample_idx]
    )
else:
    # 直接计算轮廓系数
    score = silhouette_score(X_scaled, X_clean["cluster_id"])
#
print(f"活跃普通群体内部轮廓系数 (Silhouette Score): {score:.4f}")

# 9. 精准保存模型组件
joblib.dump(gmm, "gmm_cluster_model.pkl")
joblib.dump(scaler, "scaler.pkl")
joblib.dump(feature_cols, "feature_cols.pkl")
print("训练晚餐")