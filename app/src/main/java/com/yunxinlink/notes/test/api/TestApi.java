package com.yunxinlink.notes.test.api;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * @author huanghui1
 * @update 2016/9/20 9:26
 * @version: 0.0.1
 */
public interface TestApi {
    @GET("./")
    Call<ResponseBody> test1();

    @FormUrlEncoded
    @POST("login")
    Call<ResponseBody> login(@FieldMap Map<String, String> params);
}
