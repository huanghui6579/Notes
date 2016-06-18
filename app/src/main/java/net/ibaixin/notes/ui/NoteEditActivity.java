package net.ibaixin.notes.ui;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.EditStep;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.persistent.NoteManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.DigestUtil;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.NoteEditText;

import java.lang.ref.WeakReference;
import java.util.Stack;

/**
 * 笔记编辑界面
 * @author huanghui1
 * @update 2016/3/2 10:13
 * @version: 1.0.0
 */
public class NoteEditActivity extends BaseActivity implements View.OnClickListener, TextWatcher {
    public static final String ARG_NOTE_ID = "noteId";
    public static final String ARG_FOLDER_ID = "folderId";
    
    private static final int MSG_INIT_BOOTOM_TOOL_BAR = 3;
    private static final int MSG_AUTO_LINK = 2;

    private PopupMenu mAttachPopu;
    private PopupMenu mCameraPopu;
    private PopupMenu mOverflowPopu;
    
    private NoteEditText mEtContent;
    
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
    
    private NoteManager mNoteManager;
    
    private NoteInfo mNote;
    private boolean mIsEnterLine;

    private AutoLnkTask mAutoLinkTask;

    private Handler mHandler = new MyHandler(this);
    
    //文件夹id
    private String folderId;

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
        mNoteManager = NoteManager.getInstance();
        Intent intent = getIntent();
        if (intent != null) {
            int noteId = intent.getIntExtra(ARG_NOTE_ID, 0);
            folderId = intent.getStringExtra(ARG_FOLDER_ID);
            if (noteId > 0) {
                loadNoteInfo(noteId);
            }
        }
    }

    /**
     * 显示笔记到界面上
     * @param note
     */
    private void showNote(NoteInfo note) {
        mEtContent.setText(note.getContent());
    }
    
    /**
     * 根据noteid获取note内容
     * @author huanghui1
     * @update 2016/6/18 15:04
     * @version: 1.0.0
     */
    private void loadNoteInfo(final int noteId) {
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                mNote = mNoteManager.getNote(noteId);
                if (mNote != null) {
                    mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
                }
            }
        });
    }

    /**
     * 保存笔记
     * @return
     */
    private void saveNote() {
        String content = TextUtils.isEmpty(mEtContent.getText()) ? "" : mEtContent.getText().toString();
        Intent intent = null;
        if (mNote != null) {    //更新笔记
            mNote.setContent(content);
            mNote.setModifyTime(System.currentTimeMillis());
            mNote.setSyncState(SyncState.SYNC_UP);
            intent = new Intent();
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_UPDATE_NOTE);
        } else if (!TextUtils.isEmpty(content)) {    //添加笔记
            intent = new Intent();
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_ADD_NOTE);
            long time = System.currentTimeMillis();
            mNote.setContent(content);
            mNote.setModifyTime(time);
            mNote.setCreateTime(time);
            mNote.setFolderId(folderId);
            mNote.setHash(DigestUtil.md5Digest(content));
            mNote.setKind(NoteInfo.NoteKind.TEXT);
            mNote.setSId(SystemUtil.generateNoteSid());
            int userId = getCurrentUserId();
            if (userId > 0) {
                mNote.setUserId(userId);
            }
            mNote.setSyncState(SyncState.SYNC_UP);
        }
        if (intent != null) {
            intent.putExtra(Constants.ARG_CORE_OBJ, mNote);
            startService(intent);
        }
    }

    @Override
    protected void initView() {
        mEtContent = (NoteEditText) findViewById(R.id.et_content);
        mHandler.sendEmptyMessage(MSG_INIT_BOOTOM_TOOL_BAR);

        mEtContent.addTextChangedListener(this);

        mEtContent.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //回车换行
                    mIsEnterLine = true;
                    //光标所在行
                    int selectionStart = v.getSelectionStart();
                    String text = v.getText().toString();
                    //光标所在行的开头到光标处的文本
                    String selectionLineBeforeText = getSelectionLineBeforeText(text, selectionStart);
                    int tagLength = Constants.FORMAT_LIST_TAG_LENGTH;
                    if (Constants.TAG_FORMAT_LIST.equals(selectionLineBeforeText)) {    //清除格式列表
                        String endText = getSelectionLineEndText(text, selectionStart);
                        if (endText.length() == 0) {   //该行只有“- ”，则删除
                            Editable editable = v.getEditableText();
                            int start = selectionStart - tagLength;
                            try {
                                mIsEnterLine = false;
                                editable.delete(start, selectionStart);
                                return true;
                            } catch (Exception e) {
                                Log.e(e.getMessage());
                            }
                        }
                    }
                }
                return false;
            }
        });
        mEtContent.setOnSelectionChangedListener(new NoteEditText.SelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selStart, int selEnd) {
                CharSequence text = mEtContent.getText();
                ClickableSpan[] links = ((Spannable) text).getSpans(selStart,
                        selEnd, ClickableSpan.class);
                if (links != null && links.length > 0) {
                    ClickableSpan clickableSpan = links[0];
                    Log.d(clickableSpan.toString());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        saveNote();
        super.onDestroy();
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
                        if (attachPopu != null) {
                            Menu menu = attachPopu.getMenu();

                            MenuItem shareItem = menu.findItem(R.id.action_share);
                            setMenuTint(shareItem, 0);

                            MenuItem searchItem = menu.findItem(R.id.action_search);
                            setMenuTint(searchItem, 0);

                            MenuItem deleteItem = menu.findItem(R.id.action_delete);
                            setMenuTint(deleteItem, 0);

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

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mEtContent.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ABOVE, toolContainer.getId());

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
            case R.id.iv_time:  //当前的时间
                insertTime();
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
        if (mIsEnterLine) {  //手动按的回车键
            mIsEnterLine = false;
            int selectionStart = mEtContent.getSelectionStart();

            //回车前的光标索引
            int previousIndex = selectionStart - 1;
            String lineBeforeText = getSelectionLineBeforeText(s.toString(), previousIndex);
            if (lineBeforeText.startsWith(Constants.TAG_FORMAT_LIST)) {   //上一行有“- ”，则本行继续添加
                s.insert(selectionStart, Constants.TAG_FORMAT_LIST);
            }
        }

        autoLink();
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
        String text = mEtContent.getText().toString();
        Editable editable = mEtContent.getEditableText();
        //光标的开始位置
        int selectionStart = mEtContent.getSelectionStart();
        //光标所在位置前面的文字
        String beforeText = text.substring(0, selectionStart);
        String subS = text.substring(0, selectionStart);
        //光标所在行的第一位
        int startIndex = subS.lastIndexOf("\n") + 1;
        subS = subS.substring(startIndex);
        if (subS.startsWith("- ")) {
            int end = startIndex + "- ".length();
            editable.delete(startIndex, end);
            mIsFormatList = false;
        } else {
            editable.insert(startIndex, "- ");
        }
        int lineStart = beforeText.lastIndexOf(Constants.TAG_ENTER) + 1;
        //光标后面的文字
        String endText = text.substring(selectionStart);
        //光标后面文字的第一个回车的索引
        int lineEnd = endText.indexOf(Constants.TAG_ENTER);
        //光标所在行的文字
        String lineText = null;
        if (lineEnd != -1) {    //光标后面的文字有回车换行
            String linEndText = endText.substring(0, lineEnd);
            String lineBeforeText = beforeText.substring(lineStart);
            lineText = lineBeforeText + linEndText;
        } else {
            lineText = text.substring(lineStart);
        }
        if (lineText.startsWith(Constants.TAG_FORMAT_LIST)) {  //之前有“- ”,则删除
            int end = lineStart + Constants.FORMAT_LIST_TAG_LENGTH;
            editable.delete(lineStart, end);
        } else {
            editable.insert(lineStart, Constants.TAG_FORMAT_LIST);
        }
    }

    /**
     * 在光标处插入当前时间
     * @author tiger
     * @update 2016/3/13 10:27
     * @version 1.0.0
     */
    private void insertTime() {
        String time = SystemUtil.getFormatTime();
        int selectionStart = mEtContent.getSelectionStart();
        Editable editable = mEtContent.getEditableText();
        editable.insert(selectionStart, time);
    }

    /**
     * 自动链接文本
     * @author tiger
     * @update 2016/3/13 10:46
     * @version 1.0.0
     */
    private void autoLink() {
        mHandler.removeMessages(MSG_AUTO_LINK);
        mHandler.sendEmptyMessage(MSG_AUTO_LINK);
    }
    
    /**
     * 获取光标所在行的开头到光标处的文本
     * @author Administrator
     * @update 2016/3/12 14:30
     * @version: 1.0.0
    */
    private String getSelectionLineBeforeText(String text, int selectionStart) {
        String beforeText = text.substring(0, selectionStart);
        int lineStart = beforeText.lastIndexOf(Constants.TAG_ENTER) + 1;
        return beforeText.substring(lineStart);
    }

    /**
     * 获取光标所在行的光标处到该行结尾的文本
     * @author Administrator
     * @update 2016/3/12 22:29
     * @version: 1.0.0
    */
    private String getSelectionLineEndText(String text, int selectionStart) {
        String endText = text.substring(selectionStart);
        int lineEnd = endText.indexOf(Constants.TAG_ENTER);
        if (lineEnd != -1) {    //有回车换行，则截取
            endText = endText.substring(0, lineEnd);
        }
        return endText;
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
    
    /**
     * 自动识别链接的任务
     * @author tiger
     * @update 2016/3/13 10:41
     * @version 1.0.0
     */
    class AutoLnkTask implements Runnable {

        @Override
        public void run() {
            Linkify.addLinks(mEtContent, mEtContent.getAutoLinkMask());
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
                case Constants.MSG_SUCCESS:
                    if (activity.mNote != null) {
                        activity.showNote(activity.mNote);
                    }
                case MSG_AUTO_LINK: //自动链接
                    if (activity.mAutoLinkTask == null) {
                        activity.mAutoLinkTask = activity.new AutoLnkTask();
                    }
                    post(activity.mAutoLinkTask);
                    break;
            }
            super.handleMessage(msg);
        }
    }
    
}
