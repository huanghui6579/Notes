package com.yunxinlink.notes.lock.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.ILockerActivityDelegate;
import com.yunxinlink.notes.lockpattern.utils.AlpSettings;
import com.yunxinlink.notes.lockpattern.utils.Encrypter;
import com.yunxinlink.notes.lockpattern.utils.InvalidEncrypterException;
import com.yunxinlink.notes.lockpattern.utils.LoadingView;
import com.yunxinlink.notes.lockpattern.utils.UI;
import com.yunxinlink.notes.lockpattern.widget.LockDigitalView;
import com.yunxinlink.notes.lockpattern.widget.LockPatternUtils;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.List;

import haibison.android.underdogs.Api;
import haibison.android.underdogs.Authors;
import haibison.android.underdogs.NonNull;
import haibison.android.underdogs.Nullable;
import haibison.android.underdogs.Param;
import haibison.android.underdogs.StringRes;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.yunxinlink.notes.lockpattern.utils.AlpSettings.Display.METADATA_MAX_RETRIES;
import static com.yunxinlink.notes.lockpattern.utils.AlpSettings.Security.METADATA_AUTO_SAVE_PATTERN;
import static com.yunxinlink.notes.lockpattern.utils.AlpSettings.Security.METADATA_ENCRYPTER_CLASS;

public class LockDigitalActivity extends BaseActivity implements LockDigitalView.OnInputChangedListener {
    
    private static final String CLASSNAME = LockDigitalActivity.class.getSimpleName();
    
    /**
     * Use this action to create new pattern. You can provide an {@link Encrypter} with {@link
     * AlpSettings.Security#setEncrypterClass(android.content.Context, Class)} to improve security.
     * <p>
     * If the user created a pattern, {@link #RESULT_OK} returns with the pattern ({@link #EXTRA_PATTERN}). Otherwise {@link #RESULT_CANCELED}
     * returns.
     *
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @since v2.4 beta
     */
    public static final String ACTION_CREATE_PATTERN = CLASSNAME + ".CREATE_PATTERN";

    /**
     * Use this action to compare pattern. You provide the pattern to be compared with {@link #EXTRA_PATTERN}.
     * <p>
     * If you enabled feature auto-save pattern before (with {@link AlpSettings.Security#setAutoSavePattern(Context, boolean)}), then you don't need {@link
     * #EXTRA_PATTERN} at this time. But if you use this extra, its priority is <em>higher</em> than the one stored in shared preferences.
     * <p>
     * You can use {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN} to help your users in case they forgot the patterns.
     * <p>
     * If the user passes, {@link #RESULT_OK} returns. If not, {@link #RESULT_FAILED} returns.
     * <p>
     * If the user cancels the task, {@link #RESULT_CANCELED} returns.
     * <p>
     * In any case, extra {@link #EXTRA_RETRY_COUNT} will always be available in the intent result.
     *
     * @see #EXTRA_PATTERN
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @see #RESULT_FAILED
     * @see #EXTRA_RETRY_COUNT
     * @since v2.4 beta
     */
    public static final String ACTION_COMPARE_PATTERN = CLASSNAME + ".COMPARE_PATTERN";

    /**
     * Put a {@link PendingIntent} into this key. It will be sent before {@link #RESULT_OK} will be returning. If you were calling this activity
     * with {@link #ACTION_CREATE_PATTERN}, key {@link #EXTRA_PATTERN} will be attached to the original intent which the pending intent holds.
     * <p>
     * <h1>Notes</h1>
     * <p>
     * <ul>
     *     <li>If you're going to use an activity, you don't need {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library will
     *     call it inside {@link LockPatternActivity} .</li>
     * </ul>
     */
    @Param(type = Param.Type.INPUT, dataTypes = PendingIntent.class)
    public static final String EXTRA_PENDING_INTENT_OK = CLASSNAME + ".PENDING_INTENT_OK";

    /**
     * Put a {@link PendingIntent} into this key. It will be sent before {@link #RESULT_CANCELED} will be returning.
     * <p>
     * <h1>Notes</h1>
     * <p>
     * <ul>
     *     <li>If you're going to use an activity, you don't need {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library will
     *     call it inside {@link LockPatternActivity} .</li>
     * </ul>
     */
    @Param(type = Param.Type.INPUT, dataTypes = PendingIntent.class)
    public static final String EXTRA_PENDING_INTENT_CANCELLED = CLASSNAME + ".PENDING_INTENT_CANCELLED";

    /**
     * Key to hold the pattern.
     * <ul>
     *     <li>If you use encrypter, it should be an encrypted array.</li>
     *
     *     <li>If you don't use encrypter, it should be the SHA-1 value of the actual pattern. You can generate the value by {@link
     *     LockPatternUtils#patternToSha1(List)}.</li>
     * </ul>
     *
     * @since v2 beta
     */
    @Param(type = Param.Type.IN_OUT, dataTypes = char[].class)
    public static final String EXTRA_PATTERN = CLASSNAME + ".PATTERN";

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user fails to "login" after a number of tries, this activity will finish with this
     * result code.
     *
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     */
    public static final int RESULT_FAILED = RESULT_FIRST_USER + 1;

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user forgot his/ her pattern and decided to ask for your help with recovering the
     * pattern ({@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN}), this activity will finish with this result code.
     *
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     * @see #EXTRA_PENDING_INTENT_FORGOT_PATTERN
     * @since v2.8 beta
     */
    public static final int RESULT_FORGOT_PATTERN = RESULT_FIRST_USER + 2;

    /**
     * For actions {@link #ACTION_COMPARE_PATTERN}, this key holds the number of tries that the user
     * attempted to verify the input pattern.
     */
    @Param(type = Param.Type.OUTPUT, dataTypes = int.class)
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".RETRY_COUNT";

    /**
     * For actions {@link #ACTION_COMPARE_PATTERN}, this key holds the max number of tries that the user
     * attempted to verify the input pattern.
     */
    @Param(type = Param.Type.OUTPUT, dataTypes = int.class)
    public static final String EXTRA_RETRY_MAX_COUNT = CLASSNAME + ".RETRY_MAX_COUNT";

    /**
     * You put a {@link PendingIntent} into this extra. The library will show a button <kbd>"Forgot pattern?"</kbd> and call your intent later
     * when the user taps it.
     * <p>
     * <h1>Notes</h1>
     * <p>
     * <ul>
     *     <li>If you use an activity, you don't need {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library will call it
     *     inside {@link LockPatternActivity}.</li>
     *
     *     <li>{@link LockPatternActivity} will finish with {@link #RESULT_FORGOT_PATTERN} <em><strong>after</strong> making a call</em> to
     *     start your pending intent.</li>
     *
     *     <li>It is your responsibility to make sure the intent is good. The library doesn't cover any errors when calling your intent.</li>
     * </ul>
     *
     * @see #ACTION_COMPARE_PATTERN
     * @since v2.8 beta
     */
    @Authors(names = "Yan Cheng Cheok")
    @Param(type = Param.Type.INPUT, dataTypes = PendingIntent.class)
    public static final String EXTRA_PENDING_INTENT_FORGOT_PATTERN = CLASSNAME + ".PENDING_INTENT_FORGOT_PATTERN";

    /**
     * Use this extra to provide title for the activity.
     */
    @Param(type = Param.Type.INPUT, dataTypes = { int.class, CharSequence.class })
    public static final String EXTRA_TITLE = CLASSNAME + ".TITLE";

    /**
     * Use this extra to provide text for the info text view.
     */
    @Param(type = Param.Type.INPUT, dataTypes = { int.class })
    public static final String EXTRA_TEXT_INFO = CLASSNAME + ".TEXT_INFO";

    /**
     * Use this extra to identify is is modify .
     */
    @Param(type = Param.Type.INPUT, dataTypes = { boolean.class })
    public static final String EXTRA_IS_MODIFY = CLASSNAME + ".IS_MODIFY";

    /**
     * Use this extra to controller lock.
     */
    @Param(type = Param.Type.INPUT, dataTypes = { boolean.class })
    public static final String EXTRA_HAS_LOCK_CONTROLLER = CLASSNAME + ".HAS_LOCK_CONTROLLER";

    /**
     * Helper enum for button OK commands. (Because we use only one "OK" button for different commands).
     */
    private enum ButtonOkCommand { CONTINUE, FORGOT_PATTERN, DONE }

    /**
     * Delay time to reload the lock pattern view after a wrong pattern.
     */
    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = SECOND_IN_MILLIS;

    /**
     * You can provide an {@link ResultReceiver} with this key. The activity will notify your receiver the same result code and intent data as
     * you will receive them in {@link #onActivityResult(int, int, Intent)}.
     *
     * @since v2.4 beta
     */
    @Param(type = Param.Type.INPUT, dataTypes = ResultReceiver.class)
    public static final String EXTRA_RESULT_RECEIVER = CLASSNAME + ".RESULT_RECEIVER";

    private static final String[] SUPPORTED_ACTIONS = { ACTION_CREATE_PATTERN, ACTION_COMPARE_PATTERN };

    /**
     * 4位密码
     */
    private static final int MAX_NUMBERS = 4;

    /////////
    // FIELDS
    /////////

    private int maxRetries, minWiredDots, retryCount = 0, captchaWiredDots;
    private boolean autoSave, stealthMode;
    private Encrypter encrypter;
    private ButtonOkCommand btnOkCmd;
    private Intent intentResult;
    private LoadingView<Void, Void, Object> loadingView;

    //密码输入框
    private LinearLayout mInputLayout;
    private TextView mTvInputInfo;
    private TextView mTvForget;
    private LockDigitalView mDigitalView;

    @Override
    protected int getContentView() {
        return R.layout.activity_lock_digital;
    }

    @Override
    protected void initData() {

        // Save all controls' state to restore later
        CharSequence infoText = mTvInputInfo != null ? mTvInputInfo.getText() : null;

        UI.adjustDialogSizeForLargeScreens(getWindow());

        // COMMAND BUTTONS

        int infoRes = getExtraTextInfo();

        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
            SystemUtil.setViewVisibility(mTvForget, View.GONE);

            if (!TextUtils.isEmpty(infoText)) mTvInputInfo.setText(infoText);
            else if (infoRes != 0) {
                mTvInputInfo.setText(infoRes);
            } else {
                mTvInputInfo.setText(R.string.alp_msg_input_an_lock);
            }

            // BUTTON OK
            if (btnOkCmd == null) btnOkCmd = ButtonOkCommand.CONTINUE;
//            switch (btnOkCmd) {
//                case CONTINUE: mBtnConfirm.setText(R.string.alp_cmd_continue); break;
//                case DONE: mBtnConfirm.setText(R.string.alp_cmd_confirm); break;
//                default: break;// Do nothing
//            }
//            if (btnOkEnabled != null) mBtnConfirm.setEnabled(btnOkEnabled);
        }//ACTION_CREATE_PATTERN
        else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            if (!TextUtils.isEmpty(infoText)) mTvInputInfo.setText(infoText);
            else if (infoRes != 0) {
                mTvInputInfo.setText(infoRes);
            } else {
                mTvInputInfo.setText(R.string.alp_msg_input_to_unlock);
            }
            if (getIntent().hasExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN)) {

                SystemUtil.setViewVisibility(mTvForget, View.VISIBLE);

//                mBtnConfirm.setOnClickListener(this);
//                mBtnConfirm.setText(R.string.alp_cmd_forgot_pattern);
//                mBtnConfirm.setEnabled(true);
//                mFooter.setVisibility(View.VISIBLE);
            }//if
        }//ACTION_COMPARE_PATTERN
    }

    /**
     * 获取指定的标题信息
     * @return
     */
    private int getExtraTextInfo() {
        int resId = 0;
        Intent intent = getIntent();
        if (intent != null) {
            resId = intent.getIntExtra(EXTRA_TEXT_INFO, 0);
        }
        return resId;
    }

    /**
     * 是否是解锁行为,即，是否已锁定的方式进入此界面
     * @return
     */
    private boolean isLockAction() {
        Intent intent = getIntent();
        boolean isLockAction = false;
        if (intent != null) {
            isLockAction = intent.getBooleanExtra(ILockerActivityDelegate.EXTRA_FLAG_LOCK, false);
        }
        return isLockAction;
    }

    @Override
    protected boolean hasLockedController() {
        Intent intent = getIntent();
        boolean hasLockController = false;
        if (intent != null) {
            hasLockController = intent.getBooleanExtra(EXTRA_HAS_LOCK_CONTROLLER, false);
        }
        return hasLockController;
    }

    @Override
    protected void initView() {
        //检查是否支持action
        checkAction();
        loadSettings();
        intentResult = new Intent();
        setResult(RESULT_CANCELED, intentResult);

        // Title
        if (getIntent().hasExtra(EXTRA_TITLE)) {
            Object title = getIntent().getExtras().get(EXTRA_TITLE);
            if (title instanceof Integer) title = getString((Integer) title);
            setTitle((CharSequence) title);
        }//if

        mTvInputInfo = (TextView) findViewById(R.id.alp_textview_info);
        mTvForget = (TextView) findViewById(R.id.alp_textview_forget);
        mDigitalView = (LockDigitalView) findViewById(R.id.alp_view_lock_digital);

        // LOCK PATTERN VIEW

        switch (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getResources().getDimensionPixelSize(R.dimen.alp_lockpatternview_size);
                ViewGroup.LayoutParams lp = mDigitalView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                mDigitalView.setLayoutParams(lp);

                break;
            }//LARGE / XLARGE
        }

        initDigitalInput(mContext);

        //设置密码位数
        mDigitalView.setMaxNumbers(MAX_NUMBERS);
        mDigitalView.setInputChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isModify() && getLockerActivityDelegate() != null && !hasLockedController() && !getLockerActivityDelegate().isLocked()) {
            KLog.d(TAG, "onResume is locked and will finish");
            finish();
        }
    }

    /**
     * 是否是修改密码
     * @return
     */
    private boolean isModify() {
        Intent intent = getIntent();
        boolean flag = false;
        if (intent != null) {
            flag = intent.getBooleanExtra(EXTRA_IS_MODIFY, false);
        }
        return flag;
    }

    @Override
    protected void onDestroy() {
        if (loadingView != null) loadingView.cancel(true);
        super.onDestroy();
    }

    /**
     * 取消解锁
     * @return
     */
    private boolean onUnLockCanceled() {
        if (loadingView != null) loadingView.cancel(true);

        finishWithNegativeResult(RESULT_CANCELED);

        return true;
    }

    @Override
    protected void onBack() {
        onUnLockCanceled();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Use this hook instead of onBackPressed(), because onBackPressed() is not available in API 4.
        if (keyCode == KeyEvent.KEYCODE_BACK && ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            return onUnLockCanceled();
        }//if
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Loads settings, either from manifest or {@link AlpSettings}.
     */
    private void loadSettings() {
        Bundle metaData = null;
        try { metaData = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA).metaData; }
        catch (PackageManager.NameNotFoundException e) { KLog.e(TAG, e.getMessage(), e); }// Should never catch this

        if (metaData != null && metaData.containsKey(METADATA_MAX_RETRIES))
            maxRetries = AlpSettings.Display.validateMaxRetries(this, metaData.getInt(METADATA_MAX_RETRIES));
        else maxRetries = AlpSettings.Display.getMaxRetries(this);

        if (metaData != null && metaData.containsKey(METADATA_AUTO_SAVE_PATTERN))
            autoSave = metaData.getBoolean(METADATA_AUTO_SAVE_PATTERN);
        else autoSave = AlpSettings.Security.isAutoSavePattern(this);

        // Encrypter
        final char[] encrypterClass;
        if (metaData != null && metaData.containsKey(METADATA_ENCRYPTER_CLASS))
            encrypterClass = metaData.getString(METADATA_ENCRYPTER_CLASS).toCharArray();
        else encrypterClass = AlpSettings.Security.getEncrypterClass(this);

        if (encrypterClass != null) try {
            encrypter = (Encrypter) Class.forName(new String(encrypterClass), false, getClassLoader()).newInstance();
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
            throw new InvalidEncrypterException();
        }
    }//loadSettings()

    /**
     * get max retries count
     * @return
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 检查是否支持action
     * @throws UnsupportedOperationException
     */
    private void checkAction() throws UnsupportedOperationException {
        // Check actions
        {
            final String action = getIntent().getAction();
            boolean found = false;
            for (final String s : SUPPORTED_ACTIONS) if (TextUtils.equals(action, s)) { found = true; break; }
            if (!found) throw new UnsupportedOperationException("Unsupported action: " + action);
        }//check action
    }

    /**
     * Finishes activity with {@link Activity#RESULT_OK}.
     *
     * @param digital the digital, if this is in mode creating pattern. In any cases, it can be set to {@code null}.
     */
    private void finishWithResultOk(@Nullable String digital) {
        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) intentResult.putExtra(EXTRA_PATTERN, digital);
        else {
            // If the user was "logging in", minimum try count can not be zero.
            intentResult.putExtra(EXTRA_RETRY_COUNT, retryCount + 1);
        }

        setResult(RESULT_OK, intentResult);

        // ResultReceiver
        final ResultReceiver receiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            final Bundle bundle = new Bundle();
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) bundle.putString(EXTRA_PATTERN, digital);
            else {
                // If the user was "logging in", minimum try count can not be zero.
                bundle.putInt(EXTRA_RETRY_COUNT, retryCount + 1);
            }
            receiver.send(RESULT_OK, bundle);
        }

        // PendingIntent
        final PendingIntent piOk = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT_OK);
        if (piOk != null) try { piOk.send(this, RESULT_OK, intentResult); }
        catch (Throwable t) { Log.e(TAG, CLASSNAME + " >> Failed sending PendingIntent: " + piOk, t); }

        finish();
    }//finishWithResultOk()

    /**
     * Finishes the activity with negative result ({@link #RESULT_CANCELED}, {@link #RESULT_FAILED} or {@link
     * #RESULT_FORGOT_PATTERN}).
     */
    private void finishWithNegativeResult(int resultCode) {
        boolean hasAnim = true;
        if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            intentResult.putExtra(ILockerActivityDelegate.EXTRA_FLAG_IS_BACK_PRESSED, true);
            intentResult.putExtra(EXTRA_RETRY_COUNT, retryCount);
            intentResult.putExtra(EXTRA_RETRY_MAX_COUNT, maxRetries);
            hasAnim = !isLockAction();
        }

        setResult(resultCode, intentResult);

        // ResultReceiver
        final ResultReceiver receiver = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            final Bundle resultBundle;
            if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, retryCount);
            } else resultBundle = null;
            receiver.send(resultCode, resultBundle);
        }//if

        // PendingIntent
        final PendingIntent piCancelled = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT_CANCELLED);
        if (piCancelled != null) try { piCancelled.send(this, resultCode, intentResult); }
        catch (Throwable t) { Log.e(TAG, CLASSNAME + " >> Failed sending PendingIntent: " + piCancelled, t); }

        finish();
        if (!hasAnim) {
            overridePendingTransition(0, 0);
        }
    }//finishWithNegativeResult()

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    /**
     * 清除密码
     */
    private void resetDigital() {
        resetCheckState(false);
        int infoRes = getExtraTextInfo();
        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
//            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
//                mBtnConfirm.setEnabled(false);
            if (btnOkCmd == ButtonOkCommand.CONTINUE) {
                getIntent().removeExtra(EXTRA_PATTERN);
                if (infoRes != 0) {
                    mTvInputInfo.setText(infoRes);
                } else {
                    mTvInputInfo.setText(R.string.alp_msg_input_to_unlock);
                }
            } else mTvInputInfo.setText(R.string.alp_msg_input_an_lock_confirm);
        }//ACTION_CREATE_PATTERN
        else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
//            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
            if (infoRes != 0) {
                mTvInputInfo.setText(infoRes);
            } else {
                mTvInputInfo.setText(R.string.alp_msg_input_to_unlock);
            }
        }//ACTION_COMPARE_PATTERN
    }

    /**
     * 设置密码输入框的选中状态
     * @param index 需要改变的索引
     * @param isChecked 是否选中
     */
    private void setCheckState(int index, boolean isChecked) {
        if (mInputLayout != null) {
            CheckBox checkBox = (CheckBox) mInputLayout.getChildAt(index);
            checkBox.setChecked(isChecked);
        }
    }

    /**
     * 重置密码输入框的选中状态
     * @param isChecked
     */
    private void resetCheckState(boolean isChecked) {
        if (mInputLayout != null) {
            KLog.d("resetDigital called");
            int size = mInputLayout.getChildCount();
            for (int i = 0; i < size; i++) {
                CheckBox checkBox = (CheckBox) mInputLayout.getChildAt(i);
                checkBox.setChecked(isChecked);
            }
        }
    }

    /**
     * 初始化数字输入框
     * @param context
     * @return
     */
    private LinearLayout initDigitalInput(Context context) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.input_layout);
        if (linearLayout == null) {
            return null;
        }
        int horizontalSpace  = getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        for (int i = 0; i < MAX_NUMBERS; i++) {

            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            CheckBox checkBox = new CheckBox(context);
            checkBox.setBackgroundResource(0);
            checkBox.setClickable(false);
            checkBox.setFocusable(false);
            checkBox.setButtonDrawable(com.yunxinlink.notes.lockpattern.R.drawable.alp_digital_check_color_dark);
            frameParams.gravity = Gravity.CENTER;
            checkBox.setLayoutParams(frameParams);

            frameParams.leftMargin = horizontalSpace;
            frameParams.rightMargin = horizontalSpace;

            linearLayout.addView(checkBox);
        }

        mInputLayout = linearLayout;
        return linearLayout;
    }

    /**
     * Checks and creates the pattern.
     *
     * @param digital the current digital of lock view.
     */
    private void doCheckAndCreatePattern(@NonNull final String digital) {

        if (getIntent().hasExtra(EXTRA_PATTERN)) {
            // Use a LoadingView because decrypting pattern might take time...
            loadingView = new LoadingView<Void, Void, Object>(this, null) {

                @Override
                protected Object doInBackground(Void... params) {
                    if (encrypter != null) return digital.equals(
                            encrypter.decrypt(mContext, getIntent().getStringExtra(EXTRA_PATTERN))
                    );
                    return getIntent().getStringExtra(EXTRA_PATTERN).equals(LockPatternUtils.patternToSha1(digital));
                }//doInBackground()

                @Override
                protected void onPostExecute(Object result) {
                    super.onPostExecute(result);

                    if ((Boolean) result) {
                        mTvInputInfo.setText(R.string.alp_msg_input_new_lock);
                        String digital = getIntent().getStringExtra(EXTRA_PATTERN);
                        if (autoSave) AlpSettings.Security.setDigital(mContext, digital);
                        finishWithResultOk(digital);
//                        mBtnConfirm.setEnabled(true);
                    } else {
                        mTvInputInfo.setText(R.string.alp_msg_input_lock_error);
//                        mBtnConfirm.setEnabled(false);
//                        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
//                        mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }//onPostExecute()

            };

            loadingView.execute();
        } else {
            // Use a LoadingView because encrypting pattern might take time...
            loadingView = new LoadingView<Void, Void, Object>(this, null) {

                @Override
                protected Object doInBackground(Void... params) {
                    return encrypter != null ?
                            encrypter.encrypt(mContext, digital) : LockPatternUtils.patternToSha1(digital);
                }//onCancel()

                @Override
                protected void onPostExecute(Object result) {
                    super.onPostExecute(result);

                    getIntent().putExtra(EXTRA_PATTERN, (String) result);

                    if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                        if (btnOkCmd == ButtonOkCommand.CONTINUE) {
                            btnOkCmd = ButtonOkCommand.DONE;
                            resetDigital();
                            mTvInputInfo.setText(R.string.alp_msg_input_an_lock_confirm);
//                        mBtnConfirm.setText(R.string.alp_cmd_confirm);
//                        mBtnConfirm.setEnabled(false);
                        } else {
                            final String digital = getIntent().getStringExtra(EXTRA_PATTERN);
                            if (autoSave) AlpSettings.Security.setDigital(mContext, digital);
                            finishWithResultOk(digital);
                        }
                    }//ACTION_CREATE_PATTERN
                    else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                        // We don't need to verify the extra. First, this button is only visible if there is this extra in the intent. Second, it is the
                        // responsibility of the caller to make sure the extra is good.
                        final PendingIntent pi = getIntent().getParcelableExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN);
                        try { if (pi != null) pi.send(); }
                        catch (Throwable t) { Log.e(TAG, CLASSNAME + " >> Failed sending pending intent: " + pi, t); }

                        finishWithNegativeResult(RESULT_FORGOT_PATTERN);
                    }//ACTION_COMPARE_PATTERN
                    //接着进行下一次的录制
//                    mBtnConfirm.setEnabled(true);
                }//onPostExecute()

            };

            loadingView.execute();
        }
    }//doCheckAndCreatePattern()

    /**
     * Compares {@code digital} to the given digital ( {@link #ACTION_COMPARE_PATTERN}). Then finishes the activity if they match.
     *
     * @param digital the digital to be compared.
     */
    private void doComparePattern(@NonNull final String digital) {
        if (digital == null) return;

        // Use a LoadingView because decrypting pattern might take time...
        loadingView = new LoadingView<Void, Void, Object>(this, null) {

            @Override
            protected Object doInBackground(Void... params) {
                if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                    String currentDigital = getIntent().getStringExtra(EXTRA_PATTERN);
                    if (currentDigital == null) currentDigital = AlpSettings.Security.getDigital(mContext);
                    if (currentDigital != null)
                        if (encrypter != null) return digital.equals(encrypter.decrypt(mContext, currentDigital));
                        else return currentDigital.equals(LockPatternUtils.patternToSha1(digital));
                }//ACTION_COMPARE_PATTERN

                return false;
            }//doInBackground()

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);

                if ((Boolean) result) finishWithResultOk(null);
                else {
                    retryCount++;
                    intentResult.putExtra(EXTRA_RETRY_COUNT, retryCount);

                    if (retryCount >= maxRetries) finishWithNegativeResult(RESULT_FAILED);
                    else {
//                        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                        mTvInputInfo.setText(R.string.alp_msg_try_again);
                        resetCheckState(false);
//                        mLockPatternView.postDelayed(mLockPatternViewReloader, DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }
            }//onPostExecute()

        };

        loadingView.execute();
    }//doComparePattern()

    @Override
    public void onInputCompleted(String digital) {
        setCheckState(MAX_NUMBERS - 1, true);

        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
            doCheckAndCreatePattern(digital);
        }//ACTION_CREATE_PATTERN
        else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            doComparePattern(digital);
        }//ACTION_COMPARE_PATTERN

    }

    @Override
    public void onInput(int index, boolean isChecked, String digital) {
        setCheckState(index, isChecked);
    }

    @Override
    public void onInputCleared() {
        resetDigital();
    }

    /**
     * Intent builder.
     */
    public static class IntentBuilder {

        /**
         * Makes new builder with {@link #ACTION_CREATE_PATTERN}.
         *
         * @param context the context.
         * @return new builder.
         */
        @NonNull
        public static IntentBuilder newPatternCreator(@NonNull Context context) {
            return new IntentBuilder(context, LockDigitalActivity.class, ACTION_CREATE_PATTERN);
        }//newPatternCreator()

        /**
         * Makes new builder with {@link #ACTION_COMPARE_PATTERN}.
         *
         * @param context the context.
         * @param pattern the pattern.
         * @return new builder.
         */
        @NonNull
        public static IntentBuilder newPatternComparator(@NonNull Context context, @Nullable String pattern) {
            return new IntentBuilder(context, LockDigitalActivity.class, ACTION_COMPARE_PATTERN).setPattern(pattern);
        }//newPatternComparator()

        /**
         * Makes new builder with {@link #ACTION_COMPARE_PATTERN}.
         *
         * @param context the context.
         * @return new builder.
         */
        @NonNull
        public static IntentBuilder newPatternComparator(@NonNull Context context) {
            return newPatternComparator(context, null);
        }//newPatternComparator()

        @NonNull
        private final Context context;
        @NonNull
        private final Intent intent;

        /**
         * Makes new instance.
         *
         * @param context the context.
         * @param clazz   class of {@link LockDigitalActivity} or its subclass.
         * @param action  action.
         */
        public IntentBuilder(@NonNull Context context, @NonNull Class<? extends LockDigitalActivity> clazz, @NonNull String action) {
            this.context = context;
            intent = new Intent(action, null, context, clazz);
        }//IntentBuilder()

        /**
         * Gets the intent being built.
         *
         * @return the intent.
         */
        @NonNull
        public Intent getIntent() {
            return intent;
        }//getIntent()

        /**
         * Builds the final intent.
         *
         * @return the final intent.
         */
        @NonNull
        public Intent build() {
            return intent;
        }//build()

        /**
         * Calls {@link #build()} and builds new {@link PendingIntent} from it.
         *
         * @param requestCode request code.
         * @param flags       flags.
         * @return the new pending intent.
         */
        @Nullable
        public PendingIntent buildPendingIntent(int requestCode, int flags) {
            //noinspection ResourceType
            return PendingIntent.getActivity(context, requestCode, build(), flags);
        }//buildPendingIntent()

        /**
         * Calls {@link #build()} and builds new {@link PendingIntent} from it.
         *
         * @param requestCode request code.
         * @param flags       flags.
         * @param options     options.
         * @return the new pending intent.
         */
        @Nullable
        public PendingIntent buildPendingIntent(int requestCode, int flags,
                                                @Api(level = Build.VERSION_CODES.JELLY_BEAN, required = false) @Nullable Bundle options) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                //noinspection ResourceType
                return PendingIntent.getActivity(context, requestCode, build(), flags, options);
            }//if

            return buildPendingIntent(requestCode, flags);
        }//buildPendingIntent()

        /**
         * Builds the intent via {@link #build()} and calls {@link #startActivityForResult(Intent, int)}.
         *
         * @param activity    your activity.
         * @param requestCode request code.
         */
        public void startForResult(@NonNull Activity activity, int requestCode) {
            activity.startActivityForResult(build(), requestCode);
        }//startForResult()

        /**
         * Builds the intent via {@link #build()} and calls {@link #startActivityForResult(Intent, int, Bundle)}.
         *
         * @param activity    your activity.
         * @param requestCode request code.
         * @param options     options.
         */
        @Api(level = Build.VERSION_CODES.JELLY_BEAN)
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void startForResult(@NonNull Activity activity, int requestCode, @Nullable Bundle options) {
            activity.startActivityForResult(build(), requestCode, options);
        }//startForResult()

        /**
         * Builds the intent via {@link #build()} and calls {@link Fragment#startActivityForResult(Intent, int)}.
         *
         * @param fragment    your fragment.
         * @param requestCode request code.
         */
        @Api(level = Build.VERSION_CODES.HONEYCOMB)
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public void startForResult(@NonNull Fragment fragment, int requestCode) {
            fragment.startActivityForResult(build(), requestCode);
        }//startForResult()

        /**
         * Builds the intent via {@link #build()} and calls {@link Fragment#startActivityForResult(Intent, int, Bundle)}.
         *
         * @param fragment    your fragment.
         * @param requestCode request code.
         * @param options     options.
         */
        @Api(level = Build.VERSION_CODES.JELLY_BEAN)
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void startForResult(@NonNull Fragment fragment, int requestCode, @Nullable Bundle options) {
            fragment.startActivityForResult(build(), requestCode, options);
        }//startForResult()

        /**
         * Builds the intent via {@link #build()} and calls {@link Context#startActivity(Intent)}.
         */
        public void start() {
            context.startActivity(build());
        }//start()

        /**
         * Builds the intent via {@link #build()} and calls {@link Context#startActivity(Intent, Bundle)}.
         */
        @Api(level = Build.VERSION_CODES.JELLY_BEAN)
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        public void start(@Nullable Bundle options) {
            context.startActivity(build(), options);
        }//start()

        /**
         * Sets pattern.
         *
         * @param pattern see {@link #EXTRA_PATTERN}.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setPattern(@Nullable String pattern) {
            if (pattern != null) intent.putExtra(EXTRA_PATTERN, pattern);
            else intent.removeExtra(EXTRA_PATTERN);

            return (T) this;
        }//setPattern()

        /**
         * Sets result receiver.
         *
         * @param resultReceiver see {@link #EXTRA_RESULT_RECEIVER}.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setResultReceiver(@Nullable ResultReceiver resultReceiver) {
            if (resultReceiver != null) intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            else intent.removeExtra(EXTRA_RESULT_RECEIVER);

            return (T) this;
        }//setResultReceiver()

        /**
         * Sets pending intent OK.
         *
         * @param pendingIntent see {@link #EXTRA_PENDING_INTENT_OK}.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setPendingIntentOk(@Nullable PendingIntent pendingIntent) {
            if (pendingIntent != null) intent.putExtra(EXTRA_PENDING_INTENT_OK, pendingIntent);
            else intent.removeExtra(EXTRA_PENDING_INTENT_OK);

            return (T) this;
        }//setPendingIntentOk()

        /**
         * Sets pending intent cancelled.
         *
         * @param pendingIntent see {@link #EXTRA_PENDING_INTENT_CANCELLED}.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setPendingIntentCancelled(@Nullable PendingIntent pendingIntent) {
            if (pendingIntent != null) intent.putExtra(EXTRA_PENDING_INTENT_CANCELLED, pendingIntent);
            else intent.removeExtra(EXTRA_PENDING_INTENT_CANCELLED);

            return (T) this;
        }//setPendingIntentCancelled()

        /**
         * Sets pending intent forgot pattern.
         *
         * @param pendingIntent see {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN}.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setPendingIntentForgotPattern(@Nullable PendingIntent pendingIntent) {
            if (pendingIntent != null) intent.putExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN, pendingIntent);
            else intent.removeExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN);

            return (T) this;
        }//setPendingIntentForgotPattern()

        /**
         * Adds these flags to the intent: {@link Intent#FLAG_ACTIVITY_CLEAR_TASK}, {@link Intent#FLAG_ACTIVITY_CLEAR_TOP},
         * {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
         *
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T makeRestartTask() {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            return (T) this;
        }//makeRestartTask()

        /**
         * Sets title via {@link #EXTRA_TITLE}.
         *
         * @param resId string resource ID of the title. You can pass {@code 0} to remove the extra.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setTitle(@StringRes int resId) {
            if (resId != 0) intent.putExtra(EXTRA_TITLE, resId);
            else intent.removeExtra(EXTRA_TITLE);

            return (T) this;
        }//setTitle()

        /**
         * Sets title via {@link #EXTRA_TITLE}.
         *
         * @param title the title.
         * @return this builder.
         */
        @NonNull
        public <T extends IntentBuilder> T setTitle(@Nullable CharSequence title) {
            intent.putExtra(EXTRA_TITLE, title);
            return (T) this;
        }//setTitle()

    }//IntentBuilder
}
