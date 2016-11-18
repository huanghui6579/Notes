package com.yunxinlink.notes.api.impl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.IDeviceApi;
import com.yunxinlink.notes.api.model.Platform;
import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.DeviceInfo;
import com.yunxinlink.notes.sync.download.DownloadListener;
import com.yunxinlink.notes.sync.download.DownloadTask;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * @author huanghui1
 * @update 2016/9/20 14:57
 * @version: 0.0.1
 */
public class DeviceApi extends BaseApi {
    private static final String TAG = "DeviceApi";

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
        IDeviceApi repo = retrofit.create(IDeviceApi.class);
        
        Map<String, String> params = new HashMap<>();
        String imei = deviceInfo.getImei();
        if (imei != null) {
            params.put("imei", imei);
        }
        params.put("os", deviceInfo.getOs());
        String osVersion = deviceInfo.getOsVersion();
        if (osVersion != null) {
            params.put("osVersion", osVersion);
        }
        String phoneModel = deviceInfo.getPhoneModel();
        if (phoneModel != null) {
            params.put("phoneModel", phoneModel);
        }
        String brand = deviceInfo.getBrand();
        if (brand != null) {
            params.put("brand", brand);
        }

        int versionCode = deviceInfo.getAppVersionCode();
        if (versionCode > 0) {
            params.put("appVersionCode", String.valueOf(versionCode));
        }
        
        String versionName = deviceInfo.getAppVersionName();
        
        if (versionName != null) {
            params.put("appVersionName", versionName);
        }
        
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
                KLog.d(TAG, "active device info result:" + actionResult);
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

    /**
     * 检查软件的新颁布,该方法在主线程中执行
     * @return 是否有新版本，true:有新版本
     */
    public static VersionInfo checkVersion(Context context) {
        PackageInfo packageInfo = SystemUtil.getPackageInfo(context);
        int versionCode = packageInfo.versionCode;
        String versionName = packageInfo.versionName;
        int platform = Platform.PLATFORM_ANDROID;
        
        Retrofit retrofit = buildRetrofit();
        IDeviceApi repo = retrofit.create(IDeviceApi.class);
        
        Map<String, String> map = new HashMap<>();
        map.put("platform", String.valueOf(platform));
        Call<ActionResult<VersionInfo>> call = repo.checkAppVersion(map);
        try {
            Response<ActionResult<VersionInfo>> response = call.execute();
            if (response == null || !response.isSuccessful() || response.body() == null) {
                KLog.d(TAG, "check app version failed response is null or not successful or body is null");
                return null;
            }
            ActionResult<VersionInfo> actionResult = response.body();
            if (!actionResult.isSuccess()) {
                KLog.d(TAG, "check app version failed action result is not success:" + actionResult);
                return null;
            }
            VersionInfo versionInfo = actionResult.getData();
            if (versionInfo == null || !versionInfo.checkContent()) {   //没有更新内容
                KLog.d(TAG, "check app version failed version info content is empty:" + versionInfo);
                return null;
            }
            int newVersionCode = versionInfo.getVersionCode();
            String newVersionName = versionInfo.getVersionName();
            if (newVersionCode >= versionCode && !newVersionName.equals(versionName)) { //有新版本
                KLog.d(TAG, "check app version result success and has new version:" + versionInfo);
                return versionInfo;
            } else {
                KLog.d(TAG, "check app version result success and has no new version");
                return null;
            }
        } catch (Exception e) {
            KLog.e(TAG, "check app version error:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下载APP软件
     * @param context
     * @param versionInfo
     * @param downloadListener 下载的监听器
     */
    public static Call<?> downloadApp(Context context, VersionInfo versionInfo, DownloadListener downloadListener) {
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.setTag(versionInfo);
        if (downloadListener != null) {
            downloadListener.onStart(downloadTask);
        }
        Retrofit retrofit = buildDownloadRetrofit(downloadListener);
        IDeviceApi repo = retrofit.create(IDeviceApi.class);
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(versionInfo.getId()));
        Call<ResponseBody> call = repo.downApp(map);
        boolean saveResult = false;
        try {
            Response<ResponseBody> response = call.execute();
            if (response == null || !response.isSuccessful() || response.body() == null) {
                KLog.d(TAG, "download app failed response is null or not successful");
            } else {
                Headers headers = response.headers();
                String filename = SystemUtil.getFilename(headers);
                if (TextUtils.isEmpty(filename)) {
                    filename = System.currentTimeMillis() + ".apk";
                }
                File saveFile = new File(SystemUtil.getAppDownloadPath(), filename);
                downloadTask.setSavePath(saveFile.getAbsolutePath());
                downloadTask.setFilename(filename);
                saveResult = FileUtil.writeResponseBodyToDisk(response.body(), saveFile);
                KLog.d(TAG, "download app success file:" + saveFile);
            }
        } catch (IOException e) {
            KLog.e(TAG, "download app file error:" + e.getMessage());
            e.printStackTrace();
        }
        if (downloadListener != null) {
            if (saveResult) {
                downloadListener.onCompleted(downloadTask);
            } else {
                downloadListener.onError(downloadTask);
            }
        }
        return call;
    }
    
}
