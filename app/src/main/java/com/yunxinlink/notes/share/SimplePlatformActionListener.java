package com.yunxinlink.notes.share;

import android.os.Handler;
import android.os.Message;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;

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
    public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
        KLog.d(TAG, "---platform--" + platform.getName() + "---i:" + i + "--map:" + hashMap);
        if (i == 9) {   //分享类型
            mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
        }
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {
        mHandler.sendEmptyMessage(Constants.MSG_FAILED);
        KLog.d(TAG, "SimplePlatformActionListener share error:" + throwable);
        throwable.printStackTrace();
    }

    @Override
    public void onCancel(Platform platform, int i) {

    }
}
