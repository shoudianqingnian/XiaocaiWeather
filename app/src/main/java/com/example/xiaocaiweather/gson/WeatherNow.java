package com.example.xiaocaiweather.gson;

import com.google.gson.annotations.SerializedName;

public class WeatherNow {
    @SerializedName("code")
    public String nowcode; //状态码
    public NowResult nowResult; //为子类定义对象
    class NowResult{
        @SerializedName("temp")
        public String nowtemp; //温度
        @SerializedName("text")
        public String nowtext; //当前天气
        @SerializedName("vis")
        public String nowvis;//能见度
    }
}
