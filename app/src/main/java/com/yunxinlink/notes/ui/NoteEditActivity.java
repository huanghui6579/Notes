package com.yunxinlink.notes.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.appwidget.WidgetAction;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.cache.NoteCache;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.edit.recorder.AudioRecorder;
import com.yunxinlink.notes.listener.SimpleAttachAddCompleteListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.EditStep;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.persistent.AttachManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.richtext.AttachSpec;
import com.yunxinlink.notes.service.CoreService;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;
import com.yunxinlink.notes.widget.NoteSearchLayout;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * 笔记编辑界面
 * @author huanghui1
 * @update 2016/3/2 10:13
 * @version: 1.0.0
 */
public class NoteEditActivity extends BaseActivity implements View.OnClickListener, TextWatcher, 
        NoteEditFragment.OnFragmentInteractionListener, 
        DetailListFragment.OnDetailInteractionListener {
    
    public static final String ACTION_EDIT = "com.yunxinlink.notes.EDIT_ACTION";


    public static final String ARG_NOTE_ID = "noteId";
    public static final String ARG_NOTE_SID = "noteSId";
    public static final String ARG_IS_NOTE_TEXT = "isNoteText";
    public static final String ARG_FOLDER_ID = "folderId";
    public static final String ARG_OPT_DELETE = "opt_delete";
    public static final String ARG_HAS_LOCK_CONTROLLER = "has_lock_controller";
    
    public static final String ARG_NOTE_ADD_TYPE = "note_add_type";

    private static final int MSG_INIT_BOTTOM_TOOL_BAR = 3;
    private static final int MSG_READ_CONTACT_SUCCESS = 4;
    private static final int MSG_READ_CONTACT_FAILED = 5;
    
    public static final int REQ_PICK_IMAGE = 10;
    public static final int REQ_TAKE_PIC = 11;
    public static final int REQ_PAINT = 12;
    public static final int REQ_PICK_FILE = 13;
    public static final int REQ_EDIT_PAINT = 14;
    public static final int REQ_CHOOSE_CONTACT = 15;
    
    //笔记的阅读模式
    public static final int NOTE_MODE_VIEW = 0;
    //笔记的文本编辑模式
    public static final int NOTE_MODE_TEXT = 1;
    //笔记的清单编辑模式
    public static final int NOTE_MODE_DETAIL_LIST = 2;

    private PopupMenu mAttachPopu;
    private PopupMenu mOverflowPopu;
    private PopupMenu mOverflowViewPopu;

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

    private NoteManager mNoteManager;
    
    private NoteInfo mNote;
    
    //包含清单的笔记
    private DetailNoteInfo mDetailNote;

    private Handler mHandler = new MyHandler(this);
    
    //文件夹id
    private String mFolderId;

    /**
     * 笔记的模式，默认是阅读模式：0（不可编辑），文本编辑模式：1，清单编辑模式2
     */
    private int mNoteMode;
    
    private View mBottomBar;

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

    //笔记编辑的界面
    private NoteEditFragment mNoteEditFragment;
    //清单的界面
    private DetailListFragment mDetailListFragment;
    
    private FrameLayout mContentContainer;
    
    //fragment的包装器
    private FragmentWrapper mFragmentWrapper;
    //搜索菜单的容器
    private NoteSearchLayout mSearchViewContainer;
    //菜单控件的缓存
    private List<View> mMenuItems;

    /**
     * 设置自定义标题
     * @param title
     * @param iconResId
     */
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
        
        KLog.d(TAG, "---updateToolBar--");
    }

    @Override
    protected boolean hasLockedController() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean hasLockController = intent.getBooleanExtra(ARG_HAS_LOCK_CONTROLLER, true);
            if (hasLockController) {
                return false;
            } else {
                String action = intent.getAction();
                if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {  //分享过来的，也不加锁
                    return false;
                } else {
                    return true;
                }
            }
        }
        return super.hasLockedController();
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
    protected void initView() { 
        mContentContainer = (FrameLayout) findViewById(R.id.content_container);
        mFragmentWrapper = new FragmentWrapper();
    }

    @Override
    protected void initData() {
        KLog.d(TAG, "-------initData-----");
        mNoteManager = NoteManager.getInstance();
        registContentObserver();

        //处理数据
        handleIntent();
    }

    /**
     * 处理分享过来的的数据
     */
    private void handleShareAction() {
        Intent intent = getIntent();
        if (intent == null) {
            KLog.d(TAG, "-----handleShareAction--intent--is--null---");
            return;
        }
        String action = intent.getAction();
        String type = intent.getType();
        if (type == null) {
            KLog.d(TAG, "---handleShareAction---type----is--null---");
        } else {
            boolean updateTitle = false;
            switch (action) {
                case Intent.ACTION_SEND:    //单个文件分享
                    updateTitle = true;
                    boolean isSingleFile = false;
                    if (FileUtil.MIME_TYPE_TEXT.equals(type)) { //文本
                        String text = NoteUtil.handleSendText(intent);
                        if (text == null) { //没有文本，则试图获取附件
                            isSingleFile = true;
                        } else {    //有文本内容
                            mNote.setContent(text);
                        }
                    } else {    //文件
                        isSingleFile = true;
                    }
                    if (isSingleFile) {
                        Uri uri = NoteUtil.handleSendFile(intent);
                        KLog.d(TAG, "--handleShareAction---isSingleFile---" + uri);
                        if (uri != null) {
                            handleShowAttach(uri);
                        }
                    }
                    break;
                case Intent.ACTION_SEND_MULTIPLE:   //多文件分享
                    updateTitle = true;
                    List<Uri> uris = NoteUtil.handleSendMultipleFiles(intent);
                    if (uris != null && uris.size() > 0) {
                        int size = uris.size();
                        List<Uri> shareUris = null;
                        if (size > Constants.MAX_SHARE_ATTACH_SIZE) {   //截取前5个
                            shareUris = uris.subList(0, Constants.MAX_SHARE_ATTACH_SIZE - 1);
                            SystemUtil.makeShortToast(getString(R.string.tip_share_attach_max_size, Constants.MAX_SHARE_ATTACH_SIZE));
                        } else {
                            shareUris = uris;
                        }
                        handleShowAttach(shareUris);
                    }
                    break;
            }
            if (updateTitle) {  //需要更新标题
                setTitle(getAppName());
                updateToolBar(mToolBar);
            }
        }
    }

    /**
     * 处理Intent的数据
     */
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            int noteId = intent.getIntExtra(ARG_NOTE_ID, 0);
            String sid = intent.getStringExtra(ARG_NOTE_SID);
            boolean isTextStyle = intent.getBooleanExtra(ARG_IS_NOTE_TEXT, true);
            mFolderId = intent.getStringExtra(ARG_FOLDER_ID);
            mHasDeleteOpt = intent.getBooleanExtra(ARG_OPT_DELETE, true);

            //初始化笔记
            initNote();

            if (!TextUtils.isEmpty(sid)) {
                mNote.setSid(sid);
            }

            mNote.setId(noteId);

            //设置笔记的模式，是编辑模式还是阅读模式
            setupNoteStyle(isTextStyle, false);

            if (noteId > 0) {   //查看模式
                if (isTextStyle) {
                    setNoteMode(NOTE_MODE_VIEW);
                } else {
                    setNoteMode(NOTE_MODE_DETAIL_LIST);
                }
                loadNoteInfo(noteId);
            } else {    //编辑模式
                setNoteMode(NOTE_MODE_TEXT);
            }
        }
    }

    //初始化笔记
    private void initNote() {
        if (mNote == null) {
            mNote = new NoteInfo();
            mDetailNote = new DetailNoteInfo();
            mDetailNote.setNoteInfo(mNote);
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
            if (mBottomBar != null) {
                return;
            }
            mHandler.sendEmptyMessage(MSG_INIT_BOTTOM_TOOL_BAR);
        }
    }

    /**
     * 将编辑界面在笔记的文本界面和清单界面之间切换
     * @param isTextStyle
     * @param updateMenu 是否更新菜单
     */
    private CharSequence setupNoteStyle(boolean isTextStyle, boolean updateMenu) {
        Fragment fragment = null;
        ActionFragment actionFragment = null;
        CharSequence text = null;
        if (mNote == null) {
            getNoteSid();
        }
        if (isTextStyle) {  //切换到文本编辑模式
            mNoteEditFragment = NoteEditFragment.newInstance();
            mNoteEditFragment.setViewMode(false);

            if (mDetailListFragment != null) {
                text = mDetailListFragment.getText(true);
            }
            if (text != null) {
                mNoteEditFragment.setText(text);
            }
            fragment = mNoteEditFragment;
            actionFragment = mNoteEditFragment;
            
        } else {    //清单模式
            mDetailListFragment = DetailListFragment.newInstance();

            mDetailListFragment.setNoteInfo(mNote);
            
            if (mNoteEditFragment != null) {
                text = mNoteEditFragment.getText();
            }
            if (text != null) {

                mDetailListFragment.setText(text, mNote);
            }
            fragment = mDetailListFragment;
            actionFragment = mDetailListFragment;
            
        }

        mFragmentWrapper.setFragment(actionFragment);
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, fragment, fragment.getClass().getSimpleName());
        transaction.commit();

        if (updateMenu) {

            if (isTextStyle) {
                //设置笔记的编辑模式
                setNoteMode(NOTE_MODE_TEXT);
            } else {
                //设置笔记的编辑模式
                setNoteMode(NOTE_MODE_DETAIL_LIST);
            }
            setupMenuStyle(isTextStyle);
            //根据文本类型修改菜单项
            setupOverMenuStyle(isTextStyle);
            
            //设置底部栏的状态
            setupBottomBarStyle(isTextStyle);
        }
        return text;
    }

    /**
     * 根据笔记的模式来切换菜单
     * @param isTextStyle
     */
    private void setupMenuStyle(boolean isTextStyle) {
        if (mToolBar != null) {
            Menu menu = mToolBar.getMenu();
            if (menu == null) {
                return;
            }
            
            MenuItem photoItem = menu.findItem(R.id.action_photo);
            
            if (photoItem != null) {
                photoItem.setVisible(isTextStyle);
            }
            
            MenuItem attachItem = menu.findItem(R.id.action_attach);
//
            if (attachItem != null) {
                attachItem.setVisible(isTextStyle);
            }
        }
    }

    /**
     * 修改笔记类型的菜单
     * @param isTextStyle
     */
    private void setupOverMenuStyle(boolean isTextStyle) {
        if (mOverflowPopu != null) {
            Menu menu = mOverflowPopu.getMenu();
            MenuItem detailItem = menu.findItem(R.id.action_detailed_list);
            int textTes = 0;
            Drawable drawable = null;
            if (isTextStyle) {  //文本笔记
                drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_detailed_list, getTheme());
                textTes = R.string.action_detailed_list;
            } else {
                drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_description, getTheme());
                textTes = R.string.note_type_text;
            }
            getTintDrawable(drawable, 0);
            if (detailItem != null) {
                detailItem.setIcon(drawable);
                detailItem.setTitle(textTes);
            }
        }
    }

    /**
     * 退出搜索模式
     * @return
     */
    private boolean outSearchMode() {
        if (isSearchMode()) {
            setupSearchMode(false);
            return true;
        }
        return false;
    }

    /**
     * 进行搜索操作
     * @param keyword
     */
    private void doSearch(String keyword) {
        if (mFragmentWrapper != null) {
            mFragmentWrapper.getFragment().doSearch(keyword);
        }
    }

    /**
     * 取消搜索
     */
    private void cancelSearch() {
        if (mFragmentWrapper != null) {
            mFragmentWrapper.getFragment().cancelSearch();
        }
    }

    /**
     * 查看搜索前面的结果
     */
    private void onFindPrevious() {
        if (mFragmentWrapper != null) {
            mFragmentWrapper.getFragment().onFindPrevious();
        }
    }

    /**
     * 查看搜索后面的结果
     */
    private void onFindNext() {
        if (mFragmentWrapper != null) {
            mFragmentWrapper.getFragment().onFindNext();
        }
    }

    /**
     * 搜索模式的切换
     * @param searchMode 是否要切换到搜索模式
     */
    private void setupSearchMode(boolean searchMode) {
        if (searchMode) {  //切换到搜索模式
            if (mSearchViewContainer == null) {
                mSearchViewContainer = new NoteSearchLayout(mContext);
            } else {
                mSearchViewContainer.setOnQueryTextListener(null);
                mSearchViewContainer.clearText();
            }
            mSearchViewContainer.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    doSearch(newText);
                    return true;
                }
            });
            mSearchViewContainer.setOnSearchActionListener(new NoteSearchLayout.OnSearchActionListener() {
                @Override
                public void onPrevious() {
                    onFindPrevious();
                }

                @Override
                public void onNext() {
                    onFindNext();
                }
            });
            int count = mToolBar.getChildCount();
            if (mMenuItems == null) {
                mMenuItems = new LinkedList<>();
                for (int i = 0; i < count; i++) {
                    if (i != 0) {   //第一位不移除
                        View view = mToolBar.getChildAt(i);
                        if (view != null) {
                            mMenuItems.add(view);
                        }
                    }
                }
                mToolBar.removeViews(1, count - 1);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mSearchViewContainer.setLayoutParams(params);
                mToolBar.addView(mSearchViewContainer, params);
            } else {
                KLog.d(TAG, "-----setupSearchMode--searchMode---true---but--is----already---searchMode--");
            }
            mSearchViewContainer.inSearch();
        } else {    //退出搜索模式
            cancelSearch();
            if (mSearchViewContainer != null) {
                mSearchViewContainer.outSearch();
                mToolBar.removeView(mSearchViewContainer);
            }
            int count = mToolBar.getChildCount();
            if (count <= 1 && mMenuItems != null && mMenuItems.size() > 0) {   //菜单只有一个
                for (int i = 0; i < mMenuItems.size(); i++) {
                    mToolBar.addView(mMenuItems.get(i));
                }
                mMenuItems.clear();
                mMenuItems = null;
            }
        }
    }

    /**
     * 判断是否是搜索模式
     * @return
     */
    public boolean isSearchMode() {
        return mMenuItems != null && mMenuItems.size() > 0;
    }

    /**
     * 修改底部栏的样式
     * @param isTextStyle
     */
    private void setupBottomBarStyle(boolean isTextStyle) {
        if (mBottomBar != null) {
            View unDo = mBottomBar.findViewById(R.id.iv_undo);
            View reDo = mBottomBar.findViewById(R.id.iv_redo);
            View order = mBottomBar.findViewById(R.id.iv_list);
            boolean enable = true;
            if (!isTextStyle) { //清单笔记
                enable = false;
                SystemUtil.setViewEnable(unDo, false);
                SystemUtil.setViewEnable(reDo, false);
            } else {
                SystemUtil.setViewEnable(unDo, hasUndo());
                SystemUtil.setViewEnable(reDo, hasRedo());
            }
            SystemUtil.setViewEnable(order, enable);
        }
    }

    /**
     * 是否是阅读模式
     * @return 是否是阅读模式
     */
    private boolean isViewMode() {
        return mNoteMode == NOTE_MODE_VIEW;
    }

    /**
     * 是否是文本编辑模式
     * @return
     */
    private boolean isTextMode() {
        return mNoteMode == NOTE_MODE_TEXT;
    }

    /**
     * 是否是清单模式
     * @return
     */
    private boolean isDetailListMode() {
        return mNoteMode == NOTE_MODE_DETAIL_LIST;
    }

    /**
     * 设置模式
     * @param noteMode
     */
    public void setNoteMode(int noteMode) {
        this.mNoteMode = noteMode;
        if (mNoteMode != NOTE_MODE_DETAIL_LIST) {   //切换文本的模式
            if (mNoteEditFragment != null) {
                mNoteEditFragment.setViewMode(mNoteMode == NOTE_MODE_VIEW);
            }
        } else {    //清单类型
            changeNoteMode(true);
        }
    }

    /**
     * 在笔记的文本编辑模式和清单编辑模式中切换
     * @return 是否是文本编辑模式
     */
    public boolean toggleNoteText() {
        boolean isTextStyle = true;
        if (mNoteMode == NOTE_MODE_TEXT) {  //之前是文本编辑模式
            mNoteMode = NOTE_MODE_DETAIL_LIST;  //切换为清单模式 
            isTextStyle = false;
        } else if (mNoteMode == NOTE_MODE_DETAIL_LIST) {    //之前为清单编辑模式，
            mNoteMode = NOTE_MODE_TEXT;  //切换为文本编辑模式
        }
        return isTextStyle;
    }

    /**
     * 初始化编辑模式
     */
    private void initEditInfo(boolean isViewMode) {
        if (isViewMode) {  //查看模式
            KLog.d(TAG, "--initEditInfo---isViewMode-----true--");
            return;
        }
        if (mNoteEditFragment != null) {
            mNoteEditFragment.initEditInfo();

            //处理分享过来的数据
            handleShareAction();

            if (mNote.getId() <= 0) {   //没有内容，可能是新建笔记
                showNote(mDetailNote);
            }

            changeNoteMode(true);

            //处理桌面小部件的事件
            createNote();
        }
    }

    /**
     * 出来桌面小部件的各类型的添加笔记
     */
    private void createNote() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        int noteType = intent.getIntExtra(ARG_NOTE_ADD_TYPE, -1);
        if (noteType <= 0) {
            return;
        }
                
        WidgetAction widgetAction = WidgetAction.valueOf(noteType);
        if (widgetAction == null) {
            return;
        }
        KLog.d(TAG, "create note widget action:" + widgetAction);
        switch (widgetAction) {
            case NOTE_CAMERA:
                takePicture();
                break;
            case NOTE_VOICE:
                makeVoice();
                break;
            case NOTE_BRUSH:
                makePaint();
                break;
            case NOTE_FILE:
                chooseFile();
                break;
            case NOTE_PHOTO:
                chooseImage();
                break;
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
        } else {
            menu.setGroupVisible(R.id.group_edit_type, false);
            menu.setGroupVisible(R.id.group_edit, true);
        }
    }

    /**
     * 显示笔记到界面上
     * @param detailNote
     */
    private void showNote(DetailNoteInfo detailNote) {
        ActionFragment actionFragment = getActionFragment();
        if (actionFragment != null) {
            actionFragment.showNote(detailNote, mAttachCache);
        }
        /*if (mNoteEditFragment != null) {
            mNoteEditFragment.showNote(note, mAttachCache);
        }*/
    }

    /**
     * 获取当前加载的fragment
     * @return
     */
    private ActionFragment getActionFragment() {
        ActionFragment actionFragment = null;
        if (mFragmentWrapper != null && mFragmentWrapper.getFragment() != null) {
            actionFragment = mFragmentWrapper.getFragment();
        }
        return actionFragment;
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
//                mNote = mNoteManager.getNote(noteId);
                mDetailNote = mNoteManager.getDetailNote(noteId);
                if (mDetailNote != null) {
                    mNote = mDetailNote.getNoteInfo();
                }
                if (mNote != null) {
                    mAttachCache = mNote.getAttaches();
                    mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
                }
            }
        });
    }

    /**
     * 设置笔记的内容
     * @param text
     */
    private void setNoteInfo(final CharSequence text) {
        initNote();
        doInbackground(new Runnable() {
            @Override
            public void run() {
                mNote.setContent(text.toString());
                mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
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
        }
        sid = mNote.getSid(); 
        return sid;
    }

    /**
     * 保存笔记
     * @param removeAttach　是否移除缓存中的附件数据库记录
     * @return
     */
    private void saveNote(boolean removeAttach) {
        if (mFragmentWrapper == null) {
            return;
        }
        ActionFragment actionFragment = getActionFragment();
        if (actionFragment == null) {
            KLog.d(TAG, "note edit activity save note but action fragment is null and return");
            return;
        }
        NoteInfo.NoteKind noteKind = actionFragment.getNoteType();

        String content = null;
        String title = null;
        boolean hasDetailList = false;
        List<DetailList> detailLists = null;
        boolean isTextStyle = true;
        //包含标题和正文的内容
        String body = null;
        if (noteKind == NoteInfo.NoteKind.TEXT) {   //文本笔记
            content = actionFragment.getText().toString();
            title = content;
            body = content;
        } else if (noteKind == NoteInfo.NoteKind.DETAILED_LIST) {   //清单类型
            isTextStyle = false;
            content = mDetailListFragment.getText().toString();
            title = mDetailListFragment.getTitle() == null ? "" : mDetailListFragment.getTitle().toString();
            if (!TextUtils.isEmpty(title)) {
                body = title + Constants.TAG_NEXT_LINE + content;
            } else {
                body = content;
            }
            hasDetailList = mDetailListFragment.hasDetailList();
            if (hasDetailList) {    //有清单
                detailLists = mDetailListFragment.getDetailLists();
            }
        }
        
        if (TextUtils.isEmpty(body) && removeAttach) {
            if (mNote == null || mNote.isEmpty()) {
                removeCacheAttach(true);    //移除缓存中的附件，彻底删除数据库记录
            } else {
                removeCacheAttach(false);    //移除缓存中的附件，彻底删除数据库记录
            }
            return;
        }
        
        if (TextUtils.isEmpty(body)) {
            return;
        }
        
        boolean needUpdate = true;
        Intent intent = null;
        if (mNote != null && !mNote.isEmpty()) {    //更新笔记
            intent = new Intent(mContext, CoreService.class);
            if (!mNote.isDetailNote()) {    //之前保存的模式的是文本笔记，则比较内容
                if (body.equals(mNote.getContent()) && isTextStyle) {   //内容相同或者类型没有变，没有修改
                    needUpdate = false;
                }
            } else {    //之前保存的模式是清单笔记
                //获取清单的内容
                CharSequence noteText = mDetailNote.getNoteText().toString();
                if (body.equals(noteText) && !isTextStyle) {
                    needUpdate = false;
                }
            }

            //则只检测是否有多余的附件记录，有，则删除
            intent.putExtra(Constants.ARG_SUB_OBJ, needUpdate);
            
            if (!isTextStyle) {
                mNote.setTitle(title);  //只有清单才设置标题
            }
            mNote.setKind(noteKind);
            mNote.setContent(content);
            mNote.setModifyTime(System.currentTimeMillis());
            mNote.setSyncState(SyncState.SYNC_UP);
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_UPDATE_NOTE);
        } else {    //添加笔记
            needUpdate = true;
            if (mNote == null) {
                mNote = new NoteInfo();
            }
            intent = new Intent(mContext, CoreService.class);
            intent.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_ADD_NOTE);
            long time = System.currentTimeMillis();
            if (!isTextStyle) {
                mNote.setTitle(title);  //只有清单才设置标题
            }
            mNote.setContent(content);
            mNote.setModifyTime(time);
            mNote.setCreateTime(time);
            mNote.setFolderId(mFolderId);
            //获取hash在子线程中完成
//            mNote.setHash(DigestUtil.md5Digest(content));
            mNote.setKind(noteKind);
            if (mNote.getSid() == null) {
                mNote.setSid(SystemUtil.generateNoteSid());
            }
            int userId = getCurrentUserId();
            if (userId > 0) {
                mNote.setUserId(userId);
            }
            mNote.setSyncState(SyncState.SYNC_UP);
        }
        
        //加入到缓存
        DetailNoteInfo detailNoteInfo = new DetailNoteInfo();
        detailNoteInfo.setNoteInfo(mNote);
        detailNoteInfo.setDetailList(detailLists);

        NoteCache noteCache = NoteCache.getInstance();
        
        if (mDetailNote.hasDetailList()) {  //原来有清单项
            List<DetailList> srcDetailList = new ArrayList<>(mDetailNote.getDetailList());
            noteCache.setExtraData(srcDetailList);
        }
        
        //仅仅传sid
        intent.putExtra(Constants.ARG_CORE_OBJ, mNote.getSid());
        if (mAttachCache != null && mAttachCache.size() > 0) {
            detailNoteInfo.setExtraObj(mAttachCache);
//            ArrayList<String> list = new ArrayList<>();
//            list.addAll(mAttachCache.keySet()); //将附件的sid传入，不论附件是否在笔记中
//            intent.putStringArrayListExtra(Constants.ARG_CORE_LIST, list);
        }

        noteCache.set(detailNoteInfo);
        startService(intent);

        if (needUpdate) {
            SystemUtil.makeShortToast(R.string.update_result_success);
        }
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
                    KLog.e(TAG, "--removeCacheAttach---getAttachPath--error--" + e.getMessage());
                    e.printStackTrace();
                }
            }
            startService(intent);
            mAttachCache.clear();
        }
    }

    /**
     * 进入编辑模式
     */
    private void setupEditMode() {
        if (mNoteEditFragment == null) {
            return;
        }

        setNoteMode(NOTE_MODE_TEXT);
        
        mNoteEditFragment.setupEditMode();

        //显示菜单
        setMenuVisible(null, true);

        showNote(mDetailNote);

        setSoftInputVisibility(true);

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
            setupMenuStyle(isTextMode());
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
                chooseImage();
                break;
            case R.id.action_attach:    //添加附件
                attachView = getToolBarMenuView(R.id.action_attach);
                mAttachPopu = createPopuMenu(attachView, mAttachPopu, R.menu.edit_attach, true);
                break;
            case R.id.action_more:  //更多
                attachView = getToolBarMenuView(R.id.action_more);
                mOverflowPopu = showEditActionMore(attachView, mOverflowPopu);

                break;
            case R.id.action_edit:  //进入编辑模式
                if (isViewMode()) { //之前是阅读模式
                    setupEditMode();
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
                    
                    if (mNote != null) {
                        if (mNote.isEmpty()) {
                            menu.removeItem(R.id.action_info);  //没有详情
                        }

                        //根据文本类型修改菜单项
                        setupOverMenuStyle(isTextMode());
                    }

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
        tempPopu.show();
        return tempPopu;
    }

    /**
     * 创建附件的弹出菜单
     * @param showImmediate 是否立刻显示，调用show方法
     * @author huanghui1
     * @update 2016/3/2 14:15
     * @version: 1.0.0
     */
    private PopupMenu createPopuMenu(View author, PopupMenu popupMenu, int menuResId, boolean showImmediate) {
        if (author != null) {
            if (popupMenu == null) {
                popupMenu = createPopMenu(author, menuResId, true, new OnPopMenuItemClickListener());
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

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mContentContainer.getLayoutParams();
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
        
        if (isDetailListMode()) {   //清单模式
            setupBottomBarStyle(false);
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
            case R.id.iv_contact:   //选择联系人
                chooseContact();
                break;
            case R.id.iv_down:  //隐藏/显示软键盘
                if (mBottomBar.getVisibility() == View.VISIBLE) {   //隐藏软键盘
                    setSoftInputVisibility(false);
                }
                break;
            case R.id.toolbar_title:    //自定义标题的点击事件
                stopRecorder();
                break;
            case R.id.tv_content:   //阅读模式的文本点击事件
                setupEditMode();
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
    }

    @Override
    public void afterTextChanged(Editable s) {
        
    }

    @Override
    public void onBackPressed() {

        if (outSearchMode()) {
            return;
        }
        
        if (mNoteEditFragment != null) {
            KLog.d(TAG, "*****content***" + mNoteEditFragment.getText());
        }
        super.onBackPressed();
    }

    @Override
    protected void onBack() {
        if (outSearchMode()) {
            return;
        }
        super.onBack();
    }

    @Override
    protected void beforeBack() {
        super.beforeBack();
        if (mNoteEditFragment != null) {
            mNoteEditFragment.endBatchEdit();
        }
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
                        KLog.d(TAG, "---onActivityResult---REQ_PAINT---data---is----null--");
                    }
                    break;
                case REQ_EDIT_PAINT:    //编辑绘画
                    if (data != null) {
                        AttachSpec attachSpec = data.getParcelableExtra(Constants.ARG_CORE_OBJ);
                        handleUpdateImage(attachSpec);
                    } else {
                        KLog.d(TAG, "---onActivityResult---REQ_EDIT_PAINT---data---is----null--");
                    }
                    break;
                case REQ_PICK_FILE: //选择文件，不限格式
                    if (data != null) {
                        final Uri uri = data.getData();
                        KLog.d(TAG, "--onActivityResult---req_pick_file---uri--" + uri);
                        handleShowAttach(uri);
                    } else {
                        KLog.d(TAG, "---onActivityResult---req_pick_file---data---is----null--");
                    }
                    break;
                case REQ_CHOOSE_CONTACT:    //选择联系人的结果
                    if (data != null) {
                        Uri uri = data.getData();
                        readContactInfo(uri);
                    }
                    break;
            }
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 进入选择联系人的界面
     */
    private void chooseContact() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, REQ_CHOOSE_CONTACT);
    }

    /**
     * 获取联系人的信息，仅仅是名字和号码
     *
     * @param uri
     * @return
     */
    public void readContactInfo(final Uri uri) {
        final String[] permissions = {Manifest.permission.READ_CONTACTS};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                doReadContact(uri);
            }

            @Override
            public void onDenied(String permission) {
                KLog.d(TAG, "-----onDenied---permission----" + permission);
                if (permissions[0].equals(permission)) {
                    //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
                    NoteUtil.onPermissionDenied(NoteEditActivity.this, permission, R.string.tip_read_contact_permission_need, R.string.tip_read_permission_failed);
                }
            }
        });
    }

    /**
     * 读取联系人
     * @param uri
     */
    private void doReadContact(Uri uri) {
        doInbackground(new NoteTask(uri) {
            @Override
            public void run() {
                String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query((Uri) params[0], projection,
                            null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {

                        String number = cursor.getString(cursor.getColumnIndex(projection[0]));

                        String name = cursor.getString(cursor.getColumnIndex(projection[1]));

                        //联系人信息的数据，0:姓名，1：号码
                        String[] array = new String[2];

                        array[0] = name;
                        array[1] = number;

                        Message msg = mHandler.obtainMessage();
                        msg.what = MSG_READ_CONTACT_SUCCESS;
                        msg.obj = array;
                        mHandler.sendMessage(msg);
                    } else {
                        mHandler.sendEmptyMessage(MSG_READ_CONTACT_FAILED);
                        KLog.e(TAG, "--readContactInfo--failed----permission--error--");
                    }
                } catch (Exception e) {
                    mHandler.sendEmptyMessage(MSG_READ_CONTACT_FAILED);
                    KLog.e(TAG, "--readContactInfo--error--" + e.getMessage());
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

            }
        });
    }

    /**
     * 插入联系人信息，仅包含姓名和号码
     * @param info 联系人信息的数据[0]:姓名，[1]:号码
     * @return 返回插入的联系人信息
     */
    public void insertContact(String[] info) {
        if (info != null && info.length > 0) {
            String name = info[0];
            String number = info[1];
            String text = "";
            if (!TextUtils.isEmpty(name)) {
                text += name;
            }
            if (!TextUtils.isEmpty(number)) {
                text += number;
            }
            if (TextUtils.isEmpty(text)) {
                SystemUtil.makeShortToast(R.string.read_empty_contact);
                return;
            }
            if (mFragmentWrapper != null) {
                mFragmentWrapper.getFragment().insertContact(info);
            }
        }
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
                KLog.d(TAG, "addImage----" + filePath);
                
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
                if (mNoteEditFragment != null) {
                    mNoteEditFragment.addAttach(attach, new SimpleAttachAddCompleteListenerImpl(true));
                }
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
        attach.setSid(SystemUtil.generateAttachSid());
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
        if (mNoteEditFragment != null) {
            mNoteEditFragment.handleUpdateImage(attachSpec, new SimpleAttachAddCompleteListenerImpl(false));
        }
    }

    /**
     * 显示选择的附件
     * @param uri
     */
    private void handleShowAttach(Uri uri) {
        List<Uri> list = new ArrayList<>(1);
        list.add(uri);
        handleShowAttach(list);
    }

    /**
     * 同时添加多个附件
     * @param uris
     */
    private void handleShowAttach(List<Uri> uris) {
        doInbackground(new NoteTask(uris) {
            @Override
            public void run() {
                List<Uri> list = (List<Uri>) params[0];
                List<Attach> attachList = new ArrayList<>();
                for (Uri uri : list) {
                    String filePath = SystemUtil.getFilePathFromContentUri(uri.toString(), mContext);
                    if (TextUtils.isEmpty(filePath)) {
                        KLog.d(TAG, "--handleShowAttach---filePath--is---empty--");
                        continue;
                    }
                    File file = new File(filePath);
                    Attach attach = getAddedAttach(filePath);
                    if (attach == null) {
                        attach = file2Attach(file, 0);
                        attachList.add(attach);
                    } else if (mNoteEditFragment != null) {
                        mNoteEditFragment.addAttach(attach, null);
                    }
                }
                if (mNoteEditFragment != null && attachList.size() > 0) {
                    KLog.d(TAG, "handleShowAttach--attachList--" + attachList);
                    mNoteEditFragment.addAttach(attachList, new SimpleAttachAddCompleteListenerImpl(true));
                }
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
     * 是否有回撤操作
     * @return
     */
    private boolean hasUndo() {
        return mUndoStack.size() > 0;
    }

    /**
     * 是否有前进操作
     * @return
     */
    private boolean hasRedo() {
        return mRedoStack.size() > 0;
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
        if (!editStep.isEmpty() && mNoteEditFragment != null) {
            CharSequence content = editStep.getContent();
            Editable editable = mNoteEditFragment.getEditTextView().getEditableText();
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
                        KLog.d(TAG, "----addRedo---false---not--able---to---delete---");
                    }
                } else {    //之前的操作是删除文字，则此时的操作是插入文字String sid = mEtContent.getAttachSid(content);
                    String sid = SystemUtil.getAttachSid(content);
                    if (sid != null && mAttachCache != null && mAttachCache.get(sid) != null) {  //有附件
                        Attach attach = mAttachCache.get(sid);
                        resetAttach(editStep, attach);
                    } else {
                        editable.insert(start, content);
                    }
                }
            } catch (Exception e) {
                addRedo = false;
                KLog.e(TAG, "----undo--error---" + e.getMessage());
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
        if (mNoteEditFragment == null) {
            return;
        }
        setdo(true);
        EditStep editStep = mRedoStack.pop();
        CharSequence content = editStep.getContent();
        Editable editable = mNoteEditFragment.getEditTextView().getEditableText();
        boolean addUndo = true;
        try {
            if (editStep.isAppend()) {  //前进操作是插入文字
                String sid = SystemUtil.getAttachSid(content);
                if (sid != null && mAttachCache != null && mAttachCache.get(sid) != null) {  //有附件
                    Attach attach = mAttachCache.get(sid);
                    resetAttach(editStep, attach);
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
                    KLog.d(TAG, "----addUndo---false---not--able---to---delete---");
                }
            }
        } catch (Exception e) {
            KLog.e(TAG, "---redo---error-----" + e.getMessage());
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
    private void resetAttach(final EditStep editStep, final Attach attach) {
        if (mNoteEditFragment != null) {
            mNoteEditFragment.resetAttach(editStep, attach, new NoteEditFragment.InsertTextCallback() {
                @Override
                public void beforeInsertText() {
                    setdo(true);
                }

                @Override
                public void afterInsertText() {
                    setdo(false);
                }
            });
        }
    }
    
    /**
     * 在格式化列表直接切换
     * @author huanghui1
     * @update 2016/3/10 14:36
     * @version: 1.0.0
     */
    private void toggleFormatList() {
        if (mNoteEditFragment != null) {
            mNoteEditFragment.toggleFormatList();
        }
    }

    /**
     * 在光标处插入当前时间
     * @author tiger
     * @update 2016/3/13 10:27
     * @version 1.0.0
     */
    private void insertTime() {
        if (mFragmentWrapper != null) {
            mFragmentWrapper.getFragment().insertTime();
        }
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
    private void handleAddAttach(String filePath, Object data, Attach attach) {
        if (mAttachCache == null) {
            mAttachCache = new HashMap<>();
        }
        if (attach == null) {
            KLog.d(TAG, "---handleAddAttach---added---not--need--add---");
            return;
        }
        if (mAttachCache.containsKey(attach.getSid())) {
            KLog.d(TAG, "---handleAddAttach---added---not--need--add----mAttachCache--has-----" + filePath);
            return;
        }
        doInbackground(new NoteTask(filePath, attach) {
            @Override
            public void run() {
                Attach att = (Attach) params[1];
                att.setUri((String) params[0]);

                long time = System.currentTimeMillis();
                att.setCreateTime(time);
                att.setModifyTime(time);

                int userId = getCurrentUserId();
                if (userId > 0) {
                    att.setUserId(userId);
                }
                AttachManager.getInstance().addAttach(att);
                mAttachCache.put(att.getSid(), att);
                mDetailNote.setLastAttach(att);
                KLog.d(TAG, "---handleAddAttach--mAttachCache--has---uri--add--");
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
            KLog.d(TAG, "---handleAddAttach---added---not--need--add---");
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
                    mAttachCache.put(tmpAttach.getSid(), tmpAttach);
                    mDetailNote.setLastAttach(tmpAttach);
                    KLog.d(TAG, "---handleUpdateAttach--mAttachCache--has---uri--update--");
                } else {
                    KLog.d(TAG, "--handleUpdateAttach--filePath--is---null--");
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
                    KLog.d(TAG, "---handleAddAttach--mAttachCache--has---uri--");
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
        final ActionFragment actionFragment = getActionFragment();
        if (actionFragment == null) {
            KLog.d(TAG, "note edit clear content but action fragment is null");
            return;
        }
        if (deleteNote) {
            actionFragment.clearContent();
            /*if (mNoteEditFragment != null) {
                mNoteEditFragment.clearContent();
            }*/
            return;
        }
        //给予清空内容非提示
        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setTitle(R.string.prompt)
                .setMessage(R.string.confirm_clear_content)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        actionFragment.clearContent();

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
                        KLog.e(TAG, "getAttachFile error：" + e.getMessage());
                        e.printStackTrace();
                    }
                    if (mAttachFile == null) {
                        SystemUtil.makeShortToast(R.string.record_error);
                        KLog.e(TAG, "getAttachFile error");
                    }
                }
                mAudioRecorder.setFilePath(mAttachFile.getAbsolutePath());
                mAudioRecorder.setRecordListener(new AudioRecorder.OnRecordListener() {
                    @Override
                    public void onBeforeRecord(String filePath) {
                        KLog.d(TAG, "----onBeforeRecord--");
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
                            if (mNoteEditFragment != null) {
                                mNoteEditFragment.addAttach(attach, new SimpleAttachAddCompleteListenerImpl(true));
                            }
                        } else {
                            mAttachFile = null;
                        }
                        hideRecordView();
                    }

                    @Override
                    public void onRecordError(String filePath, String errorMsg) {
                        KLog.d(TAG, "----onRecordError--");
                        hideRecordView();
                        SystemUtil.makeShortToast(R.string.record_error);
                    }
                });
                mAudioRecorder.startRecording();
            }

            @Override
            public void onDenied(String permission) {
                //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
                NoteUtil.onPermissionDenied(NoteEditActivity.this, permission, R.string.tip_record_permission_need, R.string.tip_record_permission_failed);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
        KLog.d(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
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
                    KLog.d(TAG, "----stopRecorder---record---file---" + mAttachFile);
                }
            });
            /*mAudioRecorder.releaseRecorder();
            mAudioRecorder = null;

            hideRecordView();*/
            
            KLog.d(TAG, "----stopRecorder---record---file---" + mAttachFile);
        }
    }

    /**
     * 显示或者隐藏软键盘
     * @param visible
     */
    public void setSoftInputVisibility(boolean visible) {
        if (mNoteEditFragment != null) {
            mNoteEditFragment.setSoftInputVisibility(visible);
        }
    }

    /**
     * 显示正在录音的试图
     */
    private void showRecordView() {
        setSoftInputVisibility(false);
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

    @Override
    public void beforeNoteTextChanged(CharSequence s, int start, int count, int after) {
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
    public void onNoteTextChanged(CharSequence s, int start, int before, int count) {
        if (s == null) {
            return;
        }
        KLog.d(TAG, "----onNoteTextChanged--mIsDo--" + mIsDo + "----mUndoStack----" + mUndoStack);
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
    }

    @Override
    public void afterNoteTextChanged(Editable s) {

    }

    @Override
    public void onInitCompleted(boolean isViewMode) {
        initEditInfo(isViewMode);
    }

    /**
     * 添加拍照
     */
    private void takePicture() {
        try {
            String sid = getNoteSid();
            mAttachFile = null;
            mAttachFile = SystemUtil.getCameraFile(sid);
            if (mAttachFile == null) {
                SystemUtil.makeShortToast(R.string.tip_mkfile_error);
            } else {
                openCamera(mAttachFile);
            }
        } catch (IOException e) {
            SystemUtil.makeShortToast(R.string.tip_camera_error);
            KLog.e(TAG, "----OnPopMenuItemClickListener---onMenuItemClick----openCamera---error--" + e.getMessage());
        }
    }

    /**
     * 添加语音
     */
    private void makeVoice() {
        mAttachFile = null;
        try {
            startRecorder();
        } catch (Exception e) {
            SystemUtil.makeShortToast(R.string.record_error);
            KLog.e(TAG, "---startRecorder--error--" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加涂鸦
     */
    private void makePaint() {
        Intent intent = new Intent(mContext, HandWritingActivity.class);
        AttachSpec attachSpec = new AttachSpec();
        attachSpec.noteSid = getNoteSid();
        attachSpec.attachType = Attach.PAINT;
        intent.putExtra(Constants.ARG_CORE_OBJ, attachSpec);
        startActivityForResult(intent, REQ_PAINT);
    }

    /**
     * 添加附件
     */
    private void chooseFile() {
        SystemUtil.choseFile(NoteEditActivity.this, null, REQ_PICK_FILE);
    }

    /**
     * 添加图片
     */
    private void chooseImage() {
        SystemUtil.choseImage((Activity) mContext, REQ_PICK_IMAGE);
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
                    takePicture();
                    break;
                case R.id.action_voice: //添加语音
                    makeVoice();
                    break;
                case R.id.action_delete:    //删除
                    if (mNote != null && mNote.hasId()) {
                        NoteUtil.handleDeleteNote(mContext, mNote, mHasDeleteOpt);
                    } else {    //清空内容
                        clearContent(false);
                    }
                    break;
                case R.id.action_brush: //涂鸦
                    makePaint();
                    break;
                case R.id.action_file:    //添加附件
                    chooseFile();
                    break;
                case R.id.action_share:    //分享
                    if (mDetailNote == null || TextUtils.isEmpty(mDetailNote.getNoteText())) {
                        SystemUtil.makeShortToast(R.string.tip_share_no_text);
                    } else {
                        NoteUtil.shareNote(mContext, mDetailNote);
                    }
                    break;
                case R.id.action_info:    //详情
                    if (mNote != null && !mNote.isEmpty()) {
                        NoteUtil.showInfo(mContext, mNote);
                    }
                    break;
                case R.id.action_detailed_list: //清单与文本之间的切换
                    CharSequence text = setupNoteStyle(toggleNoteText(), true);
                    if (!TextUtils.isEmpty(text)) {
                        setNoteInfo(text);
                    }
                    break;
                case R.id.action_find:  //搜索
                    setupSearchMode(true);
                    break;
            }
            return false;
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
                            if (success) {
                                NoteInfo note = null;
                                if (data instanceof NoteInfo) {
                                    note = (NoteInfo) data;
                                } else if (data instanceof DetailNoteInfo) {
                                    note = ((DetailNoteInfo) data).getNoteInfo();
                                }
                                if (note != null) {
                                    mNote.setId(note.getId());
                                }
                            } else {
                                KLog.d(TAG, "---note----add---failed--data:---" + data);
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
                case MSG_INIT_BOTTOM_TOOL_BAR:
                    activity.initBottomToolBar();
                    break;
                case Constants.MSG_SUCCESS:
                    if (activity.mDetailNote != null) {
                        activity.showNote(activity.mDetailNote);
                    }
                    break;
                case MSG_READ_CONTACT_SUCCESS:  //读取联系人成功
                    String[] info = (String[]) msg.obj;
                    activity.insertContact(info);
                    break;
                case MSG_READ_CONTACT_FAILED:   //读取联系人失败，没有权限
                    SystemUtil.makeShortToast(R.string.read_contact_failed);
                    break;
            }
            super.handleMessage(msg);
        }
    }
    
}
