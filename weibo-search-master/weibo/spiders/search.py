# -*- coding: utf-8 -*-
import json
import re
import sys
from urllib.parse import unquote
import scrapy

import weibo.utils.util as util
from scrapy.exceptions import CloseSpider
from scrapy.utils.project import get_project_settings
from weibo.items import WeiboItem
from weibo.Constant import *

# 获取文本数据
import os
LOCATION_DATA = []

# 自动找到当前文件夹
current_dir = os.path.dirname(os.path.abspath(__file__))
loc_file = os.path.join(current_dir, "loc_list.txt")

# 只存 省 + 市，自动去重
LOCATION_DATA = []
province_city_set = set()  # 用来去重

if os.path.exists(loc_file):
    with open(loc_file, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                data = json.loads(line)
                loc_name = data.get("loc_name", [])
                # 只取 省 + 市
                if len(loc_name) >= 2:
                    province = loc_name[0]  # 北京市
                    city = loc_name[1]  # 市辖区
                    # 去重：同一个 省 市 只存一次
                    key = (province, city)
                    if key not in province_city_set:
                        province_city_set.add(key)
                        LOCATION_DATA.append(key)
            except:
                pass


# 爬虫
class SearchSpider(scrapy.Spider):
    name = 'search'   # 爬虫名
    allowed_domains = ['weibo.com']  # 域名 爬取的网站
    settings = get_project_settings()  # 获取项目配置
    keyword_list = settings.get('KEYWORD_LIST') # 关键词
    if not isinstance(keyword_list, list): # 如果不是list 进入文件
        if not os.path.isabs(keyword_list): # 如果不是绝对路径 转换
            keyword_list = os.getcwd() + os.sep + keyword_list # 获取绝对路径
        if not os.path.isfile(keyword_list): # 如果不是文件
            sys.exit('不存在%s文件' % keyword_list) # 退出程序
        keyword_list = util.get_keyword_list(keyword_list) # 获取关键词列表

    #
    for i, keyword in enumerate(keyword_list):
        if len(keyword) > 2 and keyword[0] == '#' and keyword[-1] == '#':
            keyword_list[i] = '%23' + keyword[1:-1] + '%23'

    # 拼接 url 准备搜索微博
    weibo_type = util.convert_weibo_type(settings.get('WEIBO_TYPE')) # 获取微博类型
    contain_type = util.convert_contain_type(settings.get('CONTAIN_TYPE')) # 获取包含类型
    regions = util.get_regions(settings.get('REGION')) # 获取地区没用
    base_url = 'https://s.weibo.com' # 搜索微博的url
    #  时间
    start_date = settings.get('START_DATE',datetime.now().strftime('%Y-%m-%d'))
    end_date = settings.get('END_DATE', datetime.now().strftime('%Y-%m-%d'))
    if util.str_to_time(start_date) > util.str_to_time(end_date):
        sys.exit('settings.py配置错误，START_DATE值应早于或等于END_DATE值，请重新配置settings.py')
    # 爬取页数限制 默认46 如果小于46 直接爬取 否则按天拆分
    further_threshold = settings.get('FURTHER_THRESHOLD', 46)
    # 最大爬取条数
    limit_result = settings.get('LIMIT_RESULT', 0)
    result_count = 0
    # 错误标记
    mongo_error = False
    pymongo_error = False
    mysql_error = False
    pymysql_error = False
    sqlite3_error = False

    def check_limit(self):  # 是否到了爬取数量限制
        if self.limit_result > 0 and self.result_count > self.limit_result:
            print(f'已达到爬取结果数量限制：{self.limit_result}条，停止爬取')
            raise CloseSpider('已达到爬取结果数量限制')
        return False

    def start_requests(self):  # 启动爬虫发起请求
        start_date = datetime.strptime(self.start_date, '%Y-%m-%d')
        print(f"DEBUG 共加载关键词数量: {len(self.keyword_list)}")
        print(f"DEBUG 关键词列表: {self.keyword_list}")
        # 必须加一天 微博是 左闭右开
        end_date = datetime.strptime(self.end_date, '%Y-%m-%d') + timedelta(days=1)
        start_str = start_date.strftime('%Y-%m-%d') + '-0' # 微博时间固定格式
        end_str = end_date.strftime('%Y-%m-%d') + '-0'
        print(f"DEBUG: start_date={self.start_date}, end_date={self.end_date}")
        print(f"DEBUG weibo_type='{self.weibo_type}', contain_type='{self.contain_type}'")
        # 循环关键词拼接进url
        for keyword in self.keyword_list:
            # 不区分地区
            if not self.settings.get('REGION') or '全部' in self.settings.get('REGION'):
                base_url = 'https://s.weibo.com/weibo?q=%s' % keyword
                url = base_url + self.weibo_type
                url += self.contain_type
                url += '&timescope=custom:{}:{}'.format(start_str, end_str)
                yield scrapy.Request(url=url, callback=self.parse, meta={'base_url': base_url, 'keyword': keyword})
            # 区分地区
            else:
                for region in self.regions.values():
                    base_url = ('https://s.weibo.com/weibo?q={}&region=custom:{}:1000').format(keyword, region['code'])
                    url = base_url + self.weibo_type
                    url += self.contain_type
                    url += '&timescope=custom:{}:{}'.format(start_str, end_str)
                    yield scrapy.Request(url=url, callback=self.parse,
                                         meta={'base_url': base_url, 'keyword': keyword, 'province': region})

    # def start_requests(self):  # 启动爬虫发起请求
    #     start_date = datetime.strptime(self.start_date, '%Y-%m-%d')
    #     end_date = datetime.strptime(self.end_date, '%Y-%m-%d') + timedelta(days=1)
    #
    #     start_str = start_date.strftime('%Y-%m-%d') + '-0'
    #     end_str = end_date.strftime('%Y-%m-%d') + '-0'
    #
    #     print(f"DEBUG: start_date={self.start_date}, end_date={self.end_date}")
    #     print(f"DEBUG weibo_type='{self.weibo_type}', contain_type='{self.contain_type}'")
    #
    #     #循环所有关键词
    #     for keyword in self.keyword_list:
    #
    #         print(f" 正在爬取关键词：{keyword}")
    #         base_url = 'https://s.weibo.com/weibo?q=%s' % keyword
    #         url = base_url + self.weibo_type
    #         url += self.contain_type
    #         url += '&timescope=custom:{}:{}'.format(start_str, end_str)
    #
    #         # 强制每一个关键词都生成请求！
    #         yield scrapy.Request(
    #             url=url,
    #             callback=self.parse,
    #             meta={'base_url': base_url, 'keyword': keyword}
    #         )


    # 检查需要的数据库
    def check_environment(self):
        if self.pymongo_error:
            print('系统中可能没有安装pymongo库，请先运行 pip install pymongo ，再运行程序')
            raise CloseSpider()
        if self.mongo_error:
            print('系统中可能没有安装或启动MongoDB数据库，请先根据系统环境安装或启动MongoDB，再运行程序')
            raise CloseSpider()
        if self.pymysql_error:
            print('系统中可能没有安装pymysql库，请先运行 pip install pymysql ，再运行程序')
            raise CloseSpider()
        if self.mysql_error:
            print('系统中可能没有安装或正确配置MySQL数据库，请先根据系统环境安装或配置MySQL，再运行程序')
            raise CloseSpider()
        if self.sqlite3_error:
            print('系统中可能没有安装或正确配置SQLite3数据库，请先根据系统环境安装或配置SQLite3，尝试 pip install sqlite，再运行程序')
            raise CloseSpider()

    def parse(self, response):
        base_url = response.meta.get('base_url')
        keyword = response.meta.get('keyword')
        province = response.meta.get('province')
        is_empty = response.xpath('//div[@class="card card-no-result s-pt20b40"]')
        page_count = len(response.xpath('//ul[@class="s-scroll"]/li'))
        print(f"DEBUG parse page_count={page_count}, url={response.url}")
        if is_empty:
            print('当前页面搜索结果为空')
        elif page_count < self.further_threshold:
            for weibo in self.parse_weibo(response):
                self.check_environment()
                if self.check_limit():
                    return
                yield weibo
            next_url = response.xpath('//a[@class="next"]/@href').extract_first()
            if next_url:
                if self.check_limit():
                    return
                next_url = self.base_url + next_url
                yield scrapy.Request(url=next_url, callback=self.parse_page, meta={'keyword': keyword})
        else:
            start_date = datetime.strptime(self.start_date, '%Y-%m-%d')
            end_date = datetime.strptime(self.end_date, '%Y-%m-%d')
            clean_base_url = 'https://s.weibo.com/weibo?q=%s' % keyword
            while start_date <= end_date:
                start_str = start_date.strftime('%Y-%m-%d') + '-0'
                start_date = start_date + timedelta(days=1)
                end_str = start_date.strftime('%Y-%m-%d') + '-0'
                print(f"DEBUG 生成按天请求: {start_str} ~ {end_str}")
                url = clean_base_url + self.weibo_type
                url += self.contain_type
                url += '&timescope=custom:{}:{}&page=1'.format(start_str, end_str)
                yield scrapy.Request(url=url, callback=self.parse_by_day,
                                     meta={'base_url': clean_base_url, 'keyword': keyword,
                                           'province': province, 'date': start_str[:-2]})

    def parse_by_day(self, response):
        base_url = response.meta.get('base_url')
        keyword = response.meta.get('keyword')
        province = response.meta.get('province')
        is_empty = response.xpath('//div[@class="card card-no-result s-pt20b40"]')
        date = response.meta.get('date')
        page_count = len(response.xpath('//ul[@class="s-scroll"]/li'))
        if is_empty:
            print('当前页面搜索结果为空')
        elif page_count < self.further_threshold:
            for weibo in self.parse_weibo(response):
                self.check_environment()
                if self.check_limit():
                    return
                yield weibo
            next_url = response.xpath('//a[@class="next"]/@href').extract_first()
            if next_url:
                if self.check_limit():
                    return
                next_url = self.base_url + next_url
                yield scrapy.Request(url=next_url, callback=self.parse_page, meta={'keyword': keyword})
        else:
            start_date_str = date + '-0'
            start_date = datetime.strptime(start_date_str, '%Y-%m-%d-%H')
            for i in range(1, 25):
                start_str = start_date.strftime('%Y-%m-%d-X%H').replace('X0', 'X').replace('X', '')
                start_date = start_date + timedelta(hours=1)
                end_str = start_date.strftime('%Y-%m-%d-X%H').replace('X0', 'X').replace('X', '')
                url = base_url + self.weibo_type
                url += self.contain_type
                url += '&timescope=custom:{}:{}&page=1'.format(start_str, end_str)
                yield scrapy.Request(url=url,
                                     callback=self.parse_by_hour_province if province else self.parse_by_hour,
                                     meta={'base_url': base_url, 'keyword': keyword, 'province': province,
                                           'start_time': start_str, 'end_time': end_str})

    def parse_by_hour(self, response):
        keyword = response.meta.get('keyword')
        is_empty = response.xpath('//div[@class="card card-no-result s-pt20b40"]')
        start_time = response.meta.get('start_time')
        end_time = response.meta.get('end_time')
        page_count = len(response.xpath('//ul[@class="s-scroll"]/li'))
        if is_empty:
            print('当前页面搜索结果为空')
        elif page_count < self.further_threshold:
            for weibo in self.parse_weibo(response):
                self.check_environment()
                yield weibo
            next_url = response.xpath('//a[@class="next"]/@href').extract_first()
            if next_url:
                next_url = self.base_url + next_url
                yield scrapy.Request(url=next_url, callback=self.parse_page, meta={'keyword': keyword})
        else:
            for region in self.regions.values():
                url = ('https://s.weibo.com/weibo?q={}&region=custom:{}:1000').format(keyword, region['code'])
                url += self.weibo_type
                url += self.contain_type
                url += '&timescope=custom:{}:{}&page=1'.format(start_time, end_time)
                yield scrapy.Request(url=url, callback=self.parse_by_hour_province,
                                     meta={'keyword': keyword, 'start_time': start_time, 'end_time': end_time,
                                           'province': region})

    def parse_by_hour_province(self, response):
        keyword = response.meta.get('keyword')
        is_empty = response.xpath('//div[@class="card card-no-result s-pt20b40"]')
        start_time = response.meta.get('start_time')
        end_time = response.meta.get('end_time')
        province = response.meta.get('province')
        page_count = len(response.xpath('//ul[@class="s-scroll"]/li'))
        if is_empty:
            print('当前页面搜索结果为空')
        elif page_count < self.further_threshold:
            for weibo in self.parse_weibo(response):
                self.check_environment()
                yield weibo
            next_url = response.xpath('//a[@class="next"]/@href').extract_first()
            if next_url:
                next_url = self.base_url + next_url
                yield scrapy.Request(url=next_url, callback=self.parse_page, meta={'keyword': keyword})
        else:
            for city in province['city'].values():
                url = ('https://s.weibo.com/weibo?q={}&region=custom:{}:{}').format(keyword, province['code'], city)
                url += self.weibo_type
                url += self.contain_type
                url += '&timescope=custom:{}:{}&page=1'.format(start_time, end_time)
                yield scrapy.Request(url=url, callback=self.parse_page,
                                     meta={'keyword': keyword, 'start_time': start_time, 'end_time': end_time,
                                           'province': province, 'city': city})

    def parse_page(self, response):
        keyword = response.meta.get('keyword')
        is_empty = response.xpath('//div[@class="card card-no-result s-pt20b40"]')
        if is_empty:
            print('当前页面搜索结果为空')
        else:
            for weibo in self.parse_weibo(response):
                self.check_environment()
                if self.check_limit():
                    return
                yield weibo
            next_url = response.xpath('//a[@class="next"]/@href').extract_first()
            if next_url:
                if self.check_limit():
                    return
                next_url = self.base_url + next_url
                yield scrapy.Request(url=next_url, callback=self.parse_page, meta={'keyword': keyword})

    # 支持省份简称、别名匹配
    def get_location(self, text):
        if not text:
            return ""
        # 省份简称映射
        province_alias = {
            "北京": "北京市", "上海": "上海市", "天津": "天津市", "重庆": "重庆市",
            "湖北": "湖北省", "湖南": "湖南省", "广东": "广东省", "广西": "广西壮族自治区",
            "河南": "河南省", "河北": "河北省", "山东": "山东省", "山西": "山西省",
            "江苏": "江苏省", "浙江": "浙江省", "安徽": "安徽省", "福建": "福建省",
            "江西": "江西省", "四川": "四川省", "贵州": "贵州省", "云南": "云南省",
            "陕西": "陕西省", "甘肃": "甘肃省", "青海": "青海省", "内蒙古": "内蒙古自治区",
            "新疆": "新疆维吾尔自治区", "西藏": "西藏自治区", "宁夏": "宁夏回族自治区",
            "香港": "香港特别行政区", "澳门": "澳门特别行政区",
            "哈市":"黑龙江省",
            "台湾": "台湾省",
            "台湾省": "台湾省",
        }

        found = set()

        for prov, city in LOCATION_DATA:
            matched = False

            # 先处理：重庆只允许省份匹配，禁止下级城市匹配
            skip_city_match = False
            if prov == "重庆市":
                # 重庆的下级市辖区/区县，只允许省份命中，不许用城市名匹配
                skip_city_match = True

            # 1. 匹配省份全称
            if prov in text:
                matched = True
            else:
                # 2. 匹配省份简称
                for short_name, full_name in province_alias.items():
                    if short_name in text and full_name == prov:
                        matched = True
                        break

            # 3. 非重庆才允许匹配城市，且跳过市辖区
            if not matched and not skip_city_match and city and city != "市辖区" and city in text:
                matched = True

            if matched:
                found.add(prov)

        return " | ".join(sorted(found))

    def get_at_users(self, selector):
        a_list = selector.xpath('.//a')
        at_users = ''
        at_list = []
        for a in a_list:
            if len(unquote(a.xpath('@href').extract_first())) > 14 and len(
                    a.xpath('string(.)').extract_first()) > 1:
                if unquote(a.xpath('@href').extract_first())[14:] == a.xpath(
                        'string(.)').extract_first()[1:]:
                    at_user = a.xpath('string(.)').extract_first()[1:]
                    if at_user not in at_list:
                        at_list.append(at_user)
        if at_list:
            at_users = ','.join(at_list)
        return at_users

    def get_topics(self, selector):
        a_list = selector.xpath('.//a')
        topics = ''
        topic_list = []
        for a in a_list:
            text = a.xpath('string(.)').extract_first()
            if len(text) > 2 and text[0] == '#' and text[-1] == '#':
                if text[1:-1] not in topic_list:
                    topic_list.append(text[1:-1])
        if topic_list:
            topics = ','.join(topic_list)
        return topics

    def get_vip(self, selector):
        vip_type = "非会员"
        vip_level = 0

        vip_container = selector.xpath('.//div[@class="user_vip_icon_container"]')
        if vip_container:
            svvip_img = vip_container.xpath('.//img[contains(@src, "svvip_")]')
            if svvip_img:
                vip_type = "超级会员"
                src = svvip_img.xpath('@src').extract_first()
                level_match = re.search(r'svvip_(\d+)\.png', src)
                if level_match:
                    vip_level = int(level_match.group(1))
            else:
                vip_img = vip_container.xpath('.//img[contains(@src, "vip_")]')
                if vip_img:
                    vip_type = "会员"
                    src = vip_img.xpath('@src').extract_first()
                    level_match = re.search(r'vip_(\d+)\.png', src)
                    if level_match:
                        vip_level = int(level_match.group(1))

        return vip_type, vip_level

    def parse_weibo(self, response):
        keyword = response.meta.get('keyword')
        for sel in response.xpath("//div[@class='card-wrap']"):
            if self.check_limit():
                return
            info = sel.xpath("div[@class='card']/div[@class='card-feed']/div[@class='content']/div[@class='info']")
            if info:
                weibo = WeiboItem()
                weibo['id'] = sel.xpath('@mid').extract_first()
                bid = sel.xpath('.//div[@class="from"]/a[1]/@href').extract_first().split('/')[-1].split('?')[0]
                weibo['bid'] = bid
                weibo['user_id'] = info[0].xpath('div[2]/a/@href').extract_first().split('?')[0].split('/')[-1]
                weibo['screen_name'] = info[0].xpath('div[2]/a/@nick-name').extract_first()
                weibo['vip_type'], weibo['vip_level'] = self.get_vip(info[0])
                txt_sel = sel.xpath('.//p[@class="txt"]')[0]
                retweet_sel = sel.xpath('.//div[@class="card-comment"]')
                retweet_txt_sel = ''
                if retweet_sel and retweet_sel[0].xpath('.//p[@class="txt"]'):
                    retweet_txt_sel = retweet_sel[0].xpath('.//p[@class="txt"]')[0]
                content_full = sel.xpath('.//p[@node-type="feed_list_content_full"]')

                is_long_weibo = False
                is_long_retweet = False
                if content_full:
                    if not retweet_sel:
                        txt_sel = content_full[0]
                        is_long_weibo = True
                    elif len(content_full) == 2:
                        txt_sel = content_full[0]
                        retweet_txt_sel = content_full[1]
                        is_long_weibo = True
                        is_long_retweet = True
                    elif retweet_sel[0].xpath('.//p[@node-type="feed_list_content_full"]'):
                        retweet_txt_sel = retweet_sel[0].xpath('.//p[@node-type="feed_list_content_full"]')[0]
                        is_long_retweet = True
                    else:
                        txt_sel = content_full[0]
                        is_long_weibo = True
                weibo['text'] = txt_sel.xpath('string(.)').extract_first().replace('\u200b', '').replace('\ue627', '')
                weibo['location'] = self.get_location(weibo['text'])
                weibo['text'] = weibo['text'][2:].replace(' ', '')
                if is_long_weibo:
                    weibo['text'] = weibo['text'][:-4]
                weibo['at_users'] = self.get_at_users(txt_sel)
                weibo['topics'] = self.get_topics(txt_sel)
                reposts_count = sel.xpath('.//a[@action-type="feed_list_forward"]/text()').extract()
                reposts_count = "".join(reposts_count)
                try:
                    reposts_count = re.findall(r'\d+.*', reposts_count)
                except TypeError:
                    print("无法解析转发按钮，可能是 1) 网页布局有改动 2) cookie无效或已过期。\n")
                    raise CloseSpider()
                weibo['reposts_count'] = reposts_count[0] if reposts_count else '0'
                # 转换为整数
                weibo['reposts_count'] = int(weibo['reposts_count'])
                comments_count = sel.xpath('.//a[@action-type="feed_list_comment"]/text()').extract_first()
                comments_count = re.findall(r'\d+.*', comments_count)
                weibo['comments_count'] = comments_count[0] if comments_count else '0'
                # 转换为整数
                weibo['comments_count'] = int(weibo['comments_count'])
                attitudes_count = sel.xpath('.//a[@action-type="feed_list_like"]/button/span[2]/text()').extract_first()
                attitudes_count = re.findall(r'\d+.*', attitudes_count)
                weibo['attitudes_count'] = attitudes_count[0] if attitudes_count else '0'
                # 转换为整数
                weibo['attitudes_count'] = int(weibo['attitudes_count'])
                created_at = sel.xpath('.//div[@class="from"]/a[1]/text()').extract_first().replace(' ', '').replace('\n','').split('前')[0]
                weibo['created_at'] = util.standardize_date(created_at)
                if len(weibo['text']) < 5:
                    print("微博内容过短（<5），已过滤：", weibo['text'])
                    continue

                try:
                    time_str = weibo['created_at']
                    weibo_date = time_str.split(" ")[0]  # 取日期部分，如"2026-05-08"
                    if not (self.start_date <= weibo_date <= self.end_date):
                        print(f"时间不在范围内，丢弃: {time_str}")
                        continue
                except Exception as e:
                    print(f"时间解析失败，丢弃: {e}, raw_time={weibo['created_at']}")
                    continue

                source = sel.xpath('.//div[@class="from"]/a[2]/text()').extract_first()
                weibo['source'] = source if source else ''

                weibo['retweet_id'] = ''
                if retweet_sel and retweet_sel[0].xpath('.//div[@node-type="feed_list_forwardContent"]/a[1]'):
                    retweet = WeiboItem()
                    retweet['id'] = retweet_sel[0].xpath('.//a[@action-type="feed_list_like"]/@action-data').extract_first()[4:]
                    retweet['bid'] = retweet_sel[0].xpath('.//p[@class="from"]/a/@href').extract_first().split('/')[-1].split('?')[0]
                    info = retweet_sel[0].xpath('.//div[@node-type="feed_list_forwardContent"]/a[1]')[0]
                    retweet['user_id'] = info.xpath('@href').extract_first().split('/')[-1]
                    retweet['screen_name'] = info.xpath('@nick-name').extract_first()
                    retweet['vip_type'], retweet['vip_level'] = self.get_vip(info)
                    retweet['text'] = retweet_txt_sel.xpath('string(.)').extract_first().replace('\u200b', '').replace('\ue627', '')
                    retweet['location'] = self.get_location(retweet['text'])
                    retweet['text'] = retweet['text'][2:].replace(' ', '')
                    if is_long_retweet:
                        retweet['text'] = retweet['text'][:-4]
                    retweet['at_users'] = self.get_at_users(retweet_txt_sel)
                    retweet['topics'] = self.get_topics(retweet_txt_sel)
                    reposts_count = retweet_sel[0].xpath('.//ul[@class="act s-fr"]/li[1]/a[1]/text()').extract_first()
                    reposts_count = re.findall(r'\d+.*', reposts_count)
                    retweet['reposts_count'] = reposts_count[0] if reposts_count else '0'
                    comments_count = retweet_sel[0].xpath('.//ul[@class="act s-fr"]/li[2]/a[1]/text()').extract_first()
                    comments_count = re.findall(r'\d+.*', comments_count)
                    retweet['comments_count'] = comments_count[0] if comments_count else '0'
                    attitudes_count = retweet_sel[0].xpath('.//a[@class="woo-box-flex woo-box-alignCenter woo-box-justifyCenter"]//span[@class="woo-like-count"]/text()').extract_first()
                    attitudes_count = re.findall(r'\d+.*', attitudes_count)
                    retweet['attitudes_count'] = attitudes_count[0] if attitudes_count else '0'
                    created_at = retweet_sel[0].xpath('.//p[@class="from"]/a[1]/text()').extract_first().replace(' ', '').replace('\n', '').split('前')[0]
                    retweet['created_at'] = util.standardize_date(created_at)

                    # 过滤转发微博的互动度
                    try:
                        rep = int(retweet['reposts_count'])
                        com = int(retweet['comments_count'])
                        att = int(retweet['attitudes_count'])
                        if rep == 0 and com == 0 and att == 0:
                            print("转发微博 转发/评论/点赞全为0，已过滤")
                            continue
                    except:
                        print("转发微博 互动数据异常，已过滤")
                        continue



                    # 时间过滤
                    try:
                        time_str = retweet['created_at']
                        retweet_dt = datetime.strptime(time_str, "%Y-%m-%d %H:%M")
                        retweet_date = retweet_dt.date()
                        start_date = datetime.strptime(self.start_date, '%Y-%m-%d').date()
                        end_date = datetime.strptime(self.end_date, '%Y-%m-%d').date()
                        if not (start_date <= retweet_date <= end_date):
                            print(f"转发微博时间不在范围内: {time_str}")
                            continue
                    except Exception as e:
                        print(f"转发微博时间解析失败，丢弃")
                        continue

                    source = retweet_sel[0].xpath('.//p[@class="from"]/a[2]/text()').extract_first()
                    retweet['source'] = source if source else ''
                    # retweet['pics'] = pics
                    # retweet['video_url'] = video_url
                    retweet['retweet_id'] = ''
                    self.result_count += 1
                    yield {'weibo': retweet, 'keyword': keyword}
                    if self.check_limit():
                        return
                    weibo['retweet_id'] = retweet['id']

                auth_icon = sel.xpath('.//div[@class="avator"]//span[contains(@class,"woo-avatar-icon")]')
                if auth_icon:
                    title = auth_icon.xpath('@title').extract_first(default='')
                    if "微博官方认证" in title:
                        weibo['user_authentication'] = "官方媒体(蓝V)"
                    elif "微博个人认证" in title:
                        svg_id = auth_icon.xpath('.//svg/@id').extract_first(default='')
                        if "vorange" in svg_id:
                            weibo['user_authentication'] = "个人红V"
                        elif "vyellow" in svg_id:
                            weibo['user_authentication'] = "个人黄V"
                        elif "vgold" in svg_id:
                            weibo['user_authentication'] = "个人金V"
                        else:
                            weibo['user_authentication'] = "个人认证"
                else:
                    weibo['user_authentication'] = '未认证用户'
                print(weibo)
                self.result_count += 1
                yield {'weibo': weibo, 'keyword': keyword}
                if self.check_limit():
                    return

