package com.yunxinlink.notes.lock.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lockpattern.widget.DigitalLockView;
import com.yunxinlink.notes.ui.BaseActivity;

public class LockDigitalActivity extends BaseActivity implements DigitalLockView.OnInputChangedListener {

    /**
     * 4位密码
     */
    private static final int MAX_NUMBERS = 4;

    //密码输入框
    private LinearLayout mInputLayout;
    private TextView mTvInputInfo;
    private TextView mTvforget;
    private DigitalLockView mDigitalView;

    @Override
    protected int getContentView() {
        return R.layout.activity_lock_digital;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {
        mTvInputInfo = (TextView) findViewById(R.id.alp_textview_info);
        mTvforget = (TextView) findViewById(R.id.alp_textview_forget);
        mDigitalView = (DigitalLockView) findViewById(R.id.alp_view_lock_digital);
        initDigitalInput(mContext);

        //设置密码位数
        mDigitalView.setMaxNumbers(MAX_NUMBERS);
        mDigitalView.setInputChangedListener(this);
    }

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    /**
     * 清除密码
     */
    private void resetDigital() {
        if (mInputLayout != null) {
            KLog.d("resetDigital called");
            int size = mInputLayout.getChildCount();
            for (int i = 0; i < size; i++) {
                CheckBox checkBox = (CheckBox) mInputLayout.getChildAt(i);
                checkBox.setChecked(false);
            }
        }
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

    @Override
    public void onInputCompleted(String digital) {
        setCheckState(MAX_NUMBERS - 1, true);
    }

    @Override
    public void onInput(int index, boolean isChecked, String digital) {
        setCheckState(index, isChecked);
    }

    @Override
    public void onInputCleared() {
        resetDigital();
    }
}
