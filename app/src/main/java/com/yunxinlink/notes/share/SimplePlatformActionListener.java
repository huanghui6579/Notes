package com.yunxinlink.notes.share;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.wechat.friends.Wechat;

/**
 * 分享组件分享后的回调
 * @author huanghui1
 * @update 2016/8/18 16:43
 * @version: 0.0.1
 */
public class SimplePlatformActionListener implements PlatformActionListener {

    private static final String TAG = "SimplePlatformActionListener";
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String errorMsg = null;
            Context context = NoteApplication.getInstance();
            switch (msg.what) {
                case Constants.MSG_SUCCESS:
                    errorMsg = context.getString(R.string.share_success);
                    break;
                case Constants.MSG_FAILED:
                    errorMsg = (String) msg.obj;
                    if (errorMsg == null) {
                        errorMsg = context.getString(R.string.share_failed);
                    }
                    break;
            }
            if (errorMsg != null) {
                SystemUtil.makeShortToast(errorMsg);
            }
        }
    };
    
    @Override
    public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
        KLog.d(TAG, "---platform---onComplete-" + platform.getName() + "---action:" + action + "--map:" + hashMap);
        if (action == Platform.ACTION_SHARE) {   //分享类型
            mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
        }
    }

    @Override
    public void onError(Platform platform, int action, Throwable throwable) {

        Message msg = mHandler.obtainMessage();
        msg.what = Constants.MSG_FAILED;

        boolean hasClient = true;
        if (throwable != null && throwable instanceof ActivityNotFoundException) {
            hasClient = false;
        }

        String platformName = platform.getName();

        Context context = NoteApplication.getInstance();
        String appName = null;
        if (SinaWeibo.NAME.equals(platformName)) {    //新浪微博的分享
            if (hasClient) {
                switch (action) {
                    case Platform.ACTION_SHARE: //分享
                        String json = throwable == null ? "" : throwable.getMessage();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            String errorJson = jsonObject.optString("error", "");
                            if (!TextUtils.isEmpty(errorJson)) {
                                jsonObject = new JSONObject(errorJson);
                                int errorCode = jsonObject.optInt("error_code", 0);
                                if (errorCode == 20019) {   //短时间内，分享的内容重复
                                    msg.obj = context.getString(R.string.share_weibo_repeat);
                                }
                            }

                        } catch (JSONException e) {
                            KLog.e(e);
                            e.printStackTrace();
                        }
                        break;
                }
            } else {
                appName = context.getString(R.string.share_sina_weibo);
            }
        } else if (QQ.NAME.equals(platformName)) {
            if (hasClient) {
                //TODO 待填充
            } else {
                appName = context.getString(R.string.share_qq);
            }
        } else if (QZone.NAME.equals(platformName)) {
            if (hasClient) {
                //TODO 待填充
            } else {
                appName = context.getString(R.string.share_qq);
            }
        } else if (Wechat.NAME.equals(platformName)) {
            if (hasClient) {
                //TODO 待填充
            } else {
                appName = context.getString(R.string.app_wechat);
            }
        }

        if (appName != null) {  //no client
            msg.obj = context.getString(R.string.share_no_client, appName);
        }

        mHandler.sendMessage(msg);
        
        //java.lang.Throwable: {"status":400,"error":"{\"error\":\"repeat content!\",\"error_code\":20019,\"request\":\"\/2\/statuses\/update.json\"}"}
        
        KLog.d(TAG, "---platform----onError--SimplePlatformActionListener share error:" + throwable);
    }

    @Override
    public void onCancel(Platform platform, int action) {
        KLog.d(TAG, "---platform--onCancel--" + platform.getName() + "---action:" + action);
    }
}
