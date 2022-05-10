# sam-helper
山姆自动下单，提供捡漏模式和抢单模式，手机端添加购物车，自动下单，去手机端支付既可。

# 注意事项
本程序功能简易，适合有java基础的朋友使用。

希望一键使用的朋友，推荐隔壁的go项目。
https://github.com/robGoods/sams

目前只能下单普通商品，暂无法购买全球购等渠道。

抢单不易，请勿用于商业牟利。


# 简易说明

8点极速达：提前一天先加好购物车，然后运行Sentinel，记得修改UserConfig中的deliveryType。

11点保供套餐：提前运行GuaranteeSentinel即可。

14点全城送：提前一天加好购物车，然后运行Sentinel，记得修改UserConfig中的deliveryType。

# 使用说明
普通捡漏（Sentinel）：程序检测是否达到目标金额，自动选择最近配送时间并下单。 限购商品，提交数量不超过限购值，极速达超重商品自动拆分下单。

保供套餐捡漏（GuaranteeSentinel）：自动检测保供套餐是否上架，自动将上架的保供套餐逐一加入购物车并单独下单。已经下单过的套餐不会重复下单。

程序端：根据抓包数据，填写UserConfig文件中的变量，运行测试（AplicationTest）查看能否正确获得购物车信息。

手机端：app上添加商品至购物车，确认下单地址，下单成功后进行付款。

电脑端：微信小程序，抓包接口数据。以及运行程序。

# 抓包说明
使用抓包软件，mac下charles win下fiddler，抓取山姆小程序打开购物车触发的这个接口

![headers](https://github.com/NotwoJack/sam-helper/blob/main/image/headers.png)

# 测试环境
Mac 微信小程序 浦东 全城送 微信支付

# 更新记录

## 2022.05.10
1. 恢复哨兵模式下的全城送捡漏模式
2. 添加倒计时功能(重置时间节点为0点)

## 2022.05.05
1. 新增购物车商品校验，限购商品下单后自动剔除，超重订单自动分批下单

## 2022.05.02
1. 添加优惠券功能（目前只支持满减卷，自动匹配最大金额下单）
2. 两个模式模式均修改为持续不间断运行

## 2022.04.26
1. 大幅优化下单逻辑，减少延时（因为waf加强，已经回滚）

## 2022.04.24
1. 保供套餐自动检测下单功能，独立出来（可以使用，待优化）
2. 优化极速达捡漏下单功能，目前可以全天候持续捡漏下单

## 2022.04.21
1. 新增保供套餐自动检测下单功能

## 2022.04.18
1. 减少哨兵模式的请求频率避免风控
2. userconfig中的lablelist为非必填项，写死

## 2022.04.17
1. 完善代码
2. 减少并发数，避免风控
3. 更新readme
4. 添加抓包说明

## 2022.04.16
1. 哨兵捡漏模式，用于日常捡漏。
2. 完善代码，添加注释
3. 更新readme

# 特别感谢
代码结构参考至 https://github.com/JannsenYang/dingdong-helper

山姆接口参考至 https://github.com/azhan1998/sam_buy

# 交流群组
![qrcode](https://github.com/NotwoJack/sam-helper/blob/main/image/qrcode.png)
