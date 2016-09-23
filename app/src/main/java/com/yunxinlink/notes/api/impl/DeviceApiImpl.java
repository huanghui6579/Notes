package com.yunxinlink.notes.api.impl;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.DeviceApi;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.DeviceInfo;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * @author huanghui1
 * @update 2016/9/20 14:57
 * @version: 0.0.1
 */
public class DeviceApiImpl extends BaseApi {
    private static final String TAG = "DeviceApiImpl";

    /**
     * 上传设备信息
     * @param deviceInfo 设备信息
     * @return
     */
    public static void activeDeviceInfo(DeviceInfo deviceInfo, final OnLoadCompletedListener<ActionResult<Void>> listener) {
        if (deviceInfo == null) {
            KLog.d(TAG, "active device info failed params is null");
            return;
        }
        Retrofit retrofit = buildRetrofit();
        DeviceApi repo = retrofit.create(DeviceApi.class);
        
        Map<String, String> params = new HashMap<>();
        params.put("imei", deviceInfo.getImei());
        params.put("os", deviceInfo.getOs());
        params.put("osVersion", deviceInfo.getOsVersion());
        params.put("phoneModel", deviceInfo.getPhoneModel());
        params.put("brand", deviceInfo.getBrand());
        
        Call<ActionResult<Void>> call = repo.activeDeviceInfo(params);
        call.enqueue(new Callback<ActionResult<Void>>() {
            @Override
            public void onResponse(Call<ActionResult<Void>> call, Response<ActionResult<Void>> response) {
                ActionResult<Void> actionResult = response.body();
                boolean success = false;
                int code = ActionResult.RESULT_FAILED;
                if (actionResult != null) {
                    code = actionResult.getResultCode();
                    if (actionResult.isSuccess()) { //成功
                        success = true;
                    }
                }
                
                if (listener != null) {
                    if (success) {
                        listener.onLoadSuccess(actionResult);
                    } else {
                        listener.onLoadFailed(code, null); 
                    }
                }
                KLog.d(TAG, "active device info result:" + response.body().toString());
            }

            @Override
            public void onFailure(Call<ActionResult<Void>> call, Throwable t) {
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_FAILED, t.getMessage());
                }
                KLog.d(TAG, "active device info error:" + t);
            }
        });
    }
}
