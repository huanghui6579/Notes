package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.model.ActionResult;

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
    @POST("api/device/activate")
    Call<ActionResult<Void>> activeDeviceInfo(@FieldMap Map<String, String> params);

    /**
     * 检查版本更新
     * @param params
     * @return
     */
    @FormUrlEncoded
    @POST("app/check")
    Call<ActionResult<VersionInfo>> checkAppVersion(@FieldMap Map<String, String> params);

    /**
     * 下载APP的软件包
     * @param params 参数
     * @return
     */
    @GET("app/download")
    @Streaming
    Call<ResponseBody> downApp(@QueryMap Map<String, String> params);

    /**
     * 上报异常的日志文件
     * @param params
     * @return
     */
    @Multipart
    @POST("api/device/bug/report")
    Call<ActionResult<Void>> reportBug(@PartMap Map<String, RequestBody> params);
}
