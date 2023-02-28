package com.example.xiaocaiweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.xiaocaiweather.WeatherActivity;
import com.example.xiaocaiweather.gson.Weather;
import com.example.xiaocaiweather.util.HttpUtil;
import com.example.xiaocaiweather.util.Utility;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    private final static int SAVE_NOWMESSAGE=0;
    private final static int SAVE_FORECASTMESSAGE=1;
    private final static int SAVE_COMFORTMESSAGE=2;
    private final static int SAVE_AIRMESSAGE=3;
    String mysecrettoken=""; //个人的（和风天气）访问token

    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather(); //8小时后AutoUpdate重新执行
        AlarmManager manager=(AlarmManager) getSystemService(ALARM_SERVICE);
//        int anHour=8*60*60*1000; //8小时的毫秒数
        int anMinute=60*1000; //1分钟
        long triggerAtTime= SystemClock.elapsedRealtime()+anMinute; //
        Intent i=new Intent(this,AutoUpdateService.class);
        PendingIntent pi=PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flags,startId);
    }

    //更新天气信息
    private void updateWeather(){
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherId=prefs.getString("weatherId",null);
        String NowWeatherUrl="https://devapi.qweather.com/v7/weather/now?location="+weatherId+"&key="+mysecrettoken;
        String ForeWeatherUrl="https://devapi.qweather.com/v7/weather/7d?location="+weatherId+"&key="+mysecrettoken;
        String ComfortUrl="https://devapi.qweather.com/v7/indices/1d?type=1,2,8&location="+weatherId+"&key="+mysecrettoken;
        String AirUrl="https://devapi.qweather.com/v7/air/now?location="+weatherId+"&key="+mysecrettoken;
        requestData(NowWeatherUrl,0);
        requestData(ForeWeatherUrl,1);
        requestData(ComfortUrl,2);
        requestData(AirUrl,3);
        Toast.makeText(this,"天气数据已自动更新",Toast.LENGTH_SHORT).show();
        System.out.println("天气数据已自动更新");
    }

    public void requestData(String requestUrl,int saveData){
        HttpUtil.sendOkHttpRequest(requestUrl, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseText=response.body().string();
                try {
                    JSONObject jsonObject=new JSONObject(responseText); //用原生的jsonObject解析json数据
                    String statuscode=jsonObject.getString("code"); //获取网络请求状态码
                    if(!responseText.isEmpty() && "200".equals(statuscode)){
                        SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        switch (saveData){
                            case 0:
                                editor.putString("nowmessage",responseText); //向sharedpreferences中存储nowmessage信息
                                editor.apply();
                                break;
                            case 1:
                                editor.putString("forecastmessage",responseText);//向sharedpreferences中存储
                                editor.apply();
                                break;
                            case 2:
                                editor.putString("comfortmessage",responseText);//向sharedpreferences中存储
                                editor.apply();
                                break;
                            case 3:
                                editor.putString("airmessage",responseText);//向sharedpreferences中存储
                                editor.apply();
                                break;
                            default:
                        }
                    }else{
                        Toast.makeText(AutoUpdateService.this,"获取数据失败",Toast.LENGTH_SHORT).show();
                        System.out.println("获取数据失败");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

}