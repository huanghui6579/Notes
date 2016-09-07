package com.yunxinlink.notes.lock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author huanghui1
 * @update 2016/9/7 15:33
 * @version: 0.0.1
 */
public interface ILockerDelegate {

    /**
     * 跳转到输入密码的界面
     * @param activity
     * @param extra
     * @return
     */
    boolean startLockerActivity(Activity activity, Bundle extra);

    /**
     * 解锁的结果
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    int responseLockerActivityResult(Activity activity, int requestCode, int resultCode, Intent data);
}
