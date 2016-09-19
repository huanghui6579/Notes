package com.yunxinlink.notes.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yunxinlink.notes.R;

import retrofit2.Retrofit;

public class TestNetActivity extends AppCompatActivity {
    private static final String BASE_URL = "http://www.baidu.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_net);
    }
    
    public void testNet1(View view) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .build();
        
    }
}
