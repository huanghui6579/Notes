package com.yunxinlink.notes.lock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * @author huanghui1
 * @update 2016/9/7 15:35
 * @version: 0.0.1
 */
public interface ILockerActivityDelegate {
    String EXTRA_INTEGER_LAUNCH_ACTIVIY_DELAY = "extra.locker.activity.delay";

    String EXTRA_BOOLEAN_SHOULD_START_LOCK_DELAY = "extra.boolean.should.delay";

    String EXTRA_BOOLEAN_NEED_LOCK_ACTIVITY = "extra.boolean.need.lock";
    
    String EXTRA_FLAG_LOCK = "extra.flag.lock";
    String EXTRA_FLAG_IS_BACK_PRESSED = "extra.flag.back";
    String EXTRA_FLAG_IS_ACTIVTY_RECREATE = "extra.flag.activity_recreate";

    void onCreate(Activity activity, Bundle extra);

    void onRestart(Activity activity, Bundle extra);

    void onResume(Activity activity, Bundle extra);

    void onNewIntent(Activity activity, Bundle extra);

    boolean onPageSelected(Activity activity, Bundle extra);

    boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data);
}
