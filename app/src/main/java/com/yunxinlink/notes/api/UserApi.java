package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.model.ActionResult;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;

/**
 * 用户信息的一些API接口
 * @author huanghui1
 * @update 2016/9/21 17:27
 * @version: 0.0.1
 */
public interface UserApi {
    /**
     * 用户登录
     * @param params
     * @return 返回登录后的结果，若登录成功，则返回了该用户的基本信息
     */
    Call<ActionResult<UserDto>> login(@FieldMap Map<String, String> params);
}
