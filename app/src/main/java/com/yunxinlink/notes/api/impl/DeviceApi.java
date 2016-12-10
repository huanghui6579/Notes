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
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
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
        setupParam(deviceInfo, params);
        
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
     * 设置上传的参数
     * @param params
     * @return
     */
    private static Map<String, String> setupParam(DeviceInfo deviceInfo, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
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
        return params;
    }

    /**
     * 设置带有附件的参数
     * @param deviceInfo
     * @param params
     * @return
     */
    private static Map<String, RequestBody> setupPartParam(DeviceInfo deviceInfo, Map<String, RequestBody> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        String imei = deviceInfo.getImei();
        if (imei != null) {
            params.put("imei", RequestBody.create(null, imei));
        }
        params.put("os", RequestBody.create(null, deviceInfo.getOs()));
        String osVersion = deviceInfo.getOsVersion();
        if (osVersion != null) {
            params.put("osVersion", RequestBody.create(null, osVersion));
        }
        String phoneModel = deviceInfo.getPhoneModel();
        if (phoneModel != null) {
            params.put("phoneModel", RequestBody.create(null, phoneModel));
        }
        String brand = deviceInfo.getBrand();
        if (brand != null) {
            params.put("brand", RequestBody.create(null, brand));
        }

        int versionCode = deviceInfo.getAppVersionCode();
        if (versionCode > 0) {
            params.put("appVersionCode", RequestBody.create(null, String.valueOf(versionCode)));
        }

        String versionName = deviceInfo.getAppVersionName();

        if (versionName != null) {
            params.put("appVersionName", RequestBody.create(null, versionName));
        }
        return params;
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
        }
        return null;
    }

    /**
     * 异步检查版本更新
     * @param context
     * @return
     */
    public static Call<?> checkVersionAsync(Context context, final OnLoadCompletedListener<VersionInfo> listener) {
        PackageInfo packageInfo = SystemUtil.getPackageInfo(context);
        final int versionCode = packageInfo.versionCode;
        final String versionName = packageInfo.versionName;
        int platform = Platform.PLATFORM_ANDROID;

        Retrofit retrofit = buildRetrofit();
        IDeviceApi repo = retrofit.create(IDeviceApi.class);

        Map<String, String> map = new HashMap<>();
        map.put("platform", String.valueOf(platform));
        Call<ActionResult<VersionInfo>> call = repo.checkAppVersion(map);

        call.enqueue(new Callback<ActionResult<VersionInfo>>() {
            @Override
            public void onResponse(Call<ActionResult<VersionInfo>> call, Response<ActionResult<VersionInfo>> response) {
                boolean hasNewVersion = false;
                VersionInfo versionInfo = null;
                if (response == null || !response.isSuccessful() || response.body() == null) {
                    KLog.d(TAG, "check app version failed response is null or not successful or body is null");
                } else {
                    ActionResult<VersionInfo> actionResult = response.body();
                    if (!actionResult.isSuccess()) {
                        KLog.d(TAG, "check app version failed action result is not success:" + actionResult);
                    } else {
                        versionInfo = actionResult.getData();
                        if (versionInfo == null || !versionInfo.checkContent()) {   //没有更新内容
                            KLog.d(TAG, "check app version failed version info content is empty:" + versionInfo);
                        } else {
                            int newVersionCode = versionInfo.getVersionCode();
                            String newVersionName = versionInfo.getVersionName();
                            if (newVersionCode >= versionCode && !newVersionName.equals(versionName)) { //有新版本
                                KLog.d(TAG, "check app version result success and has new version:" + versionInfo);
                                hasNewVersion = true;
                            } else {
                                KLog.d(TAG, "check app version result success and has no new version");
                            }
                        }
                    }
                }
                if (listener != null) {
                    if (hasNewVersion) {
                        listener.onLoadSuccess(versionInfo);
                    } else {
                        listener.onLoadSuccess(null);
                    }
                }

            }

            @Override
            public void onFailure(Call<ActionResult<VersionInfo>> call, Throwable t) {
                KLog.d(TAG, "check app version on failed:" + t);
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_FAILED, null);
                }
            }
        });
        return call;
    }

    /**
     * 下载APP软件
     * @param versionInfo
     * @param downloadListener 下载的监听器
     */
    public static Call<?> downloadApp(VersionInfo versionInfo, DownloadListener downloadListener) {
        File saveFile = null;
        //优先检测本地是否已经下载了该app，避免重复下载

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.setTag(versionInfo);

        Call<ResponseBody> call = initDownloadApp(versionInfo, downloadTask, downloadListener);
        if (call == null) {
            return null;
        }

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
                saveFile = new File(SystemUtil.getAppDownloadPath(), filename);
                downloadTask.setSavePath(saveFile.getAbsolutePath());
                downloadTask.setFilename(filename);
                saveResult = FileUtil.writeResponseBodyToDisk(response.body(), saveFile);
                KLog.d(TAG, "download app success file:" + saveFile);
            }
        } catch (Exception e) {
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


    /**
     * 上传日志信息
     * @param deviceInfo 设备信息
     * @param logFile 日志文件
     * @return
     */
    public static int reportBug(DeviceInfo deviceInfo, File logFile) {
        Retrofit retrofit = buildRetrofit();
        IDeviceApi repo = retrofit.create(IDeviceApi.class);
        Map<String, RequestBody> map = new HashMap<>();
        setupPartParam(deviceInfo, map);
        //添加文件
        if (logFile != null && logFile.exists()) {
            String filename = logFile.getName();
            String mime = FileUtil.getWebMime(logFile);
            if (mime == null) {
                mime = "text/plain";
            }
            RequestBody img = RequestBody.create(MediaType.parse(mime), logFile);
            //avatarFile: 与服务器端的参数名相同
            map.put("logFile\"; filename=\"" + filename + "", img);
        } else {
            KLog.d(TAG, "device api report bug log file is null or not exists:" + logFile);
        }
        int code = 0;
        Call<ActionResult<Void>> call = repo.reportBug(map);
        try {
            Response<ActionResult<Void>> response = call.execute();
            if (response == null || !response.isSuccessful()) { //http请求失败
                code = ActionResult.RESULT_FAILED;
                KLog.d(TAG, "device api report bug response is null or not successful");
                return code;
            }
            ActionResult<Void> actionResult = response.body();
            if (actionResult == null || !actionResult.isSuccess()) {
                code = actionResult == null ? ActionResult.RESULT_FAILED : actionResult.getResultCode();
                KLog.d(TAG, "device api report bug action result is null or not successful result:" + actionResult);
                return code;
            }
            code = ActionResult.RESULT_SUCCESS;
            KLog.d(TAG, "device api report bug result success:" + actionResult);
            //删除该文件，以防下次继续上报
            KLog.d(TAG, "device api report bug result success and will delete file:" + logFile);
            if (logFile != null && logFile.canWrite()) {
                logFile.delete();
            } else {
                KLog.d(TAG, "device api report bug delete file failed file is null or can not write:" + logFile);
            }
        } catch (Exception e) {
            code = ActionResult.RESULT_ERROR;
            KLog.e(TAG, "device api report bug error:" + e);
        }
        return code;
    }

    /**
     * 初始化下载器
     * @param versionInfo
     * @param downloadListener
     * @return 下载是线程
     */
    private static Call<ResponseBody> initDownloadApp(VersionInfo versionInfo, DownloadTask downloadTask, DownloadListener downloadListener) {
        if (downloadListener != null) {
            downloadListener.onStart(downloadTask);
        }
        File saveFile = null;
        //优先检测本地是否已经下载了该app，避免重复下载
        String filename = versionInfo.getFilename();
        if (!TextUtils.isEmpty(filename)) {
            saveFile = new File(SystemUtil.getAppDownloadPath(), filename);
            if (saveFile.canRead() && saveFile.exists()) {    //本地文件已存在
                //检验md5
                String localMd5 = DigestUtil.md5FileHex(saveFile);
                if (localMd5 != null && localMd5.equals(versionInfo.getHash())) {
                    //相同
                    KLog.d(TAG, "download app but already download to local not need download again");
                    downloadTask.setSavePath(saveFile.getAbsolutePath());
                    downloadTask.setFilename(filename);
                    if (downloadListener != null) {
                        downloadListener.onCompleted(downloadTask);
                    }
                    return null;
                } else {
                    //MD5不相同，则先删除原有的文件
                    KLog.d(TAG, "download app but already download to local but hash not the same delete file");
                    saveFile.delete();
                }

            }
        }

        Retrofit retrofit = buildDownloadRetrofit(downloadListener);
        IDeviceApi repo = retrofit.create(IDeviceApi.class);
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(versionInfo.getId()));
        return repo.downApp(map);
    }
}
