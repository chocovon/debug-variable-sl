package util;

import com.alibaba.fastjson.JSON;

public class JsonUtil {
    public static String toJSONString(Object object, boolean prettyFormat) {
        return JSON.toJSONString(object, prettyFormat);
    }
}
