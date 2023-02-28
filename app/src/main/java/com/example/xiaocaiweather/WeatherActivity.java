package com.example.xiaocaiweather;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

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

    public SwipeRefreshLayout swipeRefreshLayout; //刷新框

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
        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String nowMessage=prefs.getString("nowmessage",null);  //实时天气类信息
        String forecastMessage=prefs.getString("forecastmessage",null); //7天预报信息
        String comfortMessage=prefs.getString("comfortmessage",null); //天气指数信息
        String airMessage=prefs.getString("airmessage",null); //空气质量信息
        String weatherId=prefs.getString("weatherId",null);
        String countyName=prefs.getString("countyname",null); //县名
        String mysecrettoken="你的token"; //个人的（和风天气）访问token
//        String weatherId=getIntent().getStringExtra("weather_id"); //获取weatherid
        weatherLayout.setVisibility(View.INVISIBLE); //设置滚动界面不可见
        System.out.println("测试");
        System.out.println(weatherId);
        //判断数据是否有缺失
        if(nowMessage==null){//处理实时天气数据
            String requestUrl="https://devapi.qweather.com/v7/weather/now?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,0); //请求城市 当前实时天气信息
        }
        else{
            showNowWeatherInfo(nowMessage);//有缓存时直接展示实时天气数据
        }
        if(forecastMessage==null){ //处理预报天气数据
            String requestUrl="https://devapi.qweather.com/v7/weather/7d?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,1); //请求城市 7天的预报天气信息
        }
        else{
            showForecastWeatherInfo(forecastMessage);//有缓存时直接展示预报天气数据
        }
        if(comfortMessage==null){//处理舒适度数据
            String requestUrl="https://devapi.qweather.com/v7/indices/1d?type=1,2,8&location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,2); //请求城市 当前1天内的舒适度信息
        }
        else{
            showComfortInfo(comfortMessage);//有缓存时直接展示舒适度数据
        }
        if(airMessage==null){//处理空气质量数据
            String requestUrl="https://devapi.qweather.com/v7/air/now?location="+weatherId+"&key="+mysecrettoken; //设置请求数据的URL
            requestWeather(requestUrl,3); //请求城市 当前空气质量信息
        }
        else{
            showAirInfo(airMessage);//有缓存时直接展示空气质量数据
        }
        showCountyName(countyName); //展示县名数据
        weatherLayout.setVisibility(View.VISIBLE); //设置滚动界面可见

        //实现刷新框下拉刷新数据的功能逻辑
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather("https://devapi.qweather.com/v7/weather/now?location="+weatherId+"&key="+mysecrettoken,0);
                requestWeather("https://devapi.qweather.com/v7/weather/7d?location="+weatherId+"&key="+mysecrettoken,1);
                requestWeather("https://devapi.qweather.com/v7/indices/1d?type=1,2,8&location="+weatherId+"&key="+mysecrettoken,2);
                requestWeather("https://devapi.qweather.com/v7/air/now?location="+weatherId+"&key="+mysecrettoken,3);
                Toast.makeText(WeatherActivity.this,"天气数据已更新到最新",Toast.LENGTH_SHORT).show(); //显示天气数据更新的提示
            }
        });
    }

    //根据url请求城市天气信息
    public void requestWeather(String requestUrl,int saveData){
        HttpUtil.sendOkHttpRequest(requestUrl, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("获取数据失败");
                        Toast.makeText(WeatherActivity.this,"获取数据失败",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);//刷新实践结束，隐藏刷新进度条
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
                                System.out.println("获取数据失败");
                            }
                            swipeRefreshLayout.setRefreshing(false);//刷新实践结束，隐藏刷新进度条
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
    }

//    //根据weatherID请求查询存储城市姓名信息
//    public void requestCountyName(String weatherId,String mysecrettoken){
//        String requestNameUrl="https://geoapi.qweather.com/v2/city/lookup?location="+weatherId+"&key="+mysecrettoken;
//        HttpUtil.sendOkHttpRequest(requestNameUrl, new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                e.printStackTrace();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        System.out.println("获取城市姓名数据失败");
//                        Toast.makeText(WeatherActivity.this,"获取城市姓名数据失败",Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                final String nameResponseText=response.body().string();
//
//                try{
//                    JSONObject jsonObject=new JSONObject(nameResponseText); //用原生的jsonObject解析json数据
//                    String namestatuscode=jsonObject.getString("code"); //获取网络请求状态码
//                    if(!nameResponseText.isEmpty() && "200".equals(namestatuscode)){
//                        String countyname=jsonObject.getJSONArray("location").getJSONObject(0).getString("name");
//                        SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
//                        editor.putString("countyname",countyname);
//                        editor.apply();
//                        showCountyName(countyname);
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        });
//    }

    //展示当前城市姓名
    private void showCountyName(String countyname){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                titleCity.setText(countyname);
            }
        });
    }

    //展示当前实时天气数据
    private void showNowWeatherInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    String updateTime=jsonObject.getString("updateTime").split("\\+")[1]; //更新时间？？？格式问题未解决
                    System.out.println(updateTime);
                    String degree=jsonObject.getJSONObject("now").getString("temp")+"℃"; //当前温度
                    String weatherInfo=jsonObject.getJSONObject("now").getString("text"); //当前天气
                    titleUpdateTime.setText(updateTime);
                    degreeText.setText(degree);
                    weatherInfoText.setText(weatherInfo);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //展示预报天气数据：1个大难点（jsonArray）
    private void showForecastWeatherInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    JSONArray foreArrsy=jsonObject.getJSONArray("daily");
                    forecastLayout.removeAllViews();
                    for(int i=0;i<foreArrsy.length();i++){
                        JSONObject thisdayObject=foreArrsy.getJSONObject(i);
//                        System.out.println(thisdayObject.getString("fxDate"));
//                        System.out.println(thisdayObject.getString("textDay"));
//                        System.out.println(thisdayObject.getString("tempMax"));
//                        System.out.println(thisdayObject.getString("tempMin"));
                        View view= LayoutInflater.from(WeatherActivity.this).inflate(R.layout.forecast_item,forecastLayout,false); //???
                        //为天气预报界面的UI控件绑定id
                        TextView dateText=(TextView) findViewById(R.id.date_text);
                        TextView infoText=(TextView) findViewById(R.id.info_text);
                        TextView maxText=(TextView) findViewById(R.id.max_text);
                        TextView minText=(TextView) findViewById(R.id.min_text);
                        dateText.setText(thisdayObject.getString("fxDate"));//预报日期
                        infoText.setText(thisdayObject.getString("textDay"));//预报天气
                        maxText.setText(thisdayObject.getString("tempMax")); //预报最高温度
                        minText.setText(thisdayObject.getString("tempMin"));//预报最低温度
                        forecastLayout.addView(view);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    //展示舒适度数据:1个难点
    private void showComfortInfo(String responsemessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject=new JSONObject(responsemessage); //用原生的jsonObject解析json数据
                    String sporttext="运动建议："+jsonObject.getJSONArray("daily").getJSONObject(0).getString("text"); //运动建议
                    String carwashtext="洗车指数："+jsonObject.getJSONArray("daily").getJSONObject(1).getString("text"); //洗车建议
                    String comforttext="舒适度："+jsonObject.getJSONArray("daily").getJSONObject(2).getString("text"); //体感舒适度
                    comfortText.setText(comforttext);
                    carWashText.setText(carwashtext);
                    sportText.setText(sporttext);
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
                    aqiText.setText(jsonObject.getJSONObject("now").getString("aqi"));
                    pm25Text.setText(jsonObject.getJSONObject("now").getString("pm2p5"));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
}