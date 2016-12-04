package com.yunxinlink.notes.lock;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.NoteUtil;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author huanghui1
 * @update 2016/9/7 15:31
 * @version: 0.0.1
 */
public class LockerDelegate implements ILockerDelegate, ILockerActivityDelegate {
    private static final String TAG = "LockerDelegate";
    
    private static final int MSG_LOCK_DELAY = 1;
    
    static final int REQUEST_CODE = 10001;

    private ILockerManager mLockerManager;
    
    private Method mMethodIsTopOfTask;

    private boolean mIsActivityRecreate = false;
    
    private int mLockerActivityState;

    private Handler mHandler;
    
    LockerDelegate(Context context) {
        mLockerManager = LockerManager.getInstance(context);
        mHandler = new Handler(Looper.getMainLooper(), new LockerActivityCallback());
        if (mMethodIsTopOfTask == null) {
            try {
                mMethodIsTopOfTask = Activity.class.getDeclaredMethod("isTopOfTask");
                if (mMethodIsTopOfTask != null) {
                    mMethodIsTopOfTask.setAccessible(true);
                } else {
                    KLog.d(TAG, "locker delegate init is top of task method is null may under Android 5.0");
                }
            } catch (NoSuchMethodException e) {
                KLog.e(TAG, "get isTopOfTask method error:" + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public static ILockerActivityDelegate getInstance(Context context) {
        return new LockerDelegate(context);
    }
    
    @Override
    public boolean startLockerActivity(Activity activity, Bundle extra) {
        mLockerActivityState = 0;
        if (activity == null) {
            KLog.d(TAG, "the activity is null when call startLockerActivity");
            return false;
        }
        Intent intent = activity.getIntent();
        intent = intent == null ? new Intent() : intent;
        KLog.d(TAG, "startLockerActivity intent:" + intent);
        boolean flag = false;
        if (mLockerManager != null && mLockerManager.isBeingLocked()) {
            LockAction lockAction = mLockerManager.acquireLockerActivityAction();
            flag = startLockerActivity(activity, extra, lockAction);
        }
        return flag;
    }
    
    private boolean startLockerActivity(Activity activity, Bundle extra, LockAction lockAction) {
        if (lockAction == null || TextUtils.isEmpty(lockAction.action) || lockAction.clazz == null) {
            return false;
        }
        boolean result = false;
        Intent intent = new Intent(lockAction.action, null, activity, lockAction.clazz);
        intent.putExtra(EXTRA_FLAG_LOCK, true);
        try {
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivityForResult(intent, REQUEST_CODE);
                activity.overridePendingTransition(0, 0);
                KLog.d(TAG, "start lock activity action:" + lockAction);
                result = true;
            }
        } catch (Exception e) {
            KLog.e(TAG, "start lock activity action:" + lockAction + ", and error:" + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int responseLockerActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        int result = 0;
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) { //解锁成功
                mLockerManager.unlock();
                result = 1;
            } else if (resultCode == Activity.RESULT_CANCELED) {
                //是否是用户手动点的返回键而取消的
                boolean isBackPressed = false;
                if (data != null) {
                    isBackPressed = data.getBooleanExtra(EXTRA_FLAG_IS_BACK_PRESSED, false);
                }
                KLog.d(TAG, "lock result code is cancel isBackPressed:" + isBackPressed);
                if (isBackPressed) {    //返回，解锁取消
                    result = -1;
                    onResultValidateFailed(activity);
                }
            } else if (resultCode == Activity.RESULT_FIRST_USER + 2) {
                result = 2;
            } else if (resultCode == Activity.RESULT_FIRST_USER + 1) {
                if (mIsActivityRecreate) {
                    mIsActivityRecreate = false;
                    result = 3;
                }
            }
        }
        return result;
    }

    @Override
    public void onCreate(Activity activity, Bundle extra) {
        KLog.d(TAG, "LockerDelegate onCreate");
        if (extra != null) {
            mIsActivityRecreate = extra.getBoolean(EXTRA_FLAG_IS_ACTIVITY_RECREATE, false);
            KLog.d(TAG, "LockerDelegate is activity recreated : " + mIsActivityRecreate);
            if (mIsActivityRecreate) {
                return;
            }
        }
        if (/*mLockerActivityState == 0 && */mLockerManager.isBeingLocked()) {
            if (needLockActivity(extra)) {
                KLog.d(TAG, "onCreate need lock activity");
                doStartLockActivity(activity,extra);
            } else {    //不需要锁定界面
                KLog.d(TAG, "onCreate don't need lock activity");
//                mLockerActivityState = 6;
            }
        }
    }

    @Override
    public void onRestart(Activity activity, Bundle extra) {
        KLog.d(TAG, "LockerDelegate onRestart");
        if (mLockerManager != null && mLockerManager.isBeingLocked()) {
            if (!isActivityTopOfTask(activity)) {   //当前界面不在最顶部
                mLockerActivityState = 3;
                return;
            }
            boolean shouldDelay = false;
            if (extra != null) {
                shouldDelay = extra.getBoolean(EXTRA_BOOLEAN_SHOULD_START_LOCK_DELAY, false);
            }
            if (shouldDelay) {
                doStartLockActivityDelay(activity, extra);  //延迟锁定
            } else {
                doStartLockActivity(activity,extra);
            }

        }
        /*if (mLockerActivityState == 0) {
            if (mLockerManager != null && mLockerManager.isBeingLocked()) {
                if (!isActivityTopOfTask(activity)) {   //当前界面不在最顶部
                    mLockerActivityState = 3;
                    return;
                }
                boolean shouldDelay = false;
                if (extra != null) {
                    shouldDelay = extra.getBoolean(EXTRA_BOOLEAN_SHOULD_START_LOCK_DELAY, false);
                }
                if (shouldDelay) {
                    doStartLockActivityDelay(activity, extra);  //延迟锁定
                } else {
                    doStartLockActivity(activity,extra);
                }
                
            }
        } else {
            if (mLockerActivityState == 10) {
                SystemClock.sleep(200);
            }
            mLockerActivityState = 0;
        }*/
    }

    /**
     * 立即锁定
     * @param activity
     * @param extra
     */
    private boolean doStartLockActivity(Activity activity, Bundle extra) {
        boolean result = startLockerActivity(activity, extra);
        KLog.d(TAG, "doStartLockActivity result:" + result);
//        if (result) {   //已加锁
//            mLockerActivityState = 1;
//        }
        return result;
    }

    /**
     * 延迟锁定
     * @param activity
     * @param extra
     */
    private void doStartLockActivityDelay(Activity activity, Bundle extra) {
        startLockActivityDelay(activity, extra);
        KLog.d(TAG, "doStartLockActivityDelay ");
//        mLockerActivityState = 5;   //延迟锁定
    }
    
    private void startLockActivityDelay(Activity activity, Bundle extra) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_LOCK_DELAY;
        msg.obj = activity;
        mHandler.sendMessageDelayed(msg, 200);
    }

    @Override
    public void onResume(Activity activity, Bundle extra) {
        KLog.d(TAG, "LockerDelegate onResume");
        if (mLockerActivityState == 3) {    //之前不在最顶部，则现在锁定
            KLog.d(TAG, "onResume start lock activity");
            doStartLockActivity(activity,extra);
        } else { //不需要锁定
            mLockerActivityState = 0;
        }
    }

    @Override
    public void onNewIntent(Activity activity, Bundle extra) {
        KLog.d(TAG, "LockerDelegate onNewIntent");
        if (/*mLockerActivityState == 0 && */mLockerManager.isBeingLocked()) {
            if (needLockActivity(extra)) {
                if (mIsActivityRecreate) {
                    doStartLockActivityDelay(activity, extra);  //延迟锁定
                } else {
                    doStartLockActivity(activity, extra);   //立即锁定
                }
            } else {    //不需要锁定
                KLog.d(TAG, "onNewIntent don't need lock activity");
//                mLockerActivityState = 6;
            }
        }
    }

    @Override
    public boolean onPageSelected(Activity activity, Bundle extra) {
        KLog.d(TAG, "LockerDelegate onPageSelected");
        return doStartLockActivity(activity, extra);
        /*if (mLockerActivityState == 0) {
           return doStartLockActivity(activity, extra);
        }
        return false;*/
    }

    @Override
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        KLog.d(TAG, "LockerDelegate onActivityResult");
        int result = responseLockerActivityResult(activity, requestCode, resultCode, data);
        /*switch (result) {
            case 1: //解锁成功
            case -1:    //解锁取消，用户主动返回
                mLockerActivityState = 2;
                break;
            case 2: //因为锁屏, 密码验证界面关闭
                break;
            case 3:
                break;
            default:
                mLockerActivityState = 0;
                break;
        }*/
        return result != 0;
    }

    @Override
    public void onDestroy(Activity activity, Bundle extra) {
        mLockerManager.lock();
    }

    @Override
    public void updateLockInfo(LockInfo lockInfo) {
        mLockerManager.setLockInfo(lockInfo);
    }

    @Override
    public boolean isLocked() {
        return mLockerManager.isBeingLocked();
    }

    @Override
    public void setLockState(boolean isLocking) {
        if (isLocking) {
            mLockerManager.lock();
        } else {
            mLockerManager.unlock();
        }
    }

    private void onResultValidateFailed(Activity activity) {
        NoteUtil.finishAll(activity);
    }

    private boolean isActivityTopOfTask(Activity a) {
        boolean isTop = true;
        try {
            if(mMethodIsTopOfTask != null) {    //Android 5.0或者之上
                Object value  = mMethodIsTopOfTask.invoke(a, (Object[])null);
                if (value != null && value instanceof Boolean) {
                    isTop = (boolean) value;
                }
            } else {    //Android 5.0之下
                ActivityManager manager = ((ActivityManager) a.getSystemService(Context.ACTIVITY_SERVICE));
                List localList = manager.getRunningTasks(1);
                ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo)localList.get(0);
                String topActivityName = localRunningTaskInfo.topActivity.getClassName();
                isTop = a.getClass().getName().equals(topActivityName);
            }
        } catch (Exception e) {
            KLog.e(TAG, "isActivityTopOfTask error:" + e.getMessage());
            e.printStackTrace();
        }

        return isTop;
    }

    /**
     * 是否需要锁定界面
     * @param extra
     * @return
     */
    private boolean needLockActivity(Bundle extra) {
        if (extra != null) {
            return extra.getBoolean(EXTRA_BOOLEAN_NEED_LOCK_ACTIVITY, true);
        }
        return false;
    }
    
    class LockerActivityCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOCK_DELAY:    //延迟加锁
                    if (msg.obj != null && msg.obj instanceof Activity) {
                        Activity activity = (Activity) msg.obj;
                        doStartLockActivity(activity, null);
                    }
                    break;
            }
            return false;
        }
    }
}
