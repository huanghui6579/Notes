package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.User;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

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
    @POST("api/user/login")
    Call<ActionResult<UserDto>> login(@FieldMap Map<String, String> params);

    /**
     * 用户注册
     * @param params 注册的参数
     * @return 服务器返回的结果
     */
    @FormUrlEncoded
    @POST("api/user/register")
    Call<ActionResult<UserDto>> register(@FieldMap Map<String, String> params);

    /**
     * 用户绑定，若服务器存在该用，则视为登录，如果不存在，则视为注册
     * @param params 绑定的参数
     * @return 服务器返回的结果
     */
    @FormUrlEncoded
    @POST("api/user/bind")
    Call<ActionResult<UserDto>> bind(@FieldMap Map<String, String> params);

    /**
     * 修改用户信息，将本地的用户信息上传到服务器
     * @param sid 用户的sid,唯一值，有服务器生成
     * @param params 上传的一些参数
     * @return 服务器的返回结果
     */
    @Multipart
    @POST("api/user/{sid}/modify")
    Call<ActionResult<Void>> modify(@Path("sid") String sid, @PartMap Map<String, RequestBody> params);

    /**
     * 仅仅提交基本信息，不包含头像
     * @param sid
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("api/user/{sid}/modify")
    Call<ActionResult<Void>> modifyBasic(@Path("sid") String sid, @FieldMap Map<String, String> params);

    /**
     * 从服务器上获取对应用户的基本信息
     * @param sid 用户的sid,唯一值，有服务器生成
     * @return
     */
    @GET("api/user/{sid}/info")
    Call<ActionResult<User>> downInfo(@Path("sid") String sid);

    /**
     * 下载用户的头像
     * @param sid 用户的sid,唯一值，有服务器生成
     * @return
     */
    @GET("api/user/{sid}/avatar")
    @Streaming
    Call<ResponseBody> downAvatar(@Path("sid") String sid);

    /**
     * 发送重置密码的邮件
     * @return
     */
    @POST("api/user/{account}/forget")
    Call<ActionResult<Void>> resetPassword(@Path("account") String account);

    /**
     * 用户校验，主要用于重置应用锁密码
     * @param params 校验的参数
     * @return 返回校验的结果，若校验成功，则返回了该用户的基本信息
     */
    @FormUrlEncoded
    @POST("api/user/validate")
    Call<ActionResult<UserDto>> validate(@FieldMap Map<String, String> params);

    /**
     * 修改用户密码，需要校验原始密码
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("api/user/{sid}/pwd/modify")
    Call<ActionResult<Void>> modifyPassword(@Path("sid") String sid, @FieldMap Map<String, String> params);
}
