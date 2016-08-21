package com.yunxinlink.notes.share;

import android.view.View;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.NoteUtil;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.tencent.qzone.QZoneWebShareAdapter;

/**
 * 自定义的授权界面
 * @author huanghui1
 * @update 2016/8/19 20:04
 * @version: 0.0.1
 */
public class NoteQZoneWebShareAdapter extends QZoneWebShareAdapter implements View.OnClickListener, PlatformActionListener {

    private PlatformActionListener mBackListener;

    @Override
    public void onCreate() {
        super.onCreate();

        //自定义标题栏
        NoteUtil.initTitleView(getActivity(), getTitleLayout());

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
        KLog.d("---onComplete--platform---" + platform.getName() + "----action:" + action);
    }

    @Override
    public void onError(Platform platform, int action, Throwable throwable) {
        KLog.d("---onError--platform---" + platform.getName() + "----action:" + action + "----error:" + throwable);
    }

    @Override
    public void onCancel(Platform platform, int action) {
        KLog.d("---onCancel--platform---" + platform.getName() + "----action:" + action);
    }
}
