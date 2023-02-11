package com.example.xiaocaiweather.util;

import android.text.TextUtils;

import com.example.xiaocaiweather.db.City;
import com.example.xiaocaiweather.db.County;
import com.example.xiaocaiweather.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//用于解析服务器返回的json数据
public class Utility {

    //解析省级数据
    public static boolean handleProvinceResponse(String response){
        if(!TextUtils.isEmpty(response)){ //若响应内容不为空
            try{
                JSONArray allProvinces=new JSONArray(response);
                for(int i=0;i< allProvinces.length();i++){
                    JSONObject provinceObject=allProvinces.getJSONObject(i);
                    Province province=new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save(); //将数据存储到数据库中
                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
        return false; //手动默认
    }

    //解析市级数据
    public static boolean handleCityResponse(String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCities = new JSONArray(response);
                for (int i = 0; i < allCities.length(); i++) {
                    JSONObject cityObject = allCities.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //解析县级数据
    public static boolean handleCountyResponse(String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
