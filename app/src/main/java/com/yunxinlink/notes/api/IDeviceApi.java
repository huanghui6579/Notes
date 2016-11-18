package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.model.ActionResult;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;

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
    Call<ActionResult<Void>> activeDeviceInfo(@FieldMap Map<String, String> params);

    /**
     * 检查版本更新
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("device/app/check")
    Call<ActionResult<VersionInfo>> checkAppVersion(@FieldMap Map<String, String> params);

    /**
     * 下载APP的软件包
     * @param params 参数
     * @return
     */
    @GET("device/app/download")
    @Streaming
    Call<ResponseBody> downApp(@QueryMap Map<String, String> params);
}
