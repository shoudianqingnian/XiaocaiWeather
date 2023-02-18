package com.example.xiaocaiweather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xiaocaiweather.gson.Weather;
import com.example.xiaocaiweather.gson.WeatherNow;
import com.example.xiaocaiweather.util.HttpUtil;
import com.example.xiaocaiweather.util.Utility;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private final static int SAVE_NOWMESSAGE=0;
    private final static int SAVE_FORECASTMESSAGE=1;
    private final static int SAVE_COMFORTMESSAGE=2;
    private final static int SAVE_AIRMESSAGE=3;

    private ScrollView weatherLayout;
    private TextView titleCity;  //城市名
    private TextView titleUpdateTime; //更新日期
    private TextView degreeText; //当前气温
    private TextView weatherInfoText; //当前天气
    private LinearLayout forecastLayout;
    private TextView aqiText; //aqi指数
    private TextView pm25Text; //pm2.5指数
    private TextView comfortText; //舒适度
    private TextView carWashText; //洗车指数
    private TextView sportText;//运动建议
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化各控件
        weatherLayout=(ScrollView) findViewById(R.id.weather_layout);
        titleCity=(TextView) findViewById(R.id.title_city);
        titleUpdateTime=(TextView) findViewById(R.id.title_updatetime);
        degreeText=(TextView)findViewById(R.id.degree_text);
        weatherInfoText=(TextView)findViewById(R.id.weather_info_text);
        forecastLayout=(LinearLayout) findViewById(R.id.forecast_layout);
        aqiText=(TextView)findViewById(R.id.aqi_text);
        pm25Text=(TextView)findViewById(R.id.pm25_text);
        comfortText=(TextView)findViewById(R.id.comfort_text);
        carWashText=(TextView)findViewById(R.id.car_wash_text);
        sportText=(TextView)findViewById(R.id.sport_text);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String nowMessage=prefs.getString("nowmessage",null);  //实时天气类信息
        String forecastMessage=prefs.getString("forecastmessage",null); //7天预报信息
        String comfortMessage=prefs.getString("comfortmessage",null); //天气指数信息
        String airMessage=prefs.getString("airmessage",null); //空气质量信息

        String weatherId=getIntent().getStringExtra("weather_id"); //获取weatherid
        String mysecrettoken="你的token"; //个人的（和风天气）访问token

        //处理实时天气数据
        if(nowMessage!=null){ //有缓存时直接展示实时天气数据
            showNowWeatherInfo(nowMessage);
        }
        else{
            weatherLayout.setVisibility(View.INVISIBLE); //设置滚动界面不可见
            String requestUrl="https://devapi.qweather.com/v7/weather/now?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,0); //请求城市 当前实时天气信息
        }

        //处理预报天气数据
        if(forecastMessage!=null){ //有缓存时直接展示预报天气数据
            showForecastWeatherInfo(forecastMessage);
        }
        else{
            weatherLayout.setVisibility(View.INVISIBLE); //设置滚动界面不可见
            String requestUrl="https://devapi.qweather.com/v7/weather/7d?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,1); //请求城市 当前预报天气信息
        }

        //处理舒适度数据
        if(comfortMessage!=null){ //有缓存时直接展示舒适度数据
            showComfortInfo(comfortMessage);
        }
        else{
            weatherLayout.setVisibility(View.INVISIBLE); //设置滚动界面不可见
            String requestUrl="https://devapi.qweather.com/v7/indices/1d?type=1,2,8?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,2); //请求城市 当前1天内的舒适度信息
        }

        //处理空气质量数据
        if(airMessage!=null){ //有缓存时直接展示空气质量数据
            showAirInfo(airMessage);
        }
        else{
            weatherLayout.setVisibility(View.INVISIBLE); //设置滚动界面不可见
            String requestUrl="https://devapi.qweather.com/v7/air/now?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,3); //请求城市 当前空气质量信息
        }
    }

    //根据天气id请求城市天气信息
    public void requestWeather(final String requestUrl,int saveData){
        HttpUtil.sendOkHttpRequest(requestUrl, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取数据失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseText=response.body().string();
                //final WeatherNow weathernow= Utility.handleWeatherNowResponse(responseText); //用Gson解析JSON数据

                try{
                    JSONObject jsonObject=new JSONObject(responseText); //用原生的jsonObject解析json数据
                    String statuscode=jsonObject.getString("code"); //获取网络请求状态码
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!responseText.isEmpty() && "200".equals(statuscode)){
                                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                                switch (saveData){
                                    case 0:
                                        editor.putString("nowmessage",responseText); //向sharedpreferences中存储nowmessage信息
                                        editor.apply();
                                        showNowWeatherInfo(responseText);
                                        break;
                                    case 1:
                                        editor.putString("forecastmessage",responseText);//向sharedpreferences中存储
                                        editor.apply();
                                        showForecastWeatherInfo(responseText);
                                        break;
                                    case 2:
                                        editor.putString("comfortmessage",responseText);//向sharedpreferences中存储
                                        editor.apply();
                                        showComfortInfo(responseText);
                                        break;
                                    case 3:
                                        editor.putString("airmessage",responseText);//向sharedpreferences中存储
                                        editor.apply();
                                        showAirInfo(responseText);
                                        break;
                                    default:
                                }
                            }else{
                                Toast.makeText(WeatherActivity.this,"获取数据失败",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }

    //？？？以下函数未修改
    //展示当前实时天气数据
    private void showNowWeatherInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    String nowweathertext=jsonObject.getJSONObject("now").getString("text"); //用原生的jsonObject解析json数据

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //展示预报天气数据
    private void showForecastWeatherInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    String nowweathertext=jsonObject.getJSONObject("now").getString("text"); //用原生的jsonObject解析json数据

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //展示舒适度数据
    private void showComfortInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    String nowweathertext=jsonObject.getJSONObject("now").getString("text"); //用原生的jsonObject解析json数据

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //展示空气质量数据
    private void showAirInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    String nowweathertext=jsonObject.getJSONObject("now").getString("text"); //用原生的jsonObject解析json数据

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}