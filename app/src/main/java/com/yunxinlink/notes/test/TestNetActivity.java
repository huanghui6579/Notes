package com.yunxinlink.notes.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.test.api.TestApi;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestNetActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String BASE_URL = "http://10.100.80.138:8080/noteapi/";
    private static final String TAG = "TestNetActivity";
    
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_net);

        Button btnNet1 = (Button) findViewById(R.id.btn_net1);
        btnNet1.setOnClickListener(this);
        
        tvResult = (TextView) findViewById(R.id.tv_result);
    }
    
    public Retrofit buildRetrofit() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
    
    public void testLogin() {
        Retrofit retrofit = buildRetrofit();
        
        String mobile = "13211111111";
        String password = "123456";
        
        TestApi repo = retrofit.create(TestApi.class);
        Map<String, String> params = new HashMap<>();
        params.put("mobile", mobile);
        params.put("password", password);
        Call<ResponseBody> call = repo.login(params);
        tvResult.setText("");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                KLog.d(TAG, "onResponse call:" + call.toString());
                try {
                    String body = response.body().string();
                    tvResult.setText(body);
                    KLog.d(TAG, "onResponse response:" + body);
                    SystemUtil.makeShortToast("登录成功");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                KLog.d(TAG, "onFailure call:" + call);
                SystemUtil.makeShortToast("登录失败");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_net1:
                testLogin();
                break;
        }
    }
}
