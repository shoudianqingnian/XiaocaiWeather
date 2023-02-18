package com.example.xiaocaiweather;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.xiaocaiweather.db.City;
import com.example.xiaocaiweather.db.County;
import com.example.xiaocaiweather.db.Province;
import com.example.xiaocaiweather.util.HttpUtil;
import com.example.xiaocaiweather.util.Utility;

import org.jetbrains.annotations.NotNull;
import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    //控件
    private ProgressDialog progressDialog; //进度条控件
    private TextView titleText;
    private Button backButton;

    //ListView相关
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    //省市县列表
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince; //选中的省份
    private City selectedCity; //选中的城市
    private int currentLevel;//当前选中的级别

    //设置ListView的适配器
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    //给ListView和Button设置点击事件
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE)
                {
                    selectedProvince = provinceList.get(position);
                    queryCities(); //请求市级数据并更新到界面上
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    selectedCity = cityList.get(position);
                    queryCounties(); //请求县级数据并更新到界面上
                }
                else if(currentLevel == LEVEL_COUNTY)
                {
                    String weatherId = countyList.get(position).getWeatherId();

                    if(getActivity() instanceof MainActivity)
                    {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }
//                    else if(getActivity() instanceof WeatherActivity)
//                    {
//                        WeatherActivity activity = (WeatherActivity) getActivity();
//                        activity.drawerLayout.closeDrawers();
//                        activity.swipeRefresh.setRefreshing(true);
//                        activity.requestWeather(weatherId);
//                    }
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_COUNTY)
                {
                    queryCities(); //当前界面显示的是县级列表，因此需要返回到市级列表,请求市数据
                }
                else if (currentLevel == LEVEL_CITY)
                {
                    queryProvinces(); //当前界面显示的是市级列表，因此需要返回到省级列表,请求市数据
                }
            }
        });
        queryProvinces(); //Activity刚创建的时候，请求省级数据并更新到界面上
    }

    //查询全国所有的省，优先从本地数据库中查询，若没有则从服务器上查询
    private void queryProvinces()
    {
        titleText.setText("中国"); //将头布局的标题设置为中国
        backButton.setVisibility(View.GONE); //隐藏返回按钮
        provinceList = LitePal.findAll(Province.class); //调用Litepal查询窗口从本地数据库中读取省级数据
        if(provinceList.size() > 0)
        {
            dataList.clear();
            for (Province province:provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged(); //更新显示内容
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE; //更新显示标签
        }
        else //若本地数据库中没有省对应的数据，则从服务器上获取
        {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    //查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCities()
    {
        titleText.setText(selectedProvince.getProvinceName()); //设置标题内容
        backButton.setVisibility(View.VISIBLE); //显示返回按钮
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0)
        {
            dataList.clear();
            for (City city: cityList)
            {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged(); //更新显示内容
            listView.setSelection(0);
            currentLevel = LEVEL_CITY; //更新显示标签
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }
    }

    //查询选中市内所有的县，优先从本地数据库查询，如果没有查询到再去服务器上查询
    private void queryCounties()
    {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for (County county: countyList)
            {
                dataList.add(county.getCountyName());
            }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_COUNTY;
        }
        else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address,"county");
        }
    }

    //根据传入的地址和类型从服务器上查询省市县数据
    private void queryFromServer(String address, final String type)
    {
        showProgressDialog(); //显示进度条
        HttpUtil.sendOkHttpRequest(address, new Callback() { //向服务器发送请求
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) { //若下载失败
                //通过runOnUiThread()方法回到主线程逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog(); //关闭进度条
                        Toast.makeText(getContext(),"加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            //若下载成功，解析返回的响应，并存储到数据库中
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type))
                {
                    result = Utility.handleProvinceResponse(responseText);
                }
                else if("city".equals(type))
                {
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }
                else if("county".equals(type))
                {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if (result) //若数据解析成功，且存入数据库成功
                {
                    getActivity().runOnUiThread(new Runnable() { //切换到主UI线程
                        @Override
                        public void run() {
                            closeProgressDialog(); //关闭进度条
                            if("province".equals(type))
                            {
                                queryProvinces(); //重新加载数据
                            }
                            else if ("city".equals(type))
                            {
                                queryCities(); //重新加载数据
                            }
                            else if ("county".equals(type))
                            {
                                queryCounties(); //重新加载数据
                            }
                        }
                    });
                }

            }
        });
    }

    //显示进度对话框
    private void showProgressDialog()
    {
        if(progressDialog == null)
        {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载···");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话框
    private void closeProgressDialog()
    {
        if(progressDialog != null)
        {
            progressDialog.dismiss();
        }
    }
}
