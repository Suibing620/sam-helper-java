import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.hutool.core.thread.ThreadUtil.sleep;

/**
 * 接口封装
 */
public class Api {

    public static final Map<String, Object> context = new HashMap<>();
    public static final List<GoodDto> limitGood = new ArrayList<>();

    /**
     * 获取用户初始化信息，收货地址信息和匹配商店信息。为app上设定的默认值
     *
     * @return 信息集合
     */
    public static Boolean init(String deliveryType) {
        try {
            if ("1".equals(deliveryType)) {
                context.put("deliveryType", "1");
                context.put("cartDeliveryType", "1");
                context.put("storeType", 4);
            } else if ("2".equals(deliveryType) || "3".equals(deliveryType)) {
                context.put("deliveryType", "2");
                context.put("cartDeliveryType", "2");
                context.put("storeType", 2);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SneakyThrows
    public static void play(String message) {
        new Thread(() -> {
            while (true) {
                // bark推送
                if (!UserConfig.barkId.isEmpty()) {
                    barkNotice(UserConfig.barkId, message);
                }
                // Server 酱推送
                if (!UserConfig.ftqqSendKey.isEmpty()) {
                    ftqqNotice(UserConfig.ftqqSendKey, message);
                }
                //这里还可以使用企业微信或者钉钉的提供的webhook  自己写代码 很简单 就是按对应数据格式发一个请求到企业微信或者钉钉
                try {
                    AudioClip audioClip = Applet.newAudioClip(new File("ding-dong.wav").toURL());
                    audioClip.loop();
                    Thread.sleep(6000);//响铃6秒
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
        }).start();
    }

    public static void print(boolean normal, String message) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        if (normal) {
            System.out.println(time + message);
        } else {
            System.err.println(time + message);
        }
    }


    /**
     * 验证请求是否成功
     *
     * @param object     返回体
     * @param actionName 动作名称
     * @return 是否成功
     */
    private static boolean isSuccess(JSONObject object, String actionName) {
        Boolean success = object.getBool("success");
        if (success == null) {
            print(false, "【失败】" + actionName + " 服务器返回无法解析的内容:" + JSONUtil.toJsonStr(object));
            return false;
        }
        if (success) {
            return true;
        }
        print(false, "【失败】" + actionName + " 原因:" + object.get("msg"));
        return false;
    }

    /**
     * 获取默认的下单地址信息
     *
     * @return 地址信息Map
     */
    @Deprecated
    public static Map<String, Object> getDeliveryAddressDetail() {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/trade/cart/getDeliveryAddressDetail");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "获取下单地址")) {
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            JSONObject data = object.getJSONObject("data");
            map.put("addressId", data.getStr("addressId"));
//            context.put("latitude", data.getStr("latitude"));
//            context.put("longitude", data.getStr("longitude"));
            print(true, "【成功】获取收货地址"
                    + " 收货地址：" + data.getStr("cityName") + data.getStr("districtName") + data.getStr("detailAddress")
                    + " 收货人：" + data.getStr("name") + " 手机号：" + data.getStr("phone"));
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取匹配的商店信息
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 商店信息Map
     */
    public static Map<String, Object> getMiniUnLoginStoreList(Double latitude, Double longitude) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/merchant/storeApi/getMiniUnLoginStoreList");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();
            request.put("latitude", latitude);
            request.put("longitude", longitude);
            request.put("requestType", "location_recmd");

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "获取商店信息")) {
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            JSONArray storeList = object.getJSONObject("data").getJSONArray("storeList");
            Iterator<Object> iterator = storeList.iterator();
            while (iterator.hasNext()) {
                JSONObject store = (JSONObject) iterator.next();
                if (store.getInt("storeType").equals(context.get("storeType"))) {
                    map.put("storeType", store.getInt("storeType"));
                    map.put("storeId", store.getStr("storeId"));
                    map.put("storeDeliveryTemplateId", store.getJSONObject("storeRecmdDeliveryTemplateData").getStr("storeDeliveryTemplateId"));
                    map.put("areaBlockId", store.getJSONObject("storeAreaBlockVerifyData").getStr("areaBlockId"));
                    map.put("deliveryModeId", store.getJSONObject("storeDeliveryModeVerifyData").getStr("deliveryModeId"));
                    map.put("storeName", store.getStr("storeName"));
                }
            }
            if (map.isEmpty()) {
                print(false, "【失败】未获取到商店信息/商店未营业");
                return null;
            }
            print(true, "【成功】获取商店信息" + " 商店名称：" + map.get("storeName") + " 商店ID：" + map.get("storeId"));
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取配送时间
     *
     * @param storeDetail 商店信息
     * @return 配送信息Map
     */
    public static Map<String, Object> getCapacityData(Map<String, Object> storeDetail) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/delivery/portal/getCapacityData");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();

            List<String> date = new ArrayList<>();
            DateTime dateTime = new DateTime();
            for (int j = 0; j < 7; j++) {
                date.add(dateTime.toString("yyyy-MM-dd"));
                dateTime.offset(DateField.DAY_OF_MONTH, 1);
            }
            request.put("perDateList", date);
            request.put("storeDeliveryTemplateId", storeDetail.get("storeDeliveryTemplateId"));

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "更新配送时间")) {
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            JSONArray capcityResponseList = object.getJSONObject("data").getJSONArray("capcityResponseList");
            //对于只有一个时间段段配送直接预判处理
            if (capcityResponseList.size() == 1) {
                JSONArray list = capcityResponseList.getJSONObject(0).getJSONArray("list");
                if (list.size() == 1) {
                    JSONObject time = list.getJSONObject(0);
                    map.put("startRealTime", time.get("startRealTime"));
                    map.put("endRealTime", time.get("endRealTime"));
                    print(true, "【成功】单一配送时间，预处理:" + capcityResponseList.getJSONObject(0).getStr("strDate") + " " + time.getStr("startTime") + " -- " + time.getStr("endTime"));
                    return map;
                }
            }

            for (int i = 0; i < capcityResponseList.size(); i++) {
                JSONObject capcityResponse = capcityResponseList.getJSONObject(i);
                if (!capcityResponse.getBool("dateISFull")) {
                    JSONArray times = capcityResponse.getJSONArray("list");
                    for (int j = 0; j < times.size(); j++) {
                        JSONObject time = times.getJSONObject(j);
                        if (!time.getBool("timeISFull")) {
                            map.put("startRealTime", time.get("startRealTime"));
                            map.put("endRealTime", time.get("endRealTime"));
                            print(true, "【成功】更新配送时间:" + capcityResponse.getStr("strDate") + " " + time.getStr("startTime") + " -- " + time.getStr("endTime"));
                            return map;
                        }
                    }
                }
            }
            print(false, "【失败】全部配送时间已满");
        } catch (JSONException e) {
            print(false, "【失败】请求过快被风控，请调整参数");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取预估的配送时间
     *
     * @param storeDetail 商店信息
     * @return 配送信息Map
     */
    @Deprecated
    public static Map<String, Object> getGuessData(Map<String, Object> storeDetail) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/delivery/portal/getCapacityData");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();

            List<String> date = new ArrayList<>();
            DateTime dateTime = new DateTime();
            for (int j = 0; j < 7; j++) {
                date.add(dateTime.toString("yyyy-MM-dd"));
                dateTime.offset(DateField.DAY_OF_MONTH, 1);
            }
            request.put("perDateList", date);
            request.put("storeDeliveryTemplateId", storeDetail.get("storeDeliveryTemplateId"));

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "更新配送时间")) {
                return null;
            }
            Map<String, Object> map = new HashMap<>();
            JSONArray times = object.getJSONObject("data").getJSONArray("capcityResponseList").getJSONObject(0).getJSONArray("list");
            if (times.size() > 1) {
                //直接拿下一个时间
                JSONObject time = times.getJSONObject(1);
                map.put("startRealTime", time.get("startRealTime"));
                map.put("endRealTime", time.get("endRealTime"));
            } else {
                //计算明天的时间
                JSONObject time = times.getJSONObject(0);
                map.put("startRealTime", new BigDecimal(time.getStr("startRealTime")).add(new BigDecimal(86400000)).toString());
                map.put("endRealTime", new BigDecimal(time.getStr("endRealTime")).add(new BigDecimal(86400000)).toString());
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取购物车信息
     *
     * @param storeDetail 商店信息
     * @return 购物车商品列表
     */
    public static List<GoodDto> getCart(Map<String, Object> storeDetail) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/trade/cart/getUserCart");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();

            List<Map> storeList = new ArrayList();
            Map<String, Object> store = new HashMap<>();
            store.put("storeType", storeDetail.get("storeType"));
            store.put("storeId", storeDetail.get("storeId"));
            store.put("areaBlockId", storeDetail.get("areaBlockId"));
            store.put("storeDeliveryTemplateId", storeDetail.get("storeDeliveryTemplateId"));
            storeList.add(store);
            request.put("storeList", storeList);

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "更新购物车")) {
                return null;
            }
            Integer selectedNumber = object.getJSONObject("data").getInt("selectedNumber");
            if (selectedNumber == 0) {
                print(false, "【失败】购物车目前暂无可下单商品");
                return null;
            } else {
                double amount = 0;
                JSONArray goods = object.getJSONObject("data").getJSONObject("miniProgramGoodsInfo").getJSONArray("normalGoodsList");
                List<GoodDto> goodDtos = new ArrayList<>();
                for (int i = 0; i < goods.size(); i++) {
                    JSONObject good = goods.getJSONObject(i);
                    if (good.getBool("isSelected")
                            && (Objects.equals(good.getInt("storeType"), storeDetail.get("storeType"))
                    )) {
                        GoodDto goodDto = new GoodDto();
                        goodDto.setSpuId(good.getStr("spuId"));
                        if (Api.limitGood.contains(goodDto)) {
                            break;
                        }
                        if (good.getInt("quantity") >= good.getInt("stockQuantity")) {
                            goodDto.setQuantity(good.getStr("stockQuantity"));
                        } else {
                            goodDto.setQuantity(good.getStr("quantity"));
                        }
                        if (!good.getJSONObject("purchaseLimitVO").isEmpty()) {
                            goodDto.setIsLimited(true);
                            if (good.getJSONObject("purchaseLimitVO").getInt("limitNum") < Integer.valueOf(goodDto.getQuantity())) {
                                goodDto.setQuantity(good.getJSONObject("purchaseLimitVO").getStr("limitNum"));
                            }
                        }
                        goodDto.setStoreId(good.getStr("storeId"));
                        goodDto.setWeight(good.getDouble("weight"));
                        goodDto.setPrice(new BigDecimal(good.getStr("price")).divide(new BigDecimal("100")).doubleValue());
                        amount = BigDecimal.valueOf(amount).add(BigDecimal.valueOf(goodDto.getPrice()).multiply(new BigDecimal(goodDto.getQuantity()))).doubleValue();
                        goodDtos.add(goodDto);
                    }
                }
                context.put("amount", amount);
                print(true, "【成功】更新购物车，可下单总金额：" + amount + "元");
                return goodDtos;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void barkNotice(String barkId, String message) {
        // sound=minuet 这里可在bark app选择自己喜爱的铃声
        HttpRequest httpRequest = HttpUtil.createGet("https://api.day.app/" + barkId + "/山姆下单助手：" + message + "?sound=minuet");
        String body = httpRequest.execute().body();
    }

    public static void ftqqNotice(String sendKey, String message) {
        HttpRequest httpRequest = HttpUtil.createPost("https://sctapi.ftqq.com/" + sendKey + ".send?title=" + message);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        httpRequest.addHeaders(headers);
        String body = httpRequest.execute().body();
    }

    /**
     * 提交订单
     *
     * @param goods        商品信息
     * @param capacityData 配送信息
     * @param addressDto   配送地址信息
     * @param storeDetail  商店信息
     * @return 下单成功与否
     */
    public static Boolean commitPay(List<GoodDto> goods, Map<String, Object> capacityData, AddressDto addressDto, Map<String, Object> storeDetail, List<CouponDto> couponDtoList) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/trade/settlement/commitPay");

            Map<String, String> headers = UserConfig.getHeaders();
            headers.put("track-info", "[{\"labelType\":\"push_trace\",\"attachId\":\"\"},{\"labelType\":\"systemMessage_trace\",\"attachId\":\"\"},{\"labelType\":\"apppushmsgtaskid_trace\",\"attachId\":\"\"},{\"labelType\":\"systemmsgtasksubid_trace\",\"attachId\":\"\"},{\"labelType\":\"tracking_id\",\"attachId\":\"1649869176133-01DBB03D-BC5C-4C49-896C-F05FC7688BED\"},{\"labelType\":\"tracepromotion\",\"createTime\":\"\",\"attachId\":\"\"}]");
            httpRequest.addHeaders(headers);

            Map<String, Object> request = UserConfig.getIdInfo();
            request.put("goodsList", goods);
            request.put("invoiceInfo", new HashMap<>());
            request.put("sceneCode", 1074);
            request.put("isSelectShoppingNotes", true);
            request.put("cartDeliveryType", context.get("cartDeliveryType"));
            request.put("floorId", 1);
            request.put("amount", 100);
            request.put("payType", 0);
            request.put("currency", "CNY");
            request.put("channel", "wechat");
            request.put("shortageId", 1);
            request.put("orderType", 0);
            request.put("remark", "");
            request.put("addressId", addressDto.getAddressId());
            request.put("shortageDesc", "其他商品继续配送（缺货商品直接退款）");
            request.put("labelList", "[{\"attachId\":\"1649949934151-1a291f41-999c-4859-8f7e-f64516ac292f\",\"createTime\":1649949934287,\"labelType\":\"tracking_id\"},{\"attachId\":1074,\"createTime\":1649949934289,\"labelType\":\"scene_xcx\"}]");
            request.put("payMethodId", "contract");

            Map<String, Object> deliveryInfoVO = new HashMap<>();
            deliveryInfoVO.put("storeDeliveryTemplateId", storeDetail.get("storeDeliveryTemplateId"));
            deliveryInfoVO.put("deliveryModeId", storeDetail.get("deliveryModeId"));
            deliveryInfoVO.put("storeType", storeDetail.get("storeType"));

            request.put("deliveryInfoVO", deliveryInfoVO);
            Map<String, Object> settleDeliveryInfo = new HashMap<>();
            settleDeliveryInfo.put("expectArrivalTime", capacityData.get("startRealTime"));
            settleDeliveryInfo.put("expectArrivalEndTime", capacityData.get("endRealTime"));
            settleDeliveryInfo.put("deliveryType", context.get("deliveryType"));
            request.put("settleDeliveryInfo", settleDeliveryInfo);

            Map<String, Object> storeInfo = new HashMap<>();
            storeInfo.put("storeId", storeDetail.get("storeId"));
            storeInfo.put("storeType", storeDetail.get("storeType"));
            storeInfo.put("areaBlockId", storeDetail.get("areaBlockId"));
            request.put("storeInfo", storeInfo);

            Double amount = 0.0;
            for (GoodDto good : goods) {
                amount = BigDecimal.valueOf(amount).add(BigDecimal.valueOf(good.getPrice()).multiply(new BigDecimal(good.getQuantity()))).doubleValue();
            }
            List<Map> couponList = new ArrayList<>();
            if (UserConfig.coupon && !couponDtoList.isEmpty()) {
                couponDtoList.forEach(couponDto -> {
                    Map<String, String> couponMap = new HashMap<>();
                    couponMap.put("promotionId", couponDto.getRuleId());
                    couponMap.put("storeId", (String) storeDetail.get("storeId"));
                    couponList.add(couponMap);
                });
            }
            request.put("couponList", couponList);
            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            if (body == null || body.isEmpty()) {
                print(false, "下单失败，可能触发403限流");
                return false;
            }
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "提交订单")) {
                return false;
            }
            print(true, "【恭喜你】已成功下单 当前下单总金额：" + amount + "元");
            context.put("amount", amount);
            Api.limitGood.addAll(goods.stream().filter(GoodDto::getIsLimited).collect(Collectors.toList()));
            return true;
        } catch (JSONException e) {
            print(false, "【失败】请求过快被风控，请调整参数");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取保供套餐信息
     *
     * @param storeDetail 商店信息
     * @return 购物车商品列表
     */
    @Deprecated
    public static List<GoodDto> getGoodsListByCategoryId(Map<String, Object> storeDetail) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/goods-portal/grouping/list");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();

            request.put("pageSize", 20);
            request.put("isReversOrder", false);
            request.put("useNewPage", true);
            request.put("frontCategoryIds", Arrays.asList("10012335", "10012336"));
            request.put("isFastDelivery", false);
            request.put("addressVO", new HashMap<>());
            request.put("secondCategoryId", "10012335");
            request.put("pageNum", 1);

            List<Map> storeList = new ArrayList();
            Map<String, Object> store = new HashMap<>();
            store.put("storeType", storeDetail.get("storeType"));
            store.put("storeId", storeDetail.get("storeId"));
            storeList.add(store);
            request.put("storeInfoVOList", storeList);

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "获取保供套餐列表")) {
                return null;
            }
            JSONArray goods = object.getJSONObject("data").getJSONArray("dataList");
            List<GoodDto> goodDtos = new ArrayList<>();
            for (int i = 0; i < goods.size(); i++) {
                JSONObject good = goods.getJSONObject(i);
                Integer stockQuantity = good.getJSONObject("stockInfo").getInt("stockQuantity");
                if (stockQuantity > 0) {
                    GoodDto goodDto = new GoodDto();
                    goodDto.setSpuId(good.getStr("spuId"));
                    goodDto.setQuantity("1");
                    goodDto.setStoreId(good.getStr("storeId"));
                    goodDtos.add(goodDto);
                    JSONArray priceInfoList = good.getJSONArray("priceInfo");
                    Iterator<Object> iterator = priceInfoList.iterator();
                    double price = 0;
                    while (iterator.hasNext()) {
                        JSONObject priceInfo = (JSONObject) iterator.next();
                        if (priceInfo.getInt("priceType") == 4) {
                            price = new BigDecimal(priceInfo.getStr("price")).divide(new BigDecimal("100")).doubleValue();
                        }
                    }
                    System.out.println(good.getStr("title") + " 价格：" + price + " 剩余库存：" + stockQuantity + "\n" + good.getStr("subTitle"));
                }
            }
            if (goodDtos.isEmpty()) {
                print(false, "【失败】暂未获取到有货的保供套餐");
                return null;
            }
            print(true, "【成功】获取到保供套餐");
            return goodDtos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<GoodDto> getPageData(Map<String, Object> storeDetail) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/decoration/portal/show/getPageData");
            Map<String, String> headers = UserConfig.getHeaders();
            httpRequest.addHeaders(headers);
            Map<String, Object> request = UserConfig.getIdInfo();

            request.put("pageContentId", "1187641882302384150");
            request.put("authorize", true);
//            request.put("latitude", Double.parseDouble(addressDto.getLatitude()));
//            request.put("longitude", Double.parseDouble(addressDto.getLongitude()));
            request.put("storeInfoList", Arrays.asList(storeDetail));

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            if (body == null || body.isEmpty()) {
                print(false, "请求失败，可能触发403限流");
                return null;
            }
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess
                    (object, "获取保供套餐列表")) {
                return null;
            }
            JSONArray pageModuleVOList = object.getJSONObject("data").getJSONArray("pageModuleVOList");
            List<GoodDto> goodDtos = new ArrayList<>();
            for (int i = 0; i < pageModuleVOList.size(); i++) {
                JSONObject renderContent = pageModuleVOList.getJSONObject(i).getJSONObject("renderContent");
                if (renderContent.getJSONArray("goodsList") != null && !renderContent.getJSONArray("goodsList").isEmpty()) {
                    JSONArray goods = renderContent.getJSONArray("goodsList");
                    for (int h = 0; h < goods.size(); h++) {
                        JSONObject good = goods.getJSONObject(h);
                        if (good.getBool("isAvailable") != null
                                && good.getBool("isAvailable")
                                && UserConfig.whitelist.stream().anyMatch(title -> good.getStr("title").contains(title))
                        ) {
                            Integer stockQuantity = good.getJSONObject("stockInfo").getInt("stockQuantity");
                            JSONArray priceInfoList = good.getJSONArray("priceInfo");
                            Iterator<Object> iterator = priceInfoList.iterator();
                            double price = 0;
                            while (iterator.hasNext()) {
                                JSONObject priceInfo = (JSONObject) iterator.next();
                                if (priceInfo.getInt("priceType") == 4) {
                                    price = new BigDecimal(priceInfo.getStr("price")).divide(new BigDecimal("100")).doubleValue();
                                }
                            }
                            if (stockQuantity > 0) {
                                GoodDto goodDto = new GoodDto();
                                goodDto.setSpuId(good.getStr("spuId"));
                                goodDto.setQuantity("1");
                                goodDto.setStoreId(good.getStr("storeId"));
                                goodDto.setPrice(price);
                                goodDto.setIsLimited(true);
                                goodDtos.add(goodDto);
                                System.out.println(good.getStr("title") + " 价格：" + price + "元 剩余库存：" + stockQuantity + "\n" + good.getStr("subTitle"));
                            } else {
                                System.out.println(good.getStr("title") + " 价格：" + price + "元 剩余库存：" + stockQuantity);
                            }
                        }
                    }
                }
            }
            if (goodDtos.isEmpty()) {
                print(false, "【失败】暂未获取到有货的保供套餐");
                return null;
            }
            print(true, "【成功】获取到保供套餐");
            return goodDtos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 将商品添加到购物车
     *
     * @param goodDtos 商品信息
     * @return
     */
    public static Boolean addCartGoodsInfo(List<GoodDto> goodDtos) {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/trade/cart/addCartGoodsInfo");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();

            List<Map> cartGoodsInfoList = new ArrayList();
            goodDtos.forEach(goodDto -> {
                Map<String, Object> cartGoodsInfo = new HashMap<>();
                cartGoodsInfo.put("componentPath", "1");
                cartGoodsInfo.put("goodsName", "1");
                cartGoodsInfo.put("price", "1");
                cartGoodsInfo.put("event_tracking_id", "sam_app_cart_category_buy");
                cartGoodsInfo.put("increaseQuantity", goodDto.getQuantity());
                cartGoodsInfo.put("storeId", goodDto.getStoreId());
                cartGoodsInfo.put("spuId", goodDto.getSpuId());
                cartGoodsInfoList.add(cartGoodsInfo);
            });
            request.put("cartGoodsInfoList", cartGoodsInfoList);

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "添加商品至购物车")) {
                return null;
            }
            print(true, "【成功】添加至购物车");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<CouponDto> getCouponList() {
        try {
            HttpRequest httpRequest = HttpUtil.createPost("https://api-sams.walmartmobile.cn/api/v1/sams/coupon/coupon/query");
            httpRequest.addHeaders(UserConfig.getHeaders());
            Map<String, Object> request = UserConfig.getIdInfo();
            request.put("pageSize", 20);
            request.put("pageNum", 1);
            request.put("status", "1");

            httpRequest.body(JSONUtil.toJsonStr(request));
            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "获取优惠券")) {
                return null;
            }
            JSONArray couponList = object.getJSONObject("data").getJSONArray("couponInfoList");
            List<CouponDto> couponDtoList = new ArrayList<>();
            for (int i = 0; i < couponList.size(); i++) {
                CouponDto couponDto = new CouponDto();
                couponDto.setCondition(Integer.parseInt(couponList.getJSONObject(i).getJSONObject("promotion").getJSONObject("condition").getStr("value")) / 100);
                couponDto.setDiscount(Integer.parseInt(couponList.getJSONObject(i).getJSONObject("promotion").getJSONObject("discount").getStr("value")) / 100);
                couponDto.setRuleId(couponList.getJSONObject(i).getStr("ruleId"));
                couponDtoList.add(couponDto);

            }
            print(true, "【成功】获取优惠卷，共计" + couponDtoList.size() + "张");
            return couponDtoList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean timeTrigger(String time) {
        String timeColonPattern = "HH:mm:ss";
        DateTimeFormatter timeColonFormatter = DateTimeFormatter.ofPattern(timeColonPattern);
        LocalTime parse = LocalTime.parse(time, timeColonFormatter);
        LocalTime now = LocalTime.now();
        if (now.isAfter(parse)) {
            return true;
        }
        System.out.println("时间触发 当前时间 " + now.format(timeColonFormatter) + " 目标时间 " + time);
        sleep(1000);
        return false;
    }

    public static List<AddressDto> getAddress() {
        try {
            HttpRequest httpRequest = HttpUtil.createGet("https://api-sams.walmartmobile.cn/api/v1/sams/sams-user/receiver_address/address_list");
            httpRequest.addHeaders(UserConfig.getHeaders());
            httpRequest.form(UserConfig.getIdInfo());

            String body = httpRequest.execute().body();
            JSONObject object = JSONUtil.parseObj(body);
            if (!isSuccess(object, "获取收货地址列表")) {
                return null;
            }
            JSONArray addressList = object.getJSONObject("data").getJSONArray("addressList");
            List<AddressDto> addressDtoList = new ArrayList<>();
            for (int i = 0; i < addressList.size(); i++) {
                JSONObject address = addressList.getJSONObject(i);
                System.out.println(
                        "序号：" + i
                                + " 收货地址：" + address.getStr("cityName") + address.getStr("districtName") + address.getStr("receiverAddress")
                                + " 收货人：" + address.getStr("name") + " 手机号：" + address.getStr("mobile"));
                AddressDto addressDto = new AddressDto();
                addressDto.setAddressId(address.getStr("addressId"));
                addressDto.setLatitude(address.getStr("latitude"));
                addressDto.setLongitude(address.getStr("longitude"));
                addressDtoList.add(addressDto);
            }
            if (!addressList.isEmpty()) {
                print(true, "【成功】获取收货地址，共计" + addressList.size() + "个");
                return addressDtoList;
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
