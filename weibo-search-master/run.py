from scrapy.cmdline import execute
import sys
import os
import logging
# 1. 把项目根目录加入 Python 路径（
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
# 2. 配置日志
logging.basicConfig(
    level=logging.INFO,  # 只输出 INFO 及以上级别日志
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler()  # 日志输出到控制台
    ]
)
# 把日志同时保存到文件，可以加 FileHandler：
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("spider.log", encoding="utf-8"),
        logging.StreamHandler()
    ]
)
# 3. 启动爬虫（这里保持你的爬虫名 search 不变）
if __name__ == "__main__":
    execute(["scrapy", "crawl", "search"])


from scrapy.cmdline import execute
import sys
import os
import logging

# 1. 把项目根目录加入 Python 路径
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

#日志配置
logging.basicConfig(
    level=logging.ERROR,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("spider.log", encoding="utf-8"),  # 输出到文件
        logging.StreamHandler()  # 输出到控制台
    ]
)

# 3. 启动爬虫
if __name__ == "__main__":
    execute(["scrapy", "crawl", "search"])