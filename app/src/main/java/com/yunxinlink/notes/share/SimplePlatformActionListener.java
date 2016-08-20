package com.yunxinlink.notes.share;

import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.sina.weibo.SinaWeibo;

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
            int resId = 0;
            switch (msg.what) {
                case Constants.MSG_SUCCESS:
                    resId = R.string.share_success;
                    break;
                case Constants.MSG_FAILED:
                    resId = R.string.share_failed;
                    break;
            }
            if (resId != 0) {
                SystemUtil.makeShortToast(resId);
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
        
        switch (action) {
            case Platform.ACTION_SHARE: //分享
                if (SinaWeibo.NAME.equals(platform.getName())) {    //新浪微博的分享
                    String msg = throwable.getMessage();
                    try {
                        JSONObject jsonObject = new JSONObject(msg);
                        Object obj = jsonObject.opt("error");
                        Gson gson = new Gson();

                        JsonObject jsonObject1 = new JsonObject();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        
        if (action == Platform.ACTION_SHARE) {  //分享
            
        }
        
        mHandler.sendEmptyMessage(Constants.MSG_FAILED);
        
        //java.lang.Throwable: {"status":400,"error":"{\"error\":\"repeat content!\",\"error_code\":20019,\"request\":\"\/2\/statuses\/update.json\"}"}
        
        KLog.d(TAG, "---platform----onError--SimplePlatformActionListener share error:" + throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void onCancel(Platform platform, int action) {
        KLog.d(TAG, "---platform--onCancel--" + platform.getName() + "---action:" + action);
    }
}
