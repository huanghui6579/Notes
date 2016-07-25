package net.ibaixin.notes.widget;

import android.content.Context;
import android.text.TextWatcher;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.util.SystemUtil;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/7/17 10:34
 */
public class NoteFramenLayout extends FrameLayout {
    private static final java.lang.String TAG = "NoteFramenLayout";
    /**
     * 是否是编辑模式
     */
    private boolean mIsEditMode;

    private NoteTextView mTvContent;

    private NoteEditText mEtContent;

    public NoteFramenLayout(Context context) {
        super(context);
    }

    public NoteFramenLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoteFramenLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTvContent = (NoteTextView) findViewById(R.id.tv_content);
        mEtContent = (NoteEditText) findViewById(R.id.et_content);
    }

    /**
     * 切换到编辑模式
     */
    public void changeToEditMode() {
        if (mIsEditMode) {
            return;
        }
        SystemUtil.showView(mEtContent);
        //移除textview
        removeView(mTvContent);
        mTvContent = null;
        mIsEditMode = true;
    }

    /**
     * 是否是编辑模式
     * @return 是否是编辑模式
     */
    public boolean isEditMode() {
        return mIsEditMode;
    }

    /**
     * @return 返回编辑控件
     */
    public EditText getEditText() {
        return mEtContent;
    }

    /**
     * @return 返回显示的控件
     */
    public TextView getTextView() {
        return mTvContent;
    }

    /**
     * 为编辑框添加文字改变的监听器
     * @param watcher 文字改变的监听器
     */
    public void addTextChangedListener(TextWatcher watcher) {
        mEtContent.addTextChangedListener(watcher);
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        mEtContent.setOnEditorActionListener(listener);
    }

    public void setOnSelectionChangedListener(NoteEditText.SelectionChangedListener listener) {
        mEtContent.setOnSelectionChangedListener(listener);
    }

    public void setEditTextMovementMethod(MovementMethod movement) {
        mEtContent.setMovementMethod(movement);
    }

    public void setTextViewMovementMethod(MovementMethod movement) {
        if (mTvContent != null) {
            mTvContent.setMovementMethod(movement);
        }
    }

    public void setOnItemClickListener(OnClickListener listener) {
        mEtContent.setOnClickListener(listener);
        if (mTvContent != null) {
            mTvContent.setOnClickListener(listener);
        }
    }

}
