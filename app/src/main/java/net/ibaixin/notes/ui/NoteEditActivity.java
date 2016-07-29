package net.ibaixin.notes.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.ibaixin.notes.R;
import net.ibaixin.notes.cache.FolderCache;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.ContentObserver;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.edit.recorder.AudioRecorder;
import net.ibaixin.notes.listener.SimpleAttachAddCompleteListener;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.DetailList;
import net.ibaixin.notes.model.EditStep;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.model.SyncState;
import net.ibaixin.notes.persistent.AttachManager;
import net.ibaixin.notes.persistent.NoteManager;
import net.ibaixin.notes.richtext.AttachResolver;
import net.ibaixin.notes.richtext.AttachSpec;
import net.ibaixin.notes.richtext.RichTextWrapper;
import net.ibaixin.notes.service.CoreService;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.DigestUtil;
import net.ibaixin.notes.util.FileUtil;
import net.ibaixin.notes.util.ImageUtil;
import net.ibaixin.notes.util.NoteLinkify;
import net.ibaixin.notes.util.NoteUtil;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.TimeUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.AttachSpan;
import net.ibaixin.notes.widget.MessageBundleSpan;
import net.ibaixin.notes.widget.NoteEditText;
import net.ibaixin.notes.widget.NoteFrameLayout;
import net.ibaixin.notes.widget.NoteLinkMovementMethod;
import net.ibaixin.notes.widget.NoteTextView;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final String ARG_OPT_DELETE = "opt_delete";
    
    private static final int MSG_AUTO_LINK = 2;
    private static final int MSG_INIT_BOOTOM_TOOL_BAR = 3;
    
    public static final int REQ_PICK_IMAGE = 10;
    public static final int REQ_TAKE_PIC = 11;
    public static final int REQ_PAINT = 12;
    public static final int REQ_PICK_FILE = 13;
    public static final int REQ_EDIT_PAINT = 14;

    private PopupMenu mAttachPopu;
    private PopupMenu mOverflowPopu;
    private PopupMenu mOverflowViewPopu;

    //笔记的编辑框
    private NoteEditText mEtContent;

    //笔记的显示视图
    private NoteTextView mTvContent;

    private NoteFrameLayout mContentLayout;

    private TextView mToolbarTitleView;

    /**
     * 编辑步骤的记录容器
     */
    private Stack<EditStep> mUndoStack = new Stack<>();
    private Stack<EditStep> mRedoStack = new Stack<>();

    private View mIvRedo;
    private View mIvUndo;
    private ImageButton mIvSoft;
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

    private AutoLinkTask mAutoLinkTask;

    private Handler mHandler = new MyHandler(this);
    
    //文件夹id
    private String mFolderId;

    /**
     * 是否是阅读模式
     */
    private boolean mIsViewMode;
    
    private View mBottomBar;

    /**
     * 富文本的包装器
     */
    private RichTextWrapper mRichTextWrapper;

    /**
     * 附件的临时缓存
     */
    private Map<String, Attach> mAttachCache;

    //是否有删除笔记的操作，第一次操作则有删除提示，后面则没有了
    private boolean mHasDeleteOpt;

    //笔记的监听器
    private NoteContentObserver mNoteObserver;

    /**
     * 指定的相机拍照的文件
     */
    private File mAttachFile;

    /**
     * 录音器
     */
    private AudioRecorder mAudioRecorder;

    /**
     * 标题
     */
    private CharSequence mTitle;

    private RecyclerView mRecyclerView;

    private void setCustomTitle(CharSequence title, int iconResId) {
        if (!TextUtils.isEmpty(title)) {
            if (mToolbarTitleView == null) {
                LayoutInflater inflater = LayoutInflater.from(this);
                mToolbarTitleView = (TextView) inflater.inflate(R.layout.edit_custom_title, null);
                mToolbarTitleView.setOnClickListener(this);
            }
            if (mToolBar != mToolbarTitleView.getParent()) {
                mToolBar.addView(mToolbarTitleView);
            }
            mToolbarTitleView.setText(title);
            if (iconResId != 0) {
                mToolbarTitleView.setCompoundDrawablesWithIntrinsicBounds(iconResId, 0, 0, 0);
            } else {
                mToolbarTitleView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        } else {
            if (mToolbarTitleView != null) {
                mToolBar.removeView(mToolbarTitleView);
            }
        }
    }

    @Override
    protected void updateToolBar(Toolbar toolbar) {
        CharSequence title = getTitle();
        
        if (mFolderId != null) {
            Folder folder = FolderCache.getInstance().getCacheFolder(mFolderId);
            if (folder != null) {
                title = folder.getName();
            }
        }
        mTitle = title;
        setTitle(null);
        setCustomTitle(title, 0);
    }

    /**
     * 更新标题
     */
    private void updateTitle() {
        setCustomTitle(mTitle, 0);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_note_edit;
    }

    @Override
    protected void initData() {
        mNoteManager = NoteManager.getInstance();
        registContentObserver();
        Intent intent = getIntent();
        if (intent != null) {
            int noteId = intent.getIntExtra(ARG_NOTE_ID, 0);
            mFolderId = intent.getStringExtra(ARG_FOLDER_ID);
            mHasDeleteOpt = intent.getBooleanExtra(ARG_OPT_DELETE, true);
            if (noteId > 0) {   //查看模式
                mNote = new NoteInfo();
                mNote.setId(noteId);
                mIsViewMode = true;
//                initNoteMode(mEtContent, true);
                loadNoteInfo(noteId);
            } else {    //编辑模式
//                initNoteMode(mEtContent, false);

                mContentLayout.changeToEditMode();
                mRichTextWrapper.setRichSpan(mEtContent);
                initEditText(mEtContent);
                changeNoteMode(true);

                generateDetailList();
            }
        }
    }

    /**
     * 是否显示键盘，初始化时
     * @param show
     */
    private void initSoftInputMode(boolean show) {
        if (show) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    /**
     * 切换笔记到编辑模式或者阅读模式
     * @param editable 是否可编辑，如果进入编辑模式，则显示顶部菜单和底部菜单
     */
    private void changeNoteMode(boolean editable) {
        if (editable) {
            if (isViewMode()) {
                mOverflowViewPopu = null;
            }
//            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            if (mBottomBar != null) {
                return;
            }
            mHandler.sendEmptyMessage(MSG_INIT_BOOTOM_TOOL_BAR);
        }
    }

    /**
     * 是否是阅读模式
     * @return 是否是阅读模式
     */
    private boolean isViewMode() {
        return mIsViewMode;
    }

    /**
     * 初始化笔记的模式，是编辑模式还是查看模式
     * @param showMode 是否是查看模式
     */
    private void initNoteMode(NoteEditText editText, boolean showMode) {
        if (showMode) { //查看模式
            initSoftInputMode(false);
            editText.setCursorVisible(false);
        } else {    //编辑模式
            editText.setCursorVisible(true);
            initSoftInputMode(true);
        }
    }

    /**
     * 设置菜单是否可见
     * @param visible 是否可见
     */
    private void setMenuVisible(Menu menu, boolean visible) {
        if (menu == null && mToolBar != null) {
            menu = mToolBar.getMenu();
        }
        if (menu == null) {
            return;
        }
        if (visible) {
            menu.setGroupVisible(R.id.group_edit_type, true);
            menu.setGroupVisible(R.id.group_edit, false);
            /*MenuItem menuItem = menu.findItem(R.id.group_edit_type);
            if (menuItem != null && !menuItem.isVisible()) {
                menuItem.setVisible(true);
            }
            //隐藏编辑菜单
            menuItem = menu.findItem(R.id.group_edit);
            if (menuItem != null && menuItem.isVisible()) {
                menuItem.setVisible(false);
            }*/
        } else {
            menu.setGroupVisible(R.id.group_edit_type, false);
            menu.setGroupVisible(R.id.group_edit, true);
            //显示编辑菜单
            /*MenuItem menuItem = menu.findItem(R.id.group_edit);
            if (menuItem != null && !menuItem.isVisible()) {
                menuItem.setVisible(true);  
            }*/
        }
    }

    /**
     * 显示笔记到界面上
     * @param note
     */
    private void showNote(NoteInfo note) {
        CharSequence s = note.getContent();
        mRichTextWrapper.setText(s, mAttachCache);
        if (mRichTextWrapper.getRichSpan() instanceof NoteTextView) {
            autoLink(s);
        }
    }
    
    /**
     * 根据noteid获取note内容
     * @author huanghui1
     * @update 2016/6/18 15:04
     * @version: 1.0.0
     */
    private void loadNoteInfo(final int noteId) {
        doInbackground(new Runnable() {
            @Override
            public void run() {
                mNote = mNoteManager.getNote(noteId);
                if (mNote != null) {
                    mAttachCache = mNote.getAttaches();
                    mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
                }
            }
        });
    }

    /**
     * 获取笔记的sid,如果没有，则生成
     * @return
     */
    private String getNoteSid() {
        String sid = null;
        if (mNote == null) {
            mNote = new NoteInfo();
            sid = SystemUtil.generateNoteSid();
            mNote.setSId(sid);
        } else {
            sid = mNote.getSId(); 
        }
        return sid;
    }

    /**
     * 保存笔记
     * @param removeAttach　是否移除缓存中的附件数据库记录
     * @return
     */
    private void saveNote(boolean removeAttach) {
        String content = TextUtils.isEmpty(mEtContent.getText()) ? "" : mEtContent.getText().toString();
        if (TextUtils.isEmpty(content) && removeAttach) {
            if (mNote == null || mNote.isEmpty()) {
                removeCacheAttach(true);    //移除缓存中的附件，彻底删除数据库记录
            } else {
                removeCacheAttach(false);    //移除缓存中的附件，彻底删除数据库记录
            }
            return;
        }

        Intent intent = null;
        if (mNote != null && !mNote.isEmpty()) {    //更新笔记
            intent = new Intent(mContext, CoreService.class);
            if (content.equals(mNote.getContent())) {   //内容相同，没有修改
                //则只检测是否有多余的附件记录，有，则删除
                intent.putExtra(Constants.ARG_SUB_OBJ, false);
            }
            mNote.setContent(content);
            mNote.setModifyTime(System.currentTimeMillis());
            mNote.setSyncState(SyncState.SYNC_UP);
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_UPDATE_NOTE);
        } else {    //添加笔记
            if (mNote == null) {
                mNote = new NoteInfo();
            }
            intent = new Intent(mContext, CoreService.class);
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_ADD_NOTE);
            long time = System.currentTimeMillis();
            mNote.setContent(content);
            mNote.setModifyTime(time);
            mNote.setCreateTime(time);
            mNote.setFolderId(mFolderId);
            mNote.setHash(DigestUtil.md5Digest(content));
            mNote.setKind(NoteInfo.NoteKind.TEXT);
            if (mNote.getSId() == null) {
                mNote.setSId(SystemUtil.generateNoteSid());
            }
            int userId = getCurrentUserId();
            if (userId > 0) {
                mNote.setUserId(userId);
            }
            mNote.setSyncState(SyncState.SYNC_UP);
        }
        
        intent.putExtra(Constants.ARG_CORE_OBJ, mNote);
        if (mAttachCache != null && mAttachCache.size() > 0) {
            ArrayList<String> list = new ArrayList<>();
            list.addAll(mAttachCache.keySet()); //将附件的sid传入，不论附件是否在笔记中
            intent.putStringArrayListExtra(Constants.ARG_CORE_LIST, list);
        }
        startService(intent);
    }

    /**
     * 移除缓存中的附件，彻底删除数据库记录
     * @param deleteParent 是否删除父类目录
     */
    private void removeCacheAttach(boolean deleteParent) {
        if (mAttachCache != null && mAttachCache.size() > 0) {
            Intent intent = new Intent(mContext, CoreService.class);
            ArrayList<Attach> list = new ArrayList<>();
            list.addAll(mAttachCache.values()); //将附件的sid传入，不论附件是否在笔记中
            intent.putParcelableArrayListExtra(Constants.ARG_CORE_LIST, list);
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_REMOVE_NOTE_ATTACH);
            if (deleteParent) {
                try {
                    String dir = SystemUtil.getAttachPath(getNoteSid(), 0, false);
                    intent.putExtra(Constants.ARG_SUB_OBJ, dir);
                } catch (IOException e) {
                    Log.e(TAG, "--removeCacheAttach---getAttachPath--error--" + e.getMessage());
                    e.printStackTrace();
                }
            }
            startService(intent);
            mAttachCache.clear();
        }
    }

    @Override
    protected void initView() {
        mEtContent = (NoteEditText) findViewById(R.id.et_content);
        mTvContent = (NoteTextView) findViewById(R.id.tv_content);

        if (mTvContent == null || mEtContent == null) {
            return;
        }

        mContentLayout = (NoteFrameLayout) findViewById(R.id.content_layout);

//        mRichTextWrapper = new RichTextWrapper(mEtContent, mHandler);
        mRichTextWrapper = new RichTextWrapper(mTvContent, mHandler);
        mRichTextWrapper.setMovementMethod(NoteLinkMovementMethod.getInstance());
        mRichTextWrapper.addResolver(AttachResolver.class);

        //初始化文本显示控件
        initTextView();

        //初始化编辑框
        initEditText(mEtContent);
    }

    /**
     * 初始化文本显示
     */
    private void initTextView() {
        mContentLayout.setOnItemClickListener(this);
    }

    /**
     * 初始化编辑框
     * @param editText
     */
    private void initEditText(final NoteEditText editText) {
        editText.addTextChangedListener(this);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        editText.setOnSelectionChangedListener(new NoteEditText.SelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selStart, int selEnd) {
                CharSequence text = editText.getText();
                ClickableSpan[] links = ((Spannable) text).getSpans(selStart,
                        selEnd, ClickableSpan.class);
                if (links != null && links.length > 0) {
                    ClickableSpan clickableSpan = links[0];
                    Log.d(TAG, "----onSelectionChanged--clickableSpan---" + clickableSpan.toString());
                } else {
                    AttachSpan[] images = ((Spannable) text).getSpans(selStart,
                            selEnd, AttachSpan.class);
                    if (images != null && images.length > 0) {
                        Log.d(TAG, "----onSelectionChanged---AttachSpan--");
                    }
                }
            }
        });

//        editText.setMovementMethod(NoteLinkMovementMethod.getInstance());
//        editText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(final View v) {
//                Log.d(TAG, "----setOnClickListener-----");
//                if (isViewMode()) { //之前是阅读模式
//                    setupEditMode((EditText) v, false);
//                } else {    //编辑模式
//                    changeNoteMode(true);
//                }
//            }
//        });
    }

    /**
     * 进入编辑模式
     * @param showSoftInput 是否显示软键盘
     */
    private void setupEditMode(NoteEditText editText, boolean showSoftInput) {
        /*initSoftInputMode(true);
        if (showSoftInput) {    //显示软键盘
            editText.requestFocus();
            SystemUtil.showSoftInput(mContext, editText);
            if (editText.getText() != null) {
                editText.setSelection(editText.getText().length());
            }
        }*/
        
        //显示光标
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (!editText.isCursorVisible()) {
                editText.setCursorVisible(true);
            }
        } else {
            editText.setCursorVisible(true);
        }*/
        mIsViewMode = false;

        mContentLayout.changeToEditMode();

        mRichTextWrapper.setRichSpan(editText);

        //显示菜单
        setMenuVisible(null, true);

//        initEditText(editText);

        showNote(mNote);

        editText.setSelection(editText.getText().length());

        SystemUtil.showSoftInput(mContext, editText);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                changeNoteMode(true);
            }
        }, 100);
        
    }

    @Override
    protected void onPause() {
        stopRecorder();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregistContentObserver();
        if (!isViewMode()) {
            saveNote(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_edit, menu);
        MenuItem item = menu.findItem(R.id.action_more);
        MenuItem viewItem = menu.findItem(R.id.action_view_more);
        setMenuOverFlowTint(item, viewItem);
        
        if (isViewMode()) {
            setMenuVisible(menu, false);
        } else {
            setMenuVisible(menu, true);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View attachView = null;
        switch (item.getItemId()) {
            case R.id.action_save:  //保存
                saveNote(false);
                break;
            case R.id.action_photo: //图片
                SystemUtil.choseImage((Activity) mContext, REQ_PICK_IMAGE);
                break;
            case R.id.action_attach:    //添加附件
                attachView = getToolBarMenuView(R.id.action_attach);
                mAttachPopu = createPopuMenu(attachView, mAttachPopu, R.menu.edit_attach, true);
                MenuItem brushItem = mAttachPopu.getMenu().findItem(R.id.action_brush);
                setMenuTint(brushItem, getResources().getColor(R.color.colorPrimary));
                break;
            case R.id.action_more:  //更多
                attachView = getToolBarMenuView(R.id.action_more);
                mOverflowPopu = showEditActionMore(attachView, mOverflowPopu);

                break;
            case R.id.action_edit:  //进入编辑模式
                if (isViewMode()) { //之前是阅读模式
                    setupEditMode(mEtContent, true);
                }
                break;
            case R.id.action_view_more: //查看模式下的更多菜单
                if (isViewMode()) { //之前是阅读模式
                    attachView = getToolBarMenuView(R.id.action_view_more);
                    mOverflowViewPopu = showViewActionMore(attachView, mOverflowViewPopu);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示编辑状态下的更多菜单
     * @param attachView 点击的view
     */
    private PopupMenu showEditActionMore(View attachView, PopupMenu popupMenu) {
        final PopupMenu tempPopu = createPopuMenu(attachView, popupMenu, R.menu.edit_overflow, false);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tempPopu != null) {
                    Menu menu = tempPopu.getMenu();
                    
                    if (mNote == null) {    //没有详情
                        menu.removeItem(R.id.action_info);
                    }
                    
                    MenuItem shareItem = menu.findItem(R.id.action_share);
                    setMenuTint(shareItem, 0);

                    MenuItem searchItem = menu.findItem(R.id.action_search);
                    setMenuTint(searchItem, 0);

                    MenuItem deleteItem = menu.findItem(R.id.action_delete);
                    setMenuTint(deleteItem, 0);

                    tempPopu.show();
                }
            }
        });
        return tempPopu;
    }

    /**
     * 显示查看状态下的更多菜单
     * @param attachView
     */
    private PopupMenu showViewActionMore(View attachView, PopupMenu popupMenu) {
        final PopupMenu tempPopu = createPopuMenu(attachView, popupMenu, R.menu.edit_view_overflow, false);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (tempPopu != null) {
                    Menu menu = tempPopu.getMenu();

                    MenuItem shareItem = menu.findItem(R.id.action_share);
                    setMenuTint(shareItem, 0);

                    MenuItem deleteItem = menu.findItem(R.id.action_delete);
                    setMenuTint(deleteItem, 0);

                    tempPopu.show();
                }
            }
        });
        return tempPopu;
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
                popupMenu = createPopMenu(aucher, menuResId, true, new OnPopMenuItemClickListener());
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
        if (mBottomBar != null) {
            if (mBottomBar.getVisibility() != View.VISIBLE) {
                mBottomBar.setVisibility(View.VISIBLE);
            }
            return;
        }
        ViewStub viewStub = (ViewStub) findViewById(R.id.bottom_tool_bar);
        if (viewStub == null) {
            return;
        }
        mBottomBar = viewStub.inflate();
        ViewGroup toolContainer = (ViewGroup) mBottomBar.findViewById(R.id.tool_container);

        ScrollView contentLayout = (ScrollView) findViewById(R.id.content_scroll);
        
        if (contentLayout == null) {
            return;
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) contentLayout.getLayoutParams();
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
                case R.id.iv_down:
                    mIvSoft = (ImageButton) child;
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
            case R.id.iv_down:  //隐藏/显示软键盘
                if (mBottomBar.getVisibility() == View.VISIBLE) {   //隐藏软键盘
                    SystemUtil.hideSoftInput(mContext, mEtContent);
                }
                break;
            case R.id.toolbar_title:    //自定义标题的点击事件
                stopRecorder();
                break;
            case R.id.tv_content:   //阅读模式的文本点击事件
                setupEditMode(mEtContent, false);
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
        if (s == null) {
            return;
        }
        if (!mIsDo) {
            EditStep editStep = getUndo();
            if (editStep != null && editStep.isAppend()) {  //添加文字
                int end = start + count;
                editStep.setEnd(end);
                editStep.setContent(s.subSequence(start, end));
                editStep.setStart(start);
                editStep.setLength(count);
            }
        }
        autoLink(s);
    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.d(TAG, "--afterTextChanged--");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_PICK_IMAGE:    //选择图片的结果
                    if (data != null) {
                        final Uri uri = data.getData();
                        handleShowImage(uri, false);
                    }
                    break;
                case REQ_TAKE_PIC:  //拍照，获取缩略图，并保存到本地
                    if (mAttachFile != null) {
                        //file uri
                        Uri fileUri = Uri.fromFile(mAttachFile);
                        handleShowImage(fileUri, true);
                    }
                    break;
                case REQ_PAINT: //画图
                    if (data != null) {
                        final Uri uri = data.getData();
                        handleShowImage(uri, Attach.PAINT, false);
                    } else {
                        Log.d(TAG, "---onActivityResult---REQ_PAINT---data---is----null--");
                    }
                    break;
                case REQ_EDIT_PAINT:    //编辑绘画
                    if (data != null) {
                        AttachSpec attachSpec = data.getParcelableExtra(Constants.ARG_CORE_OBJ);
                        handleUpdateImage(attachSpec);
                    } else {
                        Log.d(TAG, "---onActivityResult---REQ_EDIT_PAINT---data---is----null--");
                    }
                    break;
                case REQ_PICK_FILE: //选择文件，不限格式
                    if (data != null) {
                        final Uri uri = data.getData();
                        Log.d(TAG, "--onActivityResult---req_pick_file---uri--" + uri);
                        handleShowAttach(uri);
                    } else {
                        Log.d(TAG, "---onActivityResult---req_pick_file---data---is----null--");
                    }
                    break;
            }
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 显示选择的或者拍照后的图拍呢
     * @param uri 图片的地址
     * @param compressImg 是否需要压缩图片   
     */
    private void handleShowImage(final Uri uri, final boolean compressImg) {
        handleShowImage(uri, Attach.IMAGE, compressImg);
    }

    /**
     * 显示选择的或者拍照后的图拍呢
     * @param uri 图片的地址
     * @param compressImg 是否需要压缩图片   
     * @param attachType 附件的类型                   
     */
    private void handleShowImage(final Uri uri, final int attachType, final boolean compressImg) {
        doInbackground(new Runnable() {
            @Override
            public void run() {
                String filePath = SystemUtil.getFilePathFromContentUri(uri.toString(), mContext);
                Log.d(TAG, "addImage----" + filePath);
                
                if (compressImg) {
                    //压缩并保存文件
                    String savePath = filePath + ".tmp";
                    boolean success = ImageUtil.generateThumbImage(filePath, savePath, false);

                    if (success) {
                        File file = new File(savePath);
                        File renameFile = new File(filePath);
                        if (renameFile.exists()) {
                            renameFile.delete();
                        }
                        file.renameTo(renameFile);
                        
                        //添加到相册
                        SystemUtil.galleryAddPic(mContext, file);
                    }
                }
                
                Attach attach = getAddedAttach(filePath);
                if (attach == null) {
                    File file = new File(filePath);
                    attach = file2Attach(file, attachType);
                }
                mEtContent.addImage(attach, new SimpleAttachAddCompleteListenerImpl(true));
            }
        });
    }

    /**
     * 将文件转换成附件
     * @param file
     * @param attachType
     * @return
     */
    private Attach file2Attach(File file, int attachType) {
        String mimeType = FileUtil.getMimeType(file);
        String filePath = file.getAbsolutePath();
        if (attachType == 0) {
            attachType = SystemUtil.guessFileType(filePath, mimeType);
        }
        Attach attach = new Attach();
        attach.setType(attachType);
        attach.setSId(SystemUtil.generateAttachSid());
        attach.setLocalPath(filePath);
        attach.setFilename(file.getName());
        attach.setSize(file.length());
        attach.setNoteId(getNoteSid());
        attach.setMimeType(mimeType);
        
        return attach;
    }

    /**
     * 处理图片的更新，主要是绘画
     * @param attachSpec
     */
    private void handleUpdateImage(AttachSpec attachSpec) {
        mRichTextWrapper.getRichSpan().showImage(attachSpec, new SimpleAttachAddCompleteListenerImpl(false));
    }

    /**
     * 显示选择的附件
     * @param uri
     */
    private void handleShowAttach(final Uri uri) {
        doInbackground(new Runnable() {
            @Override
            public void run() {
                String filePath = SystemUtil.getFilePathFromContentUri(uri.toString(), mContext);
                if (TextUtils.isEmpty(filePath)) {
                    Log.d(TAG, "--handleShowAttach---filePath--is---empty--");
                    return;
                }
                File file = new File(filePath);
                Attach attach = getAddedAttach(filePath);
                if (attach != null) {   //该文件已添加过
                    mEtContent.addAttach(attach, null);
                } else {
                    attach = file2Attach(file, 0);
                    
                    mEtContent.addAttach(attach, new SimpleAttachAddCompleteListenerImpl(true));
                }
                Log.d(TAG, "handleShowAttach----" + filePath);
            }
        });
    }

    /**
     * 打开相机拍照
     */
    public void openCamera(File file) throws IOException {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//create a intent to take picture 
        //create a intent to take picture  
        Uri uri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri); // set the image file name 
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQ_TAKE_PIC);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 注册观察者的监听
     * @author huanghui1
     * @update 2016/3/9 18:10
     * @version: 1.0.0
     */
    private void registContentObserver() {
        mNoteObserver = new NoteContentObserver(mHandler);
        NoteManager.getInstance().addObserver(mNoteObserver);
    }

    /**
     * 注销观察者的监听
     */
    private void unregistContentObserver() {
        if (mNoteObserver != null) {
            NoteManager.getInstance().removeObserver(mNoteObserver);
        }
    }

    /**
     * 添加回退操作
     * @author tiger
     * @update 2016/3/5 9:09
     * @version 1.0.0
     */
    private void pushUndo(EditStep editStep) {
        if (mIvUndo == null) {
            return;
        }
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
        if (mUndoStack.size() == 0) {
            return null;
        }
        return mUndoStack.peek();
    }

    /**
     * 添加前进操作
     * @author tiger
     * @update 2016/3/5 9:11
     * @version 1.0.0
     */
    private void pushRedo(EditStep editStep) {
        if (mIvRedo == null) {
            return;
        }
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
            boolean addRedo = true;
            try {
                if (editStep.isAppend()) {  //之前的操作是插入文字，则此时的操作是删除文字
                    if (canDeleteText(editable, start)) {
                        if (end > editable.length()) {  //要删除的文字太多，则直接删除到结尾即可
                            end = editable.length();
                            editStep.setEnd(end);
                        }
                        editable.delete(start, end);
                    } else {
                        addRedo = false;
                        Log.d(TAG, "----addRedo---false---not--able---to---delete---");
                    }
                } else {    //之前的操作是删除文字，则此时的操作是插入文字String sid = mEtContent.getAttachSid(content);
                    String sid = SystemUtil.getAttachSid(content);
                    if (sid != null && mAttachCache != null && mAttachCache.get(sid) != null) {  //有附件
                        Attach attach = mAttachCache.get(sid);
                        resetAttach(editable, editStep, attach);
                    } else {
                        editable.insert(start, content);
                    }
                }
            } catch (Exception e) {
                addRedo = false;
                Log.e(TAG, "----undo--error---" + e.getMessage());
            }
            setdo(false);
            if (addRedo) {
                editStep.setContent(content);
                pushRedo(editStep);
            }
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
        boolean addUndo = true;
        try {
            if (editStep.isAppend()) {  //前进操作是插入文字
                String sid = SystemUtil.getAttachSid(content);
                if (sid != null && mAttachCache != null && mAttachCache.get(sid) != null) {  //有附件
                    Attach attach = mAttachCache.get(sid);
                    resetAttach(editable, editStep, attach);
                } else {
                    editable.insert(editStep.getStart(), content);
                }
            } else {    //前进操作是删除文字
                int start = editStep.getStart();
                if (canDeleteText(editable, start)) {
                    int end = editStep.getEnd();
                    if (end > editable.length()) {  //要删除的文字太多，则直接删除到结尾即可
                        end = editable.length();
                        editStep.setEnd(end);
                    }
                    editable.delete(start, end);
                } else {
                    addUndo = false;
                    Log.d(TAG, "----addUndo---false---not--able---to---delete---");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "---redo---error-----" + e.getMessage());
            addUndo = false;
        }
        if (addUndo) {
            pushUndo(editStep);
        }
        
        if (mRedoStack.size() == 0 && mIvRedo.isEnabled()) {
            mIvRedo.setEnabled(false);
        }
        setdo(false);
    }

    /**
     * 是否可以删除文字
     * @param editable
     * @param start
     * @return
     */
    private boolean canDeleteText(Editable editable, int start) {
        return start + 1 <= editable.length();
    }

    /**
     * 回退、前进显示附件
     * @param attach
     */
    private void resetAttach(final Editable editable, final EditStep editStep, final Attach attach) {
        String uri = attach.getAvailableUri();
        int attachType = attach.getType();
        if (uri != null) {
            switch (attachType) {
                case Attach.IMAGE:  //显示图片
                    showSpanImage(editable, null, editStep, uri);
                    break;
                case Attach.PAINT:  //显示绘画
                    ImageSize imageSize = mEtContent.getImageSize(attachType);
                    showSpanImage(editable, imageSize, editStep, uri);
                    break;
                case Attach.VOICE:  //显示语音
                case Attach.ARCHIVE:    //显示压缩包
                case Attach.VIDEO:    //显示视频
                case Attach.FILE:    //显示视频
                    CharSequence text = editStep.getContent();
                    int selStart = editStep.getStart();
                    setdo(true);
                    mEtContent.showSpanAttach(text, selStart, attach);
                    setdo(false);
                    break;
            }
        } else {
            editable.insert(editStep.getStart(), editStep.getContent());
        }
    }

    /**
     * 显示图片的span
     * @param editable
     * @param editStep
     * @param uri
     */
    private void showSpanImage(final Editable editable, ImageSize imageSize, final EditStep editStep, String uri) {
        ImageUtil.generateThumbImageAsync(uri, imageSize, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                ImageSpan imageSpan = new ImageSpan(mContext, loadedImage);
                CharSequence text = editStep.getContent();
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(text);
                builder.setSpan(imageSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                int selStart = editStep.getStart();
                setdo(true);
                try {
                    if (selStart < 0 || mEtContent.getText() == null || selStart >= mEtContent.getText().length()) {
                        editable.append(builder);
                    } else {
                        editable.insert(selStart, builder);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "-----resetAttach--error----" + e.getMessage());
                }
                setdo(false);
//                editable.insert(editStep.getStart(), editStep.getContent());
//                    listener.onAddComplete(imageUri, editStep, attach);
            }
        });
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
        //光标所在行的第一位
        int lineStart = beforeText.lastIndexOf(Constants.TAG_NEXT_LINE) + 1;
        //光标后面的文字
        String endText = text.substring(selectionStart);
        //光标后面文字的第一个回车的索引
        int lineEnd = endText.indexOf(Constants.TAG_NEXT_LINE);
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
    private void autoLink(CharSequence s) {
        mHandler.removeMessages(MSG_AUTO_LINK);
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_AUTO_LINK;
        msg.obj = s;
        mHandler.sendMessage(msg);
    }
    
    /**
     * 获取光标所在行的开头到光标处的文本
     * @author Administrator
     * @update 2016/3/12 14:30
     * @version: 1.0.0
    */
    private String getSelectionLineBeforeText(String text, int selectionStart) {
        String beforeText = text.substring(0, selectionStart);
        int lineStart = beforeText.lastIndexOf(Constants.TAG_NEXT_LINE) + 1;
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
        int lineEnd = endText.indexOf(Constants.TAG_NEXT_LINE);
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

    /**
     * 添加附件
     * @param attach 附件
     */
    private void handleAddAttach(final String filePath, final Object data, final Attach attach) {
        if (mAttachCache == null) {
            mAttachCache = new HashMap<>();
        }
        if (attach == null) {
            Log.d(TAG, "---handleAddAttach---added---not--need--add---");
            return;
        }
        doInbackground(new Runnable() {
            @Override
            public void run() {
                attach.setUri(filePath);

                long time = System.currentTimeMillis();
                attach.setCreateTime(time);
                attach.setModifyTime(time);

                int userId = getCurrentUserId();
                if (userId > 0) {
                    attach.setUserId(userId);
                }
                AttachManager.getInstance().addAttach(attach);
                mAttachCache.put(attach.getSId(), attach);
                Log.d(TAG, "---handleAddAttach--mAttachCache--has---uri--add--");
            }
        });
    }

    /**
     * 更新附件
     * @param attach 附件
     */
    private void handleUpdateAttach(final Attach attach) {
        if (mAttachCache == null) {
            mAttachCache = new HashMap<>();
        }
        if (attach == null) {
            Log.d(TAG, "---handleAddAttach---added---not--need--add---");
            return;
        }
        doInbackground(new Runnable() {
            @Override
            public void run() {
                String filePath = attach.getLocalPath();
                Attach tmpAttach = getAddedAttach(filePath);

                if (tmpAttach == null) {
                    tmpAttach = attach;
                }

                if (filePath != null) {
                    tmpAttach.setUri(filePath);

                    long time = System.currentTimeMillis();
                    tmpAttach.setModifyTime(time);

                    int userId = getCurrentUserId();
                    if (userId > 0) {
                        tmpAttach.setUserId(userId);
                    }

                    tmpAttach.setSize(new File(filePath).length());

                    AttachManager.getInstance().updateAttach(tmpAttach);
                    mAttachCache.put(tmpAttach.getSId(), tmpAttach);
                    Log.d(TAG, "---handleUpdateAttach--mAttachCache--has---uri--update--");
                } else {
                    Log.d(TAG, "--handleUpdateAttach--filePath--is---null--");
                }
            }
        });
    }

    /**
     * 根据uri获取已经添加的附件
     * @param filePath 文件的全路径
     * @return 已添加过的附件
     */
    public Attach getAddedAttach(String filePath) {
        //从缓存中查询，是否已经有该图片了，如果有了，则不需要再存入到数据库了
        if (mAttachCache != null && mAttachCache.size() > 0) {
            for (Attach a : mAttachCache.values()) {
                if (filePath.equals(a.getLocalPath())) {    //与本地路径一样
                    Log.d(TAG, "---handleAddAttach--mAttachCache--has---uri--");
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * 清除文本内容
     * @param deleteNote 是否手动删除笔记，如果是删除笔记操作，则仅仅是清空内容，然后结束该界面
     */
    private void clearContent(boolean deleteNote) {
        if (deleteNote) {
            mEtContent.setText("");
            return;
        }
        //给予清空内容非提示
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.prompt)
                .setMessage(R.string.confirm_clear_content)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mEtContent.setText("");

                        //删除附件
                        removeCacheAttach(false);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        
    }

    /**
     * 保存删除操作的记录
     */
    public void saveDeleteOpt() {
        if (!mHasDeleteOpt) {   //之前是否有删除操作，如果没有，则需保存  
            doInbackground(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(Constants.PREF_HAS_DELETE_OPT, true);
                    editor.apply();
                    mHasDeleteOpt = true;
                }
            });
        }
    }

    /**
     * 从缓存中获取附件
     * @param sid 附件的sid
     * @return
     */
    public Attach getAttach(String sid) {
        if (mAttachCache != null && mAttachCache.size() > 0) {
            return mAttachCache.get(sid);
        }
        return null;
    }

    /**
     * 请求录音的权限，还包含文件写入sd卡的权限
     * @param resultAction
     */
    private void requestPermission(PermissionsResultAction resultAction) {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, resultAction);
    }

    /**
     * 初始化录音器
     */
    private void startRecorder() {
        requestPermission(new PermissionsResultAction() {
            @Override
            public void onGranted() {
                if (mAudioRecorder == null) {
                    mAudioRecorder = new AudioRecorder(mHandler);
                }
                if (mAttachFile == null) {
                    String sid = getNoteSid();
                    try {
                        mAttachFile = SystemUtil.getAttachFile(sid, Attach.VOICE);
                    } catch (IOException e) {
                        Log.e(TAG, "getAttachFile error：" + e.getMessage());
                        e.printStackTrace();
                    }
                    if (mAttachFile == null) {
                        SystemUtil.makeShortToast(R.string.record_error);
                        Log.e(TAG, "getAttachFile error");
                    }
                }
                mAudioRecorder.setFilePath(mAttachFile.getAbsolutePath());
                mAudioRecorder.setRecordListener(new AudioRecorder.OnRecordListener() {
                    @Override
                    public void onBeforeRecord(String filePath) {
                        Log.d(TAG, "----onBeforeRecord--");
                        showRecordView();
                    }

                    @Override
                    public void onRecording(String filePath, long time) {
                        updateRecordTime(time);
                    }

                    @Override
                    public void onEndRecord(String filePath, long time) {
                        if (filePath != null) {
                            File file = new File(filePath);
                            Attach attach = file2Attach(file, Attach.VOICE);
                            attach.setDescription(String.valueOf(time));
                            mEtContent.addAttach(attach, new SimpleAttachAddCompleteListenerImpl(true));
                        } else {
                            mAttachFile = null;
                        }
                        hideRecordView();
                    }

                    @Override
                    public void onRecordError(String filePath, String errorMsg) {
                        Log.d(TAG, "----onRecordError--");
                        hideRecordView();
                        SystemUtil.makeShortToast(R.string.record_error);
                    }
                });
                mAudioRecorder.startRecording();
            }

            @Override
            public void onDenied(String permission) {
                //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
                if (ActivityCompat.shouldShowRequestPermissionRationale(NoteEditActivity.this,
                        permission)) {
                    Toast.makeText(mContext,"please give me the permission",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext,"onDenied--request--",Toast.LENGTH_SHORT).show();
                    //进行权限请求
                    /*ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            EXTERNAL_STORAGE_REQ_CODE);*/
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    /**
     * 停止录音
     */
    private void stopRecorder() {
        if (mAudioRecorder != null) {

            doInbackground(new Runnable() {
                @Override
                public void run() {
                    mAudioRecorder.releaseRecorder();
                    mAudioRecorder = null;
                    Log.d(TAG, "----stopRecorder---record---file---" + mAttachFile);
                }
            });
            /*mAudioRecorder.releaseRecorder();
            mAudioRecorder = null;

            hideRecordView();*/
            
            Log.d(TAG, "----stopRecorder---record---file---" + mAttachFile);
        }
    }

    /**
     * 显示正在录音的试图
     */
    private void showRecordView() {
        SystemUtil.hideSoftInput(mContext, mEtContent);
        if (mToolbarTitleView != null) {
            mToolbarTitleView.setClickable(true);
            setCustomTitle(getString(R.string.init), R.drawable.ic_record_white);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    /**
     * 更新录音时长
     * @param time 录音时长
     */
    private void updateRecordTime(long time) {
        String timeStr = TimeUtil.formatMillis(time);
        if (mToolbarTitleView != null) {
            mToolbarTitleView.setText(timeStr);
        }
    }

    /**
     * 隐藏录音的视图
     */
    private void hideRecordView() {
        if (mToolbarTitleView != null) {
            mToolbarTitleView.setClickable(false);
        }
        updateTitle();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * 添加详情的菜单
     * @param popupMenu
     */
    private void addInfoMenu(PopupMenu popupMenu) {
        if (popupMenu != null) {
            Menu menu = popupMenu.getMenu();
            MenuItem menuItem = menu.add(0, R.id.action_info, 0, getString(R.string.action_info));
            menuItem.setIcon(R.drawable.ic_action_info);
        }
    }

    /**
     * 保存结果的提示语
     * @param isSuccess 是否保存成功
     */
    private void saveResult(boolean isSuccess) {
        int resId = isSuccess ? R.string.update_result_success : R.string.update_result_error;
        SystemUtil.makeShortToast(resId);
    }

    /**
     * 附件加载完成的回调
     */
    class SimpleAttachAddCompleteListenerImpl extends SimpleAttachAddCompleteListener {
        //是添加附件的操作，也有可能只是前进显示图片的操作
        private boolean isAdd;
        
        public SimpleAttachAddCompleteListenerImpl(boolean isAdd) {
            this.isAdd = isAdd;
        }

        @Override
        public void onAddComplete(String uri, Object data, Attach attach) {
            if (isAdd) {
                handleAddAttach(uri, data, attach);
            } else {
                handleUpdateAttach(attach);
            }
        }
    }

    class OnPopMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            String sid = null;
            Intent intent = null;
            switch (item.getItemId()) {
                case R.id.action_camera: //拍照
                    try {
                        sid = getNoteSid();
                        mAttachFile = null;
                        mAttachFile = SystemUtil.getCameraFile(sid);
                        if (mAttachFile == null) {
                            SystemUtil.makeShortToast(R.string.tip_mkfile_error);
                        } else {
                            openCamera(mAttachFile);
                        }
                    } catch (IOException e) {
                        SystemUtil.makeShortToast(R.string.tip_camera_error);
                        Log.e(TAG, "----OnPopMenuItemClickListener---onMenuItemClick----openCamera---error--" + e.getMessage());
                    }
                    break;
                case R.id.action_voice: //添加语音
                    mAttachFile = null;
                    try {
                        startRecorder();
                    } catch (Exception e) {
                        SystemUtil.makeShortToast(R.string.record_error);
                        Log.e(TAG, "---startRecorder--error--" + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case R.id.action_delete:    //删除
                    if (mNote != null) {
                        NoteUtil.handleDeleteNote(mContext, mNote, mHasDeleteOpt);
                    } else {    //清空内容
                        clearContent(false);
                    }
                    break;
                case R.id.action_brush: //涂鸦
                    intent = new Intent(mContext, HandWritingActivity.class);
                    AttachSpec attachSpec = new AttachSpec();
                    attachSpec.noteSid = getNoteSid();
                    attachSpec.attachType = Attach.PAINT;
                    intent.putExtra(Constants.ARG_CORE_OBJ, attachSpec);
                    startActivityForResult(intent, REQ_PAINT);
                    break;
                case R.id.action_file:    //添加附件
                    SystemUtil.choseFile(NoteEditActivity.this, null, REQ_PICK_FILE);
                    break;
                case R.id.action_share:    //分享
                    break;
                case R.id.action_info:    //详情
                    if (mNote != null && !mNote.isEmpty()) {
                        NoteUtil.showInfo(mContext, mNote);
                    }
                    break;
                case R.id.action_detailed_list: //清单与文本之间的切换
                    View view = generateDetailList();
                    if (!(mContentLayout.getChildAt(mContentLayout.getChildCount() - 1) instanceof RecyclerView)) {    //没有添加了
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        view.setLayoutParams(params);
                        params.addRule(RelativeLayout.BELOW, mEtContent.getId());
                        mContentLayout.addView(view, params);
                    } else {
                        Log.d(TAG, "---action_detailed_list----already---added--");
                    }
                    break;
            }
            return false;
        }
    }
    
    private List<DetailList> initDetailList() {
        List<DetailList> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            DetailList detail = new DetailList();
            detail.setTitle("比啊提升人" + i);

            list.add(detail);
        }
        return list;
    }

    /**
     * 创建清单的view
     * @return
     */
    private View generateDetailList() {
        /*if (mRecyclerView == null) {
//            mRecyclerView = new RecyclerView(mContext);
            mRecyclerView = (RecyclerView) findViewById(R.id.detail_list_view);

            List<DetailList> list = initDetailList();
            DetailListAdapter adapter = new DetailListAdapter(mContext, list);
            LayoutManagerFactory mLayoutManagerFactory = new LayoutManagerFactory();
            mRecyclerView.setLayoutManager(mLayoutManagerFactory.getLayoutManager(this, false));
            mRecyclerView.setAdapter(adapter);
        } else {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }*/
        return mRecyclerView;
    }

    /**
     * 清单列表的holder
     */
    class DetailListViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        
        EditText tvTitle;
        public DetailListViewHolder(View itemView) {
            super(itemView);

            checkBox = (CheckBox) itemView.findViewById(R.id.cb_check);
            tvTitle = (EditText) itemView.findViewById(R.id.tv_title);
        }
    }

    /**
     * 清单列表的适配器
     */
    class DetailListAdapter extends RecyclerView.Adapter<DetailListViewHolder> {
        private Context context;
        
        private List<DetailList> list;
        
        private LayoutInflater inflater;

        public DetailListAdapter(Context context, List<DetailList> list) {
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public DetailListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_detail_list, parent, false);
            DetailListViewHolder holder = new DetailListViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(DetailListViewHolder holder, int position) {
            DetailList detail = list.get(position);
            holder.checkBox.setChecked(detail.isChecked());
            holder.tvTitle.setText(detail.getTitle());
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }
    }
    
    /**
     * 自动识别链接的任务
     * @author tiger
     * @update 2016/3/13 10:41
     * @version 1.0.0
     */
    class AutoLinkTask implements Runnable {
        private CharSequence s;
        
        public AutoLinkTask(CharSequence s) {
            this.s = s;
        }
        
        @Override
        public void run() {
            Log.d(TAG, "-----AutoLinkTask---run---");
            //判断附件
            TextView textView = (TextView) mRichTextWrapper.getRichSpan();
            NoteLinkify.addLinks(textView, textView.getAutoLinkMask(), MessageBundleSpan.class);
            MovementMethod movement = textView.getMovementMethod();
            if (mContentLayout.isEditMode() && (movement == null || movement instanceof LinkMovementMethod)) {

                //移除链接的点击事件
                mRichTextWrapper.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            }
        }
    }

    /**
     * 笔记的更新状态监听器
     */
    class NoteContentObserver extends ContentObserver {

        public NoteContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.NoteColumns.NOTIFY_FLAG:  //笔记的通知
                    switch (notifyType) {
                        case ADD:   //保存笔记
                            //添加“详情”的菜单
                            addInfoMenu(mOverflowPopu);
                            boolean success = data != null;
                            saveResult(success);
                            if (success && data instanceof NoteInfo) {
                                NoteInfo note = (NoteInfo) data;
                                mNote.setId(note.getId());
                            }
                            break;
                        case UPDATE: //更新、保存笔记
                            saveResult(data != null);
                            break;
                        case DELETE:    //删除笔记
                            saveDeleteOpt();
                            if (data != null) { //删除成功,结束该界面
                                clearContent(true);
                                finish();
                            } else {
                                SystemUtil.makeShortToast(R.string.delete_result_error);
                            }
                            break;
                    }
                    break;
            }
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
                    break;
                case MSG_AUTO_LINK: //自动链接
                    if (activity.mAutoLinkTask == null) {
                        CharSequence s = (CharSequence) msg.obj;
                        activity.mAutoLinkTask = activity.new AutoLinkTask(s);
                    }
                    post(activity.mAutoLinkTask);
                    break;
            }
            super.handleMessage(msg);
        }
    }
    
}
