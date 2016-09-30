package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.model.ActionResult;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * 用户信息的一些API接口
 * @author huanghui1
 * @update 2016/9/21 17:27
 * @version: 0.0.1
 */
public interface IUserApi {
    /**
     * 用户登录
     * @param params 登录的参数
     * @return 返回登录后的结果，若登录成功，则返回了该用户的基本信息
     */
    @FormUrlEncoded
    @POST("user/login")
    Call<ActionResult<UserDto>> login(@FieldMap Map<String, String> params);

    /**
     * 用户注册
     * @param params 注册的参数
     * @return 服务器返回的结果
     */
    @FormUrlEncoded
    @POST("user/register")
    Call<ActionResult<UserDto>> register(@FieldMap Map<String, String> params);
}
