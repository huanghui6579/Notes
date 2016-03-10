package net.ibaixin.notes.ui;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.EditStep;
import net.ibaixin.notes.util.log.Log;

import java.lang.ref.WeakReference;
import java.util.Stack;

/**
 * 笔记编辑界面
 * @author huanghui1
 * @update 2016/3/2 10:13
 * @version: 1.0.0
 */
public class NoteEditActivity extends BaseActivity implements View.OnClickListener, TextWatcher {
    
    private static final int MSG_INIT_BOOTOM_TOOL_BAR = 1;

    private PopupMenu mAttachPopu;
    private PopupMenu mCameraPopu;
    private PopupMenu mOverflowPopu;
    
    private EditText mEtContent;
    
    private TextView mToolbarTitleView;

    /**
     * 编辑步骤的记录容器
     */
    private Stack<EditStep> mUndoStack = new Stack<>();
    private Stack<EditStep> mRedoStack = new Stack<>();

    private View mIvRedo;
    private View mIvUndo;
    /**
     * 是否将编辑步骤添加到容器里
     */
    private boolean mIsDo;

    /**
     * 是否是编辑列表，每按依次回车，则在前面添加一个“-”
     */
    private boolean mIsFormatList;

    /**
     * 是否是手动回车换行
     */
    private boolean mIsNextLine;

    private Handler mHandler = new MyHandler(this);

    private void setCustomTitle(CharSequence title, int iconResId) {
        if (!TextUtils.isEmpty(title)) {
            if (mToolbarTitleView == null) {
                LayoutInflater inflater = LayoutInflater.from(this);
                mToolbarTitleView = (TextView) inflater.inflate(R.layout.edit_custom_title, null);
            }
            if (mToolBar != mToolbarTitleView.getParent()) {
                mToolBar.addView(mToolbarTitleView);
            }
            mToolbarTitleView.setText(title);

        } else {
            if (mToolbarTitleView != null) {
                mToolBar.removeView(mToolbarTitleView);
            }
        }
    }
    
    @Override
    protected void initToolBar() {
        super.initToolBar();
        CharSequence title = getTitle();
        setTitle(null);
        setCustomTitle(title, 0);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_note_edit;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {
        mEtContent = (EditText) findViewById(R.id.et_content);
        mHandler.sendEmptyMessage(MSG_INIT_BOOTOM_TOOL_BAR);

        mEtContent.addTextChangedListener(this);

        mEtContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //回车换行
                    mIsNextLine = true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_edit, menu);
        MenuItem item = menu.findItem(R.id.action_more);
        setMenuOverFlowTint(item);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View attachView = null;
        switch (item.getItemId()) {
            case R.id.action_attach:    //添加附件
                attachView = getToolBarMenuView(R.id.action_attach);
                createPopuMenu(attachView, mAttachPopu, R.menu.edit_attach, true);
                break;
            case R.id.action_more:  //更多
                attachView = getToolBarMenuView(R.id.action_more);
                final PopupMenu attachPopu = createPopuMenu(attachView, mAttachPopu, R.menu.edit_overflow, false);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Menu menu = attachPopu.getMenu();

                        MenuItem shareItem = menu.findItem(R.id.action_share);
                        setMenuTint(shareItem, 0);

                        MenuItem searchItem = menu.findItem(R.id.action_search);
                        setMenuTint(searchItem, 0);

                        MenuItem deleteItem = menu.findItem(R.id.action_delete);
                        setMenuTint(deleteItem, 0);

                        if (attachPopu != null) {
                            attachPopu.show();
                        }
                    }
                });

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 创建附件的弹出菜单
     * @param showImmediate 是否立刻显示，调用show方法
     * @author huanghui1
     * @update 2016/3/2 14:15
     * @version: 1.0.0
     */
    private PopupMenu createPopuMenu(View aucher, PopupMenu popupMenu, int menuResId, boolean showImmediate) {
        if (aucher != null) {
            if (popupMenu == null) {
                popupMenu = createPopuMenu(aucher, menuResId, true, new OnPopuMenuItemClickListener());
            }
            if (showImmediate) {
                popupMenu.show();
            }
            return popupMenu;
        } else {
            return null;
        }
    }

    /**
     * 初始化底部的工具栏
     * @author tiger
     * @update 2016/3/5 8:44
     * @version 1.0.0
     */
    private void initBottomToolBar() {
        ViewStub viewStub = (ViewStub) findViewById(R.id.bottom_tool_bar);
        View bottomBar = viewStub.inflate();
        ViewGroup toolContainer = (ViewGroup) bottomBar.findViewById(R.id.tool_container);
        int childSize = toolContainer.getChildCount();
        for (int i = 0; i < childSize; i++) {
            View child = toolContainer.getChildAt(i);
            switch (child.getId()) {
                case R.id.iv_undo:
                    mIvUndo = child;
                    child.setEnabled(false);
                    break;
                case R.id.iv_redo:
                    mIvRedo = child;
                    child.setEnabled(false);
                    break;
            }
            child.setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_undo:  //后退
                undo();
                break;
            case R.id.iv_redo:  //前进
                redo();
                break;
            case R.id.iv_list:  //格式化列表
                toggleFormatList();
                break;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (!mIsDo && s != null) {
            EditStep editStep = new EditStep();
            editStep.setLength(count);
            editStep.setStart(start);
            if (after == 0 && count > 0) {  //删除的文字
                String content = s.toString();
                int end = start + count;
                content = content.substring(start, end);    //截取出删除的文字
                editStep.setContent(content);
                editStep.setAppend(false);
                editStep.setEnd(end);
            } else {
                editStep.setAppend(true);
            }
            pushUndo(editStep);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!mIsDo && s != null) {
            EditStep editStep = getUndo();
            if (editStep.isAppend()) {  //添加文字
                int end = start + count;
                editStep.setEnd(end);
                editStep.setContent(s.subSequence(start, end));
                editStep.setStart(start);
                editStep.setLength(count);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "*****content***" + mEtContent.getText());
        super.onBackPressed();
    }

    @Override
    protected void beforeBack() {
        super.beforeBack();
        mEtContent.beginBatchEdit();
        Log.d(TAG, "*****content***" + mEtContent.getText());
    }

    /**
     * 添加回退操作
     * @author tiger
     * @update 2016/3/5 9:09
     * @version 1.0.0
     */
    private void pushUndo(EditStep editStep) {
        mUndoStack.push(editStep);

        if (!mIvUndo.isEnabled()) {
            mIvUndo.setEnabled(true);
        }
    }

    /**
     * 获取顶部的回退操作
     * @author tiger
     * @update 2016/3/5 14:16
     * @version 1.0.0
     */
    private EditStep getUndo() {
        return mUndoStack.peek();
    }

    /**
     * 添加前进操作
     * @author tiger
     * @update 2016/3/5 9:11
     * @version 1.0.0
     */
    private void pushRedo(EditStep editStep) {
        mRedoStack.push(editStep);

        if (!mIvRedo.isEnabled()) {
            mIvRedo.setEnabled(true);
        }
    }

    /**
     * 撤销一步编辑
     * @author tiger
     * @update 2016/3/5 8:47
     * @version 1.0.0
     */
    private void undo() {
        EditStep editStep = mUndoStack.pop();
        if (!editStep.isEmpty()) {
            CharSequence content = editStep.getContent().toString();
            Editable editable = mEtContent.getEditableText();
            int start = editStep.getStart();
            int end = editStep.getEnd();
            setdo(true);
            if (editStep.isAppend()) {  //之前的操作是插入文字，则此时的操作是删除文字
                editable.delete(start, end);
            } else {    //之前的操作是删除文字，则此时的操作是插入文字
                editable.insert(start, content);
            }
            setdo(false);
            editStep.setContent(content);
            pushRedo(editStep);
        }

        if (mUndoStack.size() == 0 && mIvUndo.isEnabled()) {
            mIvUndo.setEnabled(false);
        }
    }
    
    /**
     * 前进
     * @author tiger
     * @update 2016/3/5 9:16
     * @version 1.0.0
     */
    private void redo() {
        setdo(true);
        EditStep editStep = mRedoStack.pop();
        CharSequence content = editStep.getContent();
        Editable editable = mEtContent.getEditableText();
        if (editStep.isAppend()) {  //前进操作是插入文字
            editable.insert(editStep.getStart(), content);
        } else {    //前进操作是删除文字
            editable.delete(editStep.getStart(), editStep.getEnd());
        }

        pushUndo(editStep);

        if (mRedoStack.size() == 0 && mIvRedo.isEnabled()) {
            mIvRedo.setEnabled(false);
        }
        setdo(false);
    }
    
    /**
     * 在格式化列表直接切换
     * @author huanghui1
     * @update 2016/3/10 14:36
     * @version: 1.0.0
     */
    private void toggleFormatList() {
        mIsFormatList = !mIsFormatList;
        String text = mEtContent.getText().toString();
        Editable editable = mEtContent.getEditableText();
        //光标的开始位置
        int start = mEtContent.getSelectionStart();
        String subS = text.substring(0, start);
        //光标所在行的第一位
        int startIndex = subS.lastIndexOf("\n") + 1;
        editable.insert(startIndex, "-");
    }

    /**
     * 设置该操作是都前进操作
     * @author tiger
     * @update 2016/3/5 9:38
     * @version 1.0.0
     */
    private void setdo(boolean isDo) {
        mIsDo = isDo;
    }

    class OnPopuMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return false;
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<NoteEditActivity> mTarget;

        public MyHandler(NoteEditActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            NoteEditActivity activity = mTarget.get();
            switch (msg.what) {
                case MSG_INIT_BOOTOM_TOOL_BAR:
                    activity.initBottomToolBar();
                    break;
            }
            super.handleMessage(msg);
        }
    }
    
}
