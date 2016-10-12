package com.yunxinlink.notes.api;

import com.yunxinlink.notes.model.ActionResult;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * 设备的基本API，包括设备基本信息的上报等
 * @author huanghui1
 * @update 2016/9/20 14:36
 * @version: 0.0.1
 */
public interface IDeviceApi {
    /**
     * 激活设备，首次使用此应用时，上传设备的基本信息
     * @param params 上传的参数
     * @return
     */
    @FormUrlEncoded
    @POST("device/activate")
    Call<ActionResult<Void>> activeDeviceInfo(@FieldMap Map<String, String>params);
}
