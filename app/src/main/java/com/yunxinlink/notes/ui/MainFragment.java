package com.yunxinlink.notes.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.helper.AdapterRefreshHelper;
import com.yunxinlink.notes.listener.OnCheckedChangeListener;
import com.yunxinlink.notes.listener.OnItemClickListener;
import com.yunxinlink.notes.listener.OnItemLongClickListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;
import com.yunxinlink.notes.widget.LayoutManagerFactory;
import com.yunxinlink.notes.widget.NoteItemViewAware;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 显示笔记的主界面
 * @author huanghui1
 * @update 2016/8/22 10:20
 * @version: 1.0.0
 */
public class MainFragment extends BaseFragment implements View.OnClickListener {
    
    public static final String ARG_FOLDER_ID = "folder_id";
    public static final String ARG_IS_TRASH = "is_trash";

    private static final int MSG_MOVE_FAILED = 4;
    private static final int MSG_MOVE_SUCCESS = 5;
    private static final int MSG_PALETTE_COLOR = 6;

    private SwipeRefreshLayout mRefresher;

    private RecyclerView mRecyclerView;
    
    private CoordinatorLayout mMainFrame;

    private NoteListAdapter mNoteListAdapter;
    private NoteGridAdapter mNoteGridAdapter;

    private List<DetailNoteInfo> mNotes;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;

    /**
     * 主界面右上角菜单
     */
    private PopupMenu mMainPopuMenu;

    /**
     * 显示的是否是网格风格
     */
    private boolean mIsGridStyle = true;
    /**
     * 笔记的排序方式
     */
    private int mNoteSort;

    private LayoutManagerFactory mLayoutManagerFactory;

    /**
     * 主界面的空的控件
     */
    private View mMainEmptyView;

    private NoteManager mNoteManager;

    //是否有删除笔记的操作，第一次操作则有删除提示，后面则没有了
    private boolean mHasDeleteOpt;

    private final Handler mHandler = new MyHandler(this);

    /**
     * 是否是多选模式
     */
    private boolean mIsChooseMode;

    private ActionMode mActionMode;

    //选择的笔记集合
    private List<DetailNoteInfo> mSelectedList;

    private OnMainFragmentInteractionListener mListener;
    
    //要加载的笔记本的sid
    private String mFolderId;

    /**
     * 是否加载的是垃圾桶的笔记
     */
    private boolean mIsTrash;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param folderId 笔记本的id,要加载的文件夹下的笔记，null：表示加载所有的笔记
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String folderId) {
        return newInstance(folderId, false);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param folderId 笔记本的id,要加载的文件夹下的笔记，null：表示加载所有的笔记
     * @param isTrash 是否只加载垃圾桶中的数据                
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String folderId, boolean isTrash) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        if (!TextUtils.isEmpty(folderId)) {
            args.putString(ARG_FOLDER_ID, folderId);
        }
        args.putBoolean(ARG_IS_TRASH, isTrash);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setHasOptionsMenu(true);
        
        Bundle args = getArguments();
        if (args != null) {
            mFolderId = args.getString(ARG_FOLDER_ID);
            mIsTrash = args.getBoolean(ARG_IS_TRASH, false);
        }
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //初始化下拉刷新界面
        mRefresher = (SwipeRefreshLayout) view.findViewById(R.id.refresher);
        
        mMainFrame = (CoordinatorLayout) view.findViewById(R.id.main_frame);

        mNotes = new ArrayList<>();
        mLayoutManagerFactory = new LayoutManagerFactory();
        mRecyclerView = (RecyclerView) view.findViewById(R.id.lv_data);

        mNoteManager = NoteManager.getInstance();

        mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        doOnRefresh();
                    }
                });

            }
        };

        mRefresher.setColorSchemeResources(R.color.colorPrimary);
        mRefresher.setOnRefreshListener(mOnRefreshListener);

        //初始化数据
        initData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.main, menu);
        
        if (mIsTrash) { //回收站的界面
            MenuItem clearAllItem = menu.add(0, R.id.action_clear_all, 200, R.string.action_clear_all);
            MenuItemCompat.setShowAsAction(clearAllItem, MenuItem.SHOW_AS_ACTION_ALWAYS);
            clearAllItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_clear_all, getContext().getTheme()));

            menu.removeItem(R.id.action_search);
        } else {
            MenuItem searchItem = menu.findItem(R.id.action_search);
            ((BaseActivity) getActivity()).setMenuTint(searchItem, Color.WHITE);
        }
        
        MenuItem item = menu.findItem(R.id.action_more);
        SystemUtil.setMenuOverFlowTint(getContext(), item);
        
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.action_more:  //更多
                
                View view = ((BaseActivity) getActivity()).getToolBarMenuView(id);
                createPopMenu(view);
                break;
            case R.id.action_clear_all: //清空回收站，彻底删除回收站所有的笔记
                NoteUtil.handleClearTrash(getContext());
                break;
            case R.id.action_search:    //搜索
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //初始化配置文件
        initProperties();
        //刷新数据
        pullRefresh();
    }

    /**
     * 刷新数据
     */
    private void doOnRefresh() {
        if (mListener == null) {
            KLog.d(TAG, "-----mListener--is--null---can--not---doOnRefresh----");
            return;
        }

        //初始化笔记的一些设置项
        initNoteSettings();

        //加载笔记
        loadNotes(mFolderId);
    }

    /**
     * 手动下拉刷新
     */
    private void pullRefresh() {
        mRefresher.post(new Runnable() {
            @Override
            public void run() {
                mRefresher.setRefreshing(true);
                mOnRefreshListener.onRefresh();
            }
        });
    }

    /**
     * 重新刷新数据
     * @param showProgress 是否加载进度条
     */
    public void reLoadData(boolean showProgress) {
        if (showProgress) {
            clearEmptyView();
            //刷新数据
            pullRefresh();
        } else {
            //加载笔记
            loadNotes(mFolderId);
        }
    }

    /**
     * 初始化配置文件
     * @author huanghui1
     * @update 2016/6/22 17:32
     * @version: 1.0.0
     */
    private void initProperties() {
        doInbackground(new Runnable() {
            @Override
            public void run() {
                if (mListener == null) {
                    return;
                }
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                mHasDeleteOpt = sharedPreferences.getBoolean(Constants.PREF_HAS_DELETE_OPT, false);
            }
        });
    }

    /**
     * 初始化主界面的显示方式，默认网格
     * @author huanghui1
     * @update 2016/6/30 20:42
     * @version: 1.0.0
     */
    private void initNoteSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mIsGridStyle = sharedPreferences.getBoolean(Constants.PREF_IS_GRID_STYLE, true);
        mNoteSort = sharedPreferences.getInt(Constants.PREF_NOTE_SORT, 0);
    }

    /**
     * 更新主界面的显示方式
     * @param isGridStyle 是否是网格显示
     * @author huanghui1
     * @update 2016/6/30 20:45
     * @version: 1.0.0
     */
    private void updateShowStyle(boolean isGridStyle) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.PREF_IS_GRID_STYLE, isGridStyle);
        editor.apply();
    }

    /**
     * 修改笔记的排序方式
     * @param sort
     */
    private void updateNoteSort(int sort) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREF_NOTE_SORT, sort);
        editor.apply();
    }

    /**
     * 选择笔记本
     * @param folderSid
     */
    public void selectFolder(String folderSid) {
        this.mFolderId = folderSid;
    }

    /**
     * 加载笔记
     * @param folderId 所属笔记本
     */
    public void loadNotes(final String folderId) {
        NoteManager noteManager = NoteManager.getInstance();
        Bundle args = new Bundle();
        if (!TextUtils.isEmpty(folderId)) {
            args.putString("folderId", folderId);
        }
        args.putInt("sort", mNoteSort);
        args.putBoolean("isRecycle", mIsTrash);
        List<DetailNoteInfo> list = noteManager.getAllDetailNotes(getCurrentUser(true), args);
        Message msg = mHandler.obtainMessage();
        msg.what = Constants.MSG_SUCCESS2;
        msg.obj = list;
        mHandler.sendMessage(msg);
    }

    /**
     * 加载空的提示view
     * @author huanghui1
     * @update 2016/3/9 14:44
     * @version: 1.0.0
     */
    private View loadEmptyView() {
        if (mMainEmptyView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mMainEmptyView = inflater.inflate(R.layout.main_empty_view, null);
            Button btnLogin = (Button) mMainEmptyView.findViewById(R.id.btn_login);
            btnLogin.setOnClickListener(this);
            TextView tvContent = (TextView) mMainEmptyView.findViewById(R.id.tv_content);
            if (mIsTrash) {
                
                SystemUtil.setViewVisibility(btnLogin, View.GONE);

                tvContent.setText(R.string.tip_note_empty_trash);
            } else if (hasUser()) {    //本地已有账号，但是没有笔记
                SystemUtil.setViewVisibility(btnLogin, View.VISIBLE);
                tvContent.setText(R.string.tip_note_empty);
                btnLogin.setText(R.string.do_refresh);
            }
            
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            layoutParams.anchorGravity = Gravity.CENTER;
            mMainEmptyView.setLayoutParams(layoutParams);

            if (mMainFrame != null) {
                mMainFrame.addView(mMainEmptyView, layoutParams);
            }
        } else {
            if (mMainEmptyView.getVisibility() != View.VISIBLE) {
                mMainEmptyView.setVisibility(View.VISIBLE);
            }
        }
        return mMainEmptyView;
    }

    /**
     * 清除空的提示控件
     * @author huanghui1
     * @update 2016/3/9 15:07
     * @version: 1.0.0
     */
    private void clearEmptyView() {
        if (mRefresher != null) {
            //显示recycleView
            SystemUtil.setViewVisibility(mRefresher, View.VISIBLE);
        }
        if (mMainEmptyView != null) {
            if (mMainEmptyView.getVisibility() == View.VISIBLE) {
                mMainEmptyView.setVisibility(View.GONE);
            }
//            CoordinatorLayout viewGroup = (CoordinatorLayout) findViewById(R.id.content_main);
            if (mMainFrame != null) {
                mMainFrame.removeView(mMainEmptyView);
                mMainEmptyView = null;
            }
        }
    }

    /**
     * 响应菜单按键的事件
     */
    public boolean onMenuKeyUp() {
        if (mMainPopuMenu != null) {
            togglePopuMenu(mMainPopuMenu);
        } else {
            //TODO 菜单的显示
            View view = ((BaseActivity) getActivity()).getToolBarMenuView(R.id.action_more);
            createPopMenu(view);
        }
        return true;
    }

    /**
     * popuMenu菜单的显示与消失之间的切换
     * @author tiger
     * @update 2016/2/27 17:43
     * @version 1.0.0
     */
    private void togglePopuMenu(PopupMenu popupMenu) {
        if (popupMenu != null) {
            if (SystemUtil.isPopuMenuShowing(popupMenu)) { //已经显示了，则隐藏
                popupMenu.dismiss();
            } else {
                popupMenu.show();
            }
        }
    }

    /**
     * 创建popuMenu
     * @param author 菜单要在哪个view上弹出
     * @author tiger
     * @update 2016/2/27 17:46
     * @version 1.0.0
     */
    private PopupMenu createPopMenu(View author) {
        if (author != null) {
            if (mMainPopuMenu == null) {
                mMainPopuMenu = createPopMenu(author, R.menu.main_overflow, true, new OnPopuMenuItemClickListener());
                Menu menu = mMainPopuMenu.getMenu();
                if (menu != null) {
                    tintMenu(menu);
                    if (!mIsGridStyle) {    //开始就显示列表，则菜单为网格
                        MenuItem menuItem = menu.findItem(R.id.nav_show_style);
                        if (menuItem != null) {
                            menuItem.setTitle(R.string.action_show_grid);
                            menuItem.setIcon(R.drawable.ic_action_grid);
                        }
                    }
                }
            }
            mMainPopuMenu.show();
            return mMainPopuMenu;
        } else {
            return null;
        }
    }

    /**
     * 根据不同的显示方式来显示不同的样式
     * @param isGridStyle 是否是网格显示样式
     * @param resetAdapter 是否重新设置adapter，如果不重新设置，则只是adapter的更新
     * @param refreshHelper 更新的类型，0：全部刷新，1：添加，2：更新单个，3：删除                    
     * @author huanghui1
     * @update 2016/3/1 11:45
     * @version: 1.0.0
     */
    private void setShowContentStyle(boolean isGridStyle, boolean resetAdapter, AdapterRefreshHelper refreshHelper) {
        if (isGridStyle) { //显示成网格样式
            /*if (mItemDecoration != null) {
                mRecyclerView.removeItemDecoration(mItemDecoration);
            }*/
            if (mNoteGridAdapter == null) {
                initNoteAdapter(true);
                mRecyclerView.setLayoutManager(mLayoutManagerFactory.getLayoutManager(getContext(), true));
                resetAdapter = true;
            }
            if (resetAdapter) {
                mRecyclerView.setAdapter(mNoteGridAdapter);
            } else {
                if (refreshHelper == null) {
                    mNoteGridAdapter.notifyDataSetChanged();
                } else {
                    refreshHelper.refresh(mNoteGridAdapter);
                }
            }
            if (mRecyclerView.getPaddingLeft() == 0) {
                int padding = getContext().getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
                mRecyclerView.setPadding(padding, 0, padding, 0);
            }
        } else {    //列表样式
//            mRecyclerView.addItemDecoration(getItemDecoration(mContext));
            if (mNoteListAdapter == null) {
                initNoteAdapter(false);
                mRecyclerView.setLayoutManager(mLayoutManagerFactory.getLayoutManager(getContext(), false));
                resetAdapter = true;
            }
            if (resetAdapter) {
                mRecyclerView.setAdapter(mNoteListAdapter);
            } else {
                if (refreshHelper == null) {
                    mNoteListAdapter.notifyDataSetChanged();
                } else {
                    refreshHelper.refresh(mNoteListAdapter);
                }
            }
            if (mRecyclerView.getPaddingLeft() != 0) {
                mRecyclerView.setPadding(0, 0, 0, 0);
            }
        }
    }

    /**
     * 添加笔记
     * @author huanghui1
     * @update 2016/3/9 17:29
     * @version: 1.0.0
     */
    public void addNote(DetailNoteInfo note) {
        mNotes.add(0, note);
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        refreshHelper.position = 0;
        refreshUI(mNotes, refreshHelper);
    }

    /**
     * 添加多条笔记，刷新界面
     * @param list
     */
    public void addNotes(List<DetailNoteInfo> list) {
        mNotes.addAll(0, list);
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        refreshHelper.fromPosition = 0;
        refreshHelper.toPosition = list.size() - 1;
        refreshUI(mNotes, refreshHelper);
    }

    /**
     * 显示撤销删除的提示
     */
    private void showUnDeleteToast(final List<DetailNoteInfo> list) {
        View contentView = mListener == null ? null : mListener.getContentMainView();
        contentView = contentView == null ? mMainFrame : contentView;
        Snackbar.make(contentView, R.string.delete_result_success, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleUnDeleteNote(list);
                    }
                }).show();
    }

    /**
     * 显示撤销删除的提示
     */
    private void showUnDeleteToast(final DetailNoteInfo detailNote) {
        List<DetailNoteInfo> list = new ArrayList<>(1);
        list.add(detailNote);
        showUnDeleteToast(list);
    }

    /**
     * 删除笔记
     * @param detailNote 笔记
     * @param isMove 是指只是移动笔记到其他文件文件，如果是指移动，那么在"所有文件夹"中就不需要删除了
     */
    public void deleteNote(DetailNoteInfo detailNote, boolean isMove) {
        int index = mNotes.indexOf(detailNote);
        if (index != -1) {  //列表中存在
            if (isMove) {
                NoteInfo note = detailNote.getNoteInfo();
                setupUpdateNote(mNotes.get(index).getNoteInfo(), note);
            } else {
                mNotes.remove(index);
                AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
                refreshHelper.position = index;
                refreshUI(mNotes, refreshHelper);

                if (!mIsTrash) {    //非回收站才显示
                    //显示回撤提示
                    showUnDeleteToast(detailNote);
                }
                
            }
        }
    }

    /**
     * 移除多个笔记，并刷新界面
     * @param list 笔记列表
     * @param isMove 是指只是移动笔记到其他文件文件，如果是指移动，那么在"所有文件夹"中就不需要删除了
     */
    public void deleteNotes(List<DetailNoteInfo> list, boolean isMove) {
        if (list != null && list.size() > 0) {
            if (isMove) {   //移动到其他文件夹
                for (DetailNoteInfo detailNote : list) {
                    int index = mNotes.indexOf(detailNote);
                    if (index != -1) {
                        NoteInfo note = detailNote.getNoteInfo();
                        setupUpdateNote(mNotes.get(index).getNoteInfo(), note);
                    }
                }
            } else {
                mNotes.removeAll(list);
                refreshUI(mNotes, null);
                
                if (!mIsTrash) {    //非回收站才显示
                    //显示回撤提示
                    showUnDeleteToast(list);
                }
            }
        }
    }

    /**
     * 清空界面所有的笔记
     */
    public void clearNotes() {
        mNotes.clear();
        refreshUI(mNotes, null);
    }

    /**
     * 之前选中的是第一项“所有文件夹”，移除所删除文件夹中的笔记
     * @param folderId 笔记本的sid
     */
    public void removeNotes(String folderId) {
        //TODO 删除选择的笔记
        List<DetailNoteInfo> deleteList = new ArrayList<>();
        for (DetailNoteInfo detailNote : mNotes) {
            NoteInfo note = detailNote.getNoteInfo();
            if (folderId.equals(note.getFolderId())) {
                deleteList.add(detailNote);
            }
        }
        if (deleteList.size() > 0) {
            mNotes.removeAll(deleteList);
            refreshUI(mNotes, null);
        }
    }

    /**
     * 删除单条笔记
     * @param detailNote 笔记
     * @author huanghui1
     * @update 2016/6/29 19:37
     * @version: 1.0.0
     */
    private void handleDeleteNote(final DetailNoteInfo detailNote) {
        List<DetailNoteInfo> list = new ArrayList<>(1);
        list.add(detailNote);
        NoteUtil.handleDeleteNote(getContext(), list, mHasDeleteOpt, mIsTrash);
    }

    /**
     * 恢复笔记
     * @param list
     */
    private void handleUnDeleteNote(List<DetailNoteInfo> list) {
        final List<DetailNoteInfo> detailNoteInfos = new ArrayList<>(list);
        doInbackground(new NoteTask(detailNoteInfos) {
            @Override
            public void run() {
                boolean success = NoteManager.getInstance().deleteNote((List<DetailNoteInfo>) params[0], DeleteState.DELETE_NONE);
                if (success) {
                    NoteUtil.notifyAppWidgetList(getContext());
                }
            }
        });
    }

    /**
     * 恢复笔记
     * @param detailNote
     */
    private void handleUnDeleteNote(DetailNoteInfo detailNote) {
        List<DetailNoteInfo> list = new ArrayList<>(1);
        list.add(detailNote);
        handleUnDeleteNote(list);
    }

    /**
     * 保存删除操作的记录
     */
    public void saveDeleteOpt() {
        if (!mHasDeleteOpt) {   //之前是否有删除操作，如果没有，则需保存  
            doInbackground(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(Constants.PREF_HAS_DELETE_OPT, true);
                    editor.apply();
                    mHasDeleteOpt = true;
                }
            });
        }
    }

    /**
     * 显示编辑笔记
     * @param note 笔记
     */
    private void showNoteInfo(NoteInfo note) {
        Intent intent = new Intent(getContext(), NoteEditActivity.class);
        intent.putExtra(NoteEditActivity.ARG_NOTE_ID, note.getId());
        intent.putExtra(NoteEditActivity.ARG_NOTE_SID, note.getSId());
        intent.putExtra(NoteEditActivity.ARG_IS_NOTE_TEXT, !note.isDetailNote());
        intent.putExtra(NoteEditActivity.ARG_FOLDER_ID, note.getFolderId());
        intent.putExtra(NoteEditActivity.ARG_OPT_DELETE, mHasDeleteOpt);
        startActivity(intent);
    }

    /**
     * 修改笔记
     * @author huanghui1
     * @update 2016/3/9 17:33
     * @version: 1.0.0
     */
    public void updateNote(DetailNoteInfo detailNote) {
        int index = mNotes.indexOf(detailNote);
        NoteInfo note = detailNote.getNoteInfo();
        if (index != -1) {  //列表中存在
            DetailNoteInfo oldDetail = mNotes.get(index);
            NoteInfo info = oldDetail.getNoteInfo();
            setupUpdateNote(info, note);
            oldDetail.setLastAttach(detailNote.getLastAttach());
            oldDetail.setDetailList(detailNote.getDetailList());

            //先删除原来位置的
            mNotes.remove(index);
            //再添加到0位
            mNotes.add(0, oldDetail);

            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_SWAP;
            refreshHelper.fromPosition = index;
            refreshHelper.toPosition = 0;
            refreshHelper.position = index;
            refreshUI(mNotes, refreshHelper);
            mRecyclerView.scrollToPosition(0);
        }
    }

    /**
     * 更新笔记的内容
     * @param oldNote 原来的笔记
     * @param newNote 新的笔记
     */
    private void setupUpdateNote(NoteInfo oldNote, NoteInfo newNote) {
        oldNote.setHash(newNote.getHash());
        oldNote.setOldContent(oldNote.getContent());
        oldNote.setModifyTime(newNote.getModifyTime());
        oldNote.setContent(newNote.getContent());
        oldNote.setShowContent(newNote.getShowContent());
        oldNote.setHasAttach(newNote.hasAttach());
        oldNote.setKind(newNote.getKind());
        oldNote.setRemindId(newNote.getRemindId());
        oldNote.setSyncState(newNote.getSyncState());
        oldNote.setFolderId(newNote.getFolderId());
        oldNote.setTitle(newNote.getTitle());
        oldNote.setAttaches(newNote.getAttaches());
    }

    /**
     * 刷新ui界面
     * @author huanghui1
     * @update 2016/3/10 9:10
     * @version: 1.0.0
     */
    private void refreshUI(List<DetailNoteInfo> list, AdapterRefreshHelper refreshHelper) {
        if (!SystemUtil.isEmpty(list)) {  //有数据
            setShowContentStyle(mIsGridStyle, false, refreshHelper);
            clearEmptyView();
            configMenuItem(true);
        } else {    //没有数据
            setShowContentStyle(mIsGridStyle, false, null);
            //隐藏recycleView
            SystemUtil.setViewVisibility(mRefresher, View.GONE);
            loadEmptyView();
            configMenuItem(false);
        }
    }

    /**
     * 显示或者隐藏菜单项
     * @param hasData 是否有数据
     */
    private void configMenuItem(boolean hasData) {
        if (!mIsTrash) {    //只有回收站界面才处理
            return;
        }
        Menu menu = ((BaseActivity) getActivity()).getOptionsMenu();
        if (menu != null) {
            int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem menuItem = menu.getItem(i);
                if (hasData) {  //设为可用
                    if (!menuItem.isVisible()) {
                        menuItem.setVisible(true);
                    }
                } else {
                    if (menuItem.isVisible()) {
                        menuItem.setVisible(false);
                    }
                }
            }
        }
    }

    /**
     * 移动笔记到其他文件夹
     * @param detailNote
     */
    private void moveNote(final DetailNoteInfo detailNote) {
        List<DetailNoteInfo> list = new ArrayList<>(1);
        list.add(detailNote);

        moveNotes(list);
    }

    /**
     * 移动笔记到其他文件夹
     * @param noteList
     */
    private void moveNotes(List<DetailNoteInfo> noteList) {
        if (noteList == null || noteList.size() == 0) {
            return;
        }
        final List<Folder> list = FolderCache.getInstance().getSortFolders();
        String currentFolderId = null;
        if (noteList.size() == 1) { //只有一个笔记
            currentFolderId = noteList.get(0).getNoteInfo().getFolderId();
        } else {    //多个笔记
            currentFolderId = mFolderId;
        }
        if (list != null && list.size() > 0) {
            AlertDialog.Builder builder = NoteUtil.buildDialog(getContext());
            int checkedItem = -1;
            int size = list.size();
            Folder selectedFolder = null;
            String[] items = new String[size];
            for (int i = 0; i < size; i++) {
                Folder folder = list.get(i);
                items[i] = folder.getName();
                if (folder.getSId().equals(currentFolderId)) {
                    checkedItem = i;
                    selectedFolder = folder;
                }
            }
            final List<DetailNoteInfo> selects = new ArrayList<>(noteList);
            final int defaultItem = checkedItem;
            final Folder oldFolder = selectedFolder;
            final AlertDialog dialog = builder.setTitle(R.string.move_to)
                    .setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            if (defaultItem != which) { //有改变
                                doInbackground(new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean success = false;
                                        if (selects.size() == 1) { //只有一条记录
                                            success = mNoteManager.move2Folder(selects, oldFolder, list.get(which));
                                        } else {    //多条记录
                                            Folder newFolder = list.get(which);
                                            List<DetailNoteInfo> actualList = new ArrayList<>();
                                            for (DetailNoteInfo detailNote : selects) {
                                                NoteInfo note = detailNote.getNoteInfo();
                                                if (!newFolder.getSId().equals(note.getFolderId())) {
                                                    actualList.add(detailNote);
                                                }
                                            }
                                            if (actualList.size() == 0) {
                                                success = true;
                                                mHandler.sendEmptyMessage(MSG_MOVE_SUCCESS);
                                                KLog.d(TAG, "--moveNotes--actualList---size---0---success--");
                                            } else {
                                                success = mNoteManager.move2Folder(actualList, oldFolder, newFolder);
                                            }
                                        }
                                        if (!success) {
                                            mHandler.sendEmptyMessage(MSG_MOVE_FAILED);
                                        }
                                    }
                                });
                                dialog.dismiss();
                            }
                        }
                    }).create();
            dialog.show();
        } else {
            SystemUtil.makeShortToast(R.string.folder_move_no_more);
        }
    }

    /**
     * 选择模式下每一项的选择事件
     * @param buttonView
     * @param isChecked
     */
    private void handleItemCheck(CompoundButton buttonView, boolean isChecked) {
        Integer pos = (Integer) buttonView.getTag();
        if (pos == null) {
            return;
        }
        if (isChecked) {
            addSelectedItem(pos);
        } else {
            removeSelectedItem(pos);
        }
    }

    /**
     * 初始化note的适配器
     * @author huanghui1
     * @update 2016/6/30 21:22
     * @version: 1.0.0
     */
    private void initNoteAdapter(boolean isGridStyle) {
        if (isGridStyle) {
            mNoteGridAdapter = new NoteGridAdapter(getContext(), mNotes);
            mNoteGridAdapter.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    handleItemCheck(buttonView, isChecked);
                }
            });
            mNoteGridAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(View view) {
                    Integer pos = (Integer) view.getTag(R.integer.item_tag_data);

                    if (pos == null) {
                        return false;
                    }

                    NoteGridViewHolder holder = (NoteGridViewHolder) view.getTag();
                    //初始化actionMode
                    initActionMode();

                    addSelectedItem(pos);

                    if (holder.mCbCheck.getVisibility() != View.VISIBLE) {
                        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                        refreshHelper.refresh(mNoteGridAdapter);
//                        mNoteGridAdapter.notifyDataSetChanged();
                    }
                    return true;
                }
            });
            mNoteGridAdapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view) {
                    if (mIsChooseMode) {    //选择模式
                        NoteGridViewHolder holder = (NoteGridViewHolder) view.getTag();
                        holder.mCbCheck.toggle();
                    } else {
                        Integer pos = (Integer) view.getTag(R.integer.item_tag_data);
                        if (pos == null) {
                            return;
                        }
                        if (mIsTrash) { //回收站界面不能查看，显示详细信息
                            showTrashNote(mNotes.get(pos));
                        } else {    //非回收站界面
                            showNoteInfo(mNotes.get(pos).getNoteInfo());
                        }
                    }
                }
            });
        } else {
            mNoteListAdapter = new NoteListAdapter(getContext(), mNotes);
            mNoteListAdapter.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    handleItemCheck(buttonView, isChecked);
                }
            });
            mNoteListAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(View view) {

                    Integer pos = (Integer) view.getTag(R.integer.item_tag_data);

                    if (pos == null) {
                        return false;
                    }

                    NoteListViewHolder holder = (NoteListViewHolder) view.getTag();
                    //初始化actionMode
                    initActionMode();

                    addSelectedItem(pos);

                    if (holder.mCbCheck.getVisibility() != View.VISIBLE) {
                        mNoteListAdapter.notifyDataSetChanged();
                    }
                    return true;
                }
            });
            mNoteListAdapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view) {
                    if (mIsChooseMode) {    //选择模式
                        NoteListViewHolder holder = (NoteListViewHolder) view.getTag();
                        holder.mCbCheck.toggle();
                    } else {
                        Integer pos = (Integer) view.getTag(R.integer.item_tag_data);
                        if (pos == null) {
                            return;
                        }
                        if (mIsTrash) { //回收站界面不能查看，显示详细信息
                            showTrashNote(mNotes.get(pos));
                        } else {    //非回收站界面
                            showNoteInfo(mNotes.get(pos).getNoteInfo());
                        }
                    }
                }
            });
        }
    }
    
    /**
     * 显示垃圾桶中的笔记信息
     * @author huanghui1
     * @update 2016/8/25 17:33
     * @version: 1.0.0
     */
    private void showTrashNote(final DetailNoteInfo detailNote) {
        NoteInfo note = detailNote.getNoteInfo();
        String info = note.getNoteInfo(getContext());
        AlertDialog.Builder builder = NoteUtil.buildDialog(getContext());
        builder.setTitle(note.getNoteTitle(false))
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.action_restore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleUnDeleteNote(detailNote);
                    }
                })
                .show();
    }

    /**
     * 添加选择项
     * @param position
     */
    private void addSelectedItem(int position) {
        if (mActionMode != null) {
            if (mSelectedList == null) {
                mSelectedList = new LinkedList<>();
            }
            DetailNoteInfo note = mNotes.get(position);
            if (mSelectedList.contains(note)) {
                return;
            }
            mSelectedList.add(note);
            mActionMode.setTitle(getSelectedTitle(mSelectedList.size(), mNotes.size()));

            if (mSelectedList.size() == 1) {
                updateActionModeMenuState(mActionMode, true, true);
            } else {
                updateActionModeMenuState(mActionMode, true, false);
            }
        }
    }

    /**
     * 组装选择的标题
     * @param selectedSize
     * @param totalSize
     * @return
     */
    private String getSelectedTitle(int selectedSize, int totalSize) {
        return selectedSize + "/" + totalSize;
    }

    /**
     * 移除选择项
     * @param position
     */
    private void removeSelectedItem(int position) {
        if (mActionMode != null) {
            if (mSelectedList == null) {
                mSelectedList = new LinkedList<>();
            }
            DetailNoteInfo note = mNotes.get(position);
            mSelectedList.remove(note);
            mActionMode.setTitle(getSelectedTitle(mSelectedList.size(), mNotes.size()));

            if (mSelectedList.isEmpty()) {  //没有选择项，则将菜单置为不可点
                updateActionModeMenuState(mActionMode, false, false);
            } else if (mSelectedList.size() == 1) {
                updateActionModeMenuState(mActionMode, true, true);
            } else {
                updateActionModeMenuState(mActionMode, true, false);
            }
        }
    }

    /**
     * 更新选择模式各菜单的状态
     * @param actionMode
     * @param enable
     * @param isSingle 是否只选择了一项
     */
    private void updateActionModeMenuState(ActionMode actionMode, boolean enable, boolean isSingle) {
        Menu menu = actionMode.getMenu();
        if (menu != null) {
            int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem menuItem = menu.getItem(i);
                if (enable) {
                    if (isSingle) { //只选择了单个
                        if (!menuItem.isEnabled()) {
                            menuItem.setEnabled(true);
                        }
                    } else {    //选择了多个
                        int menuId = menuItem.getItemId();
                        if (menuId == R.id.action_share || menuId == R.id.action_info) {
                            menuItem.setEnabled(false);
                        } else {
                            if (!menuItem.isEnabled()) {
                                menuItem.setEnabled(true);
                            }
                        }
                    }

                } else {
                    if (menuItem.isEnabled()) {
                        menuItem.setEnabled(false);
                    }
                }

            }
        }
    }

    /**
     * 清除选择的项
     */
    private void clearSelectedItem() {
        mIsChooseMode = false;
        if (mSelectedList != null) {
            mSelectedList.clear();
        }
        
        if (mListener != null) {
            mListener.setActionMode(false);
        }
    }

    /**
     * 初始化actionMode
     */
    private void initActionMode() {
        if (!mIsChooseMode) {
            mIsChooseMode = true;
            
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new ActionModeCallbackImpl());

            if (mListener != null) {
                mListener.setActionMode(true);
            }
        }
    }

    /**
     * 退出多选模式
     */
    public void outActionMode(boolean refresh) {
        if (!mIsChooseMode) {
            return;
        }

        //销毁actionMode
        finishActionMode(mActionMode);

        if (refresh) {
            //刷新界面
            refreshUI(mNotes, null);
        }

        //清除选择的项
        clearSelectedItem();
    }

    /**
     * 隐藏ActionMode
     * @param actionMode
     */
    private void finishActionMode(ActionMode actionMode) {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    /**
     * 根据位置获取对应的viewholder
     * @param position
     * @return
     */
    public RecyclerView.ViewHolder getViewHolder(int position) {
        return mRecyclerView.findViewHolderForAdapterPosition(position);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:    //登录
                if (hasUser()) {    //有本地用户了，则刷新笔记，优先加载本地笔记
                    reLoadData(true);
                } else {    //本地没有账号，需登录
                    Intent intent = new Intent(getContext(), AuthorityActivity.class);
                    startActivity(intent);
                }
                break;
        }
    }

    /**
     * 菜单的显示回调
     * @author huanghui1
     * @update 2016/7/1 14:55
     * @version: 1.0.0
     */
    class ActionModeCallbackImpl implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, final Menu menu) {
            MenuInflater menuInflater = getActivity().getMenuInflater();
            
            if (mIsTrash) { //回收站界面
                menuInflater.inflate(R.menu.note_trash_grid_item, menu);
            } else {
                menuInflater.inflate(R.menu.note_grid_item, menu);
                if (!FolderCache.getInstance().hasMoreFolder()) {   //没有更多的文件夹，除了“所有文件夹”
                    //移除“移动”菜单
                    menu.removeItem(R.id.action_move);
                }
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_move:  //移动
                    moveNotes(mSelectedList);
                    break;
                case R.id.action_delete:    //删除
                    NoteUtil.handleDeleteNote(getContext(), mSelectedList, mHasDeleteOpt, mIsTrash);
                    break;
                case R.id.action_share:    //分享
                    NoteUtil.shareNote(getContext(), mSelectedList.get(0));
                    break;
                case R.id.action_info:    //详情
                    NoteUtil.showInfo(getContext(), mSelectedList.get(0).getNoteInfo());
                    break;
                case R.id.action_restore:   //还原
                    handleUnDeleteNote(mSelectedList);
                    break;
            }
            outActionMode(true);
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            outActionMode(true);
        }
    }

    /**
     * 切换显示模式
     */
    private void changeShowMode(final MenuItem item) {
        mHandler.removeMessages(MSG_PALETTE_COLOR);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                outActionMode(false);
                mIsGridStyle = !mIsGridStyle;
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        updateShowStyle(mIsGridStyle);
                    }
                });
                RecyclerView.LayoutManager layoutManager = mLayoutManagerFactory.getLayoutManager(getContext(), mIsGridStyle);
                mRecyclerView.setLayoutManager(layoutManager);
                int resId = 0;
                if (mIsGridStyle) { //显示成网格样式
                                /*if (mItemDecoration != null) {
                                    mRecyclerView.removeItemDecoration(mItemDecoration);
                                }*/
                    if (mNoteGridAdapter == null) {
                        initNoteAdapter(true);
                    }
                    int padding = getContext().getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
                    mRecyclerView.setPadding(padding, 0, padding, 0);
                    mRecyclerView.setAdapter(mNoteGridAdapter);
                    item.setTitle(R.string.action_show_list);
                    resId = R.drawable.ic_action_view_list;
//                    item.setIcon(R.drawable.ic_action_view_list);
                } else {    //列表样式
                    mRecyclerView.setPadding(0, 0, 0, 0);
//                                mRecyclerView.addItemDecoration(getItemDecoration(mContext));
                    if (mNoteListAdapter == null) {
                        initNoteAdapter(false);
                    }
                    mRecyclerView.setAdapter(mNoteListAdapter);
                    item.setTitle(R.string.action_show_grid);
                    resId = R.drawable.ic_action_grid;
//                    item.setIcon(R.drawable.ic_action_grid);
                }
                Drawable drawable = ResourcesCompat.getDrawable(getResources(), resId, getActivity().getTheme());
                drawable = getTintDrawable(drawable, 0);
                item.setIcon(drawable);

            }
        });
    }

    /**
     * 是否是修改时间排序
     * @return
     */
    private boolean isModifyTimeSort() {
        return mNoteSort != NoteInfo.SORT_CREATE_TIME;
    }

    /**
     * 显示排序方式的对话框
     * @param currentSort 当前的排序，默认勾选项
     */
    private void showSortStyle(final int currentSort) {
        AlertDialog.Builder builder = NoteUtil.buildDialog(getContext());
        builder.setTitle(R.string.sort_style)
                .setSingleChoiceItems(R.array.sort_menu_items, currentSort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        dialog.dismiss();
                        if (which != currentSort) {
                            mNoteSort = which;
                            //下拉刷新
                            pullRefresh();

                            //修改笔记的排序方式
                            doInbackground(new NoteTask() {
                                @Override
                                public void run() {
                                    updateNoteSort(which);
                                }
                            });
                        }
                    }
                })
                .show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMainFragmentInteractionListener) {
            mListener = (OnMainFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnRegisterFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mIsChooseMode = false;
        mListener = null;
    }

    /**
     * popuMenu每一项点击的事件
     * @author huanghui1
     * @update 2016/3/2 15:05
     * @version: 1.0.0
     */
    class OnPopuMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_show_style:   //列表显示方式，有列表方式个网格方式
                    changeShowMode(item);

                    break;
                case R.id.nav_upload:   //同步
                    break;
                case R.id.nav_sort: //排序
                    showSortStyle(mNoteSort);
                    break;
            }
            return false;
        }
    }

    class NoteListViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvIcon;
        TextView mTvContent;
        TextView mTvTime;
        CheckBox mCbCheck;

        public NoteListViewHolder(View itemView) {
            super(itemView);

            mIvIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            mTvContent = (TextView) itemView.findViewById(R.id.tv_content);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
            mCbCheck = (CheckBox) itemView.findViewById(R.id.cb_check);
        }
    }

    class NoteListAdapter extends RecyclerView.Adapter<NoteListViewHolder> {

        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<DetailNoteInfo> mList;

        private OnItemLongClickListener mOnItemLongClickListener;

        private OnItemClickListener mOnItemClickListener;

        private OnCheckedChangeListener mOnCheckedChangeListener;

        //图片加载失败的图片
        private ItemAttachIcon mItemAttachIcon;
        //着色的颜色
        private int mTintColor;

        public NoteListAdapter(Context context, List<DetailNoteInfo> list) {
            this.mContext = context;
            this.mList = list;
            mLayoutInflater = LayoutInflater.from(context);

            mTintColor = initTintColor();

            mItemAttachIcon = new ItemAttachIcon();
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.mOnItemLongClickListener = onItemLongClickListener;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener = onCheckedChangeListener;
        }

        @Override
        public NoteListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_main_list, parent, false);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemLongClickListener != null) {
                        return mOnItemLongClickListener.onItemLongClick(v);
                    } else {
                        return false;
                    }
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v);
                    }
                }
            });
            NoteListViewHolder holder = new NoteListViewHolder(view);
            view.setTag(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(final NoteListViewHolder holder, int position) {
            DetailNoteInfo detailNote = mList.get(position);
            NoteInfo note = detailNote.getNoteInfo();
            holder.itemView.setTag(R.integer.item_tag_data, holder.getAdapterPosition());
            holder.mCbCheck.setTag(holder.getAdapterPosition());
            if (note != null) {
                holder.mCbCheck.setOnCheckedChangeListener(null);
                if (mIsChooseMode) {    //选择模式
                    boolean checked = mSelectedList != null && mSelectedList.size() > 0 &&  mSelectedList.contains(detailNote);
                    showCheckbox(holder.mCbCheck);
                    holder.mCbCheck.setChecked(checked);
                } else {
                    hideCheckbox(holder.mCbCheck);
                    if (holder.mCbCheck.isChecked()) {
                        holder.mCbCheck.setChecked(false);
                    }
                }

                //重置附件图标的各种状态
                resetAttachView(holder);

                holder.mTvContent.setText(note.getStyleContent(true, detailNote.getDetailList()));
                holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getShowTime(isModifyTimeSort())));

                holder.mCbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (mOnCheckedChangeListener != null) {
                            mOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                        }
                    }
                });

                if (note.hasAttach() && detailNote.getLastAttach() != null) {
                    SystemUtil.setViewVisibility(holder.mIvIcon, View.VISIBLE);
                    showAttachIcon(detailNote, holder);
                }

            }
        }

        /**
         * 重置附件图标的各种状态
         * @param holder
         */
        private void resetAttachView(RecyclerView.ViewHolder holder) {
            NoteListViewHolder listHolder = (NoteListViewHolder) holder;
            listHolder.mIvIcon.setImageResource(0);
            SystemUtil.setViewVisibility(listHolder.mIvIcon, View.GONE);
        }

        /**
         * 初始化着色的颜色
         * @return
         */
        private int initTintColor() {
            Resources resources = getResources();
            return ResourcesCompat.getColor(resources, R.color.text_time_color, getContext().getTheme());
        }

        /**
         * 初始化图片加载时候的图片
         * @return
         */
        private Drawable initFailedImage(int color) {
            Resources resources = getResources();
            Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_broken_image, getContext().getTheme());
            drawable = getTintDrawable(drawable, color);
            return drawable;
        }

        /**
         * 初始化图片加载时候的图片
         * @return
         */
        private Drawable initAttachIcon(int attachType, int color) {
            Drawable drawable = null;
            int resId = 0;
            Resources resources = getResources();
            switch (attachType) {
                case Attach.VOICE:
                    if (mItemAttachIcon.mMusicDrawable == null) {
                        resId = R.drawable.ic_library_music;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getContext().getTheme());
                        mItemAttachIcon.mMusicDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mMusicDrawable;
                    break;
                case Attach.VIDEO:
                    if (mItemAttachIcon.mVideoDrawable == null) {
                        resId = R.drawable.ic_library_music;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getContext().getTheme());
                        mItemAttachIcon.mVideoDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mVideoDrawable;
                    break;
                case Attach.ARCHIVE:
                    if (mItemAttachIcon.mArchiveDrawable == null) {
                        resId = R.drawable.ic_library_archive;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getContext().getTheme());
                        mItemAttachIcon.mArchiveDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mArchiveDrawable;
                    break;
                case Attach.FILE:
                    if (mItemAttachIcon.mFileDrawable == null) {
                        resId = R.drawable.ic_library_file;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getContext().getTheme());
                        mItemAttachIcon.mFileDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mFileDrawable;
                    break;
            }
            return drawable;
        }

        /**
         * 显示附件的图标
         * @param detailNote
         * @param holder
         */
        private void showAttachIcon(DetailNoteInfo detailNote, NoteListViewHolder holder) {
            Attach lastAttach = detailNote.getLastAttach();
            if (lastAttach.isImage()) { //图片文件
                ImageUtil.displayImage(lastAttach.getLocalPath(), new ImageViewAware(holder.mIvIcon, false), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (loadedImage == null) {
                            loadImageFailed((ImageView) view);
                        }
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        loadImageFailed((ImageView) view);
                    }
                });
            } else {
                Drawable drawable = initAttachIcon(lastAttach.getType(), mTintColor);
                holder.mIvIcon.setImageDrawable(drawable);
            }
        }

        /**
         * 图片加载失败
         * @param imageView
         */
        private void loadImageFailed(ImageView imageView) {
            if (mItemAttachIcon.mFailedDrawable == null) {
                mItemAttachIcon.mFailedDrawable = initFailedImage(mTintColor);
            }
            imageView.setImageDrawable(mItemAttachIcon.mFailedDrawable);
        }

        /**
         * 显示复选框
         * @param checkBox
         */
        public void showCheckbox(CheckBox checkBox) {
            if (checkBox.getVisibility() != View.VISIBLE) {
                checkBox.setVisibility(View.VISIBLE);
            }
        }

        /**
         * 隐藏复选框
         * @param checkBox
         */
        public void hideCheckbox(CheckBox checkBox) {
            if (checkBox.getVisibility() == View.VISIBLE) {
                checkBox.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        /**
         * 附件的类型的图标
         */
        class ItemAttachIcon {
            //图片加载失败的图片
            Drawable mFailedDrawable;
            Drawable mMusicDrawable;
            //压缩文件
            Drawable mArchiveDrawable;
            //视频文件
            Drawable mVideoDrawable;
            Drawable mFileDrawable;

        }
    }

    class NoteGridViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvOverflow;
        TextView mTvTitle;
        TextView mTvSummary;
        TextView mTvTime;
        CheckBox mCbCheck;
        View mItemContainer;
        ImageView mIvAttachIcon;

        public NoteGridViewHolder(final View itemView) {
            super(itemView);

            mIvOverflow = (ImageView) itemView.findViewById(R.id.iv_overflow);
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mTvSummary = (TextView) itemView.findViewById(R.id.tv_summary);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
            mCbCheck = (CheckBox) itemView.findViewById(R.id.cb_check);
            mItemContainer = itemView.findViewById(R.id.item_container);
            mIvAttachIcon = (ImageView) itemView.findViewById(R.id.iv_attach_icon);
        }
    }

    class NoteGridAdapter extends RecyclerView.Adapter<NoteGridViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<DetailNoteInfo> mList;

        private OnItemLongClickListener mOnItemLongClickListener;

        private OnItemClickListener mOnItemClickListener;

        private OnCheckedChangeListener mOnCheckedChangeListener;

        private ItemColor mItemColor;

        public NoteGridAdapter(Context context, List<DetailNoteInfo> list) {
            this.mContext = context;
            this.mList = list;
            mLayoutInflater = LayoutInflater.from(context);

            mItemColor = initItemColor();
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.mOnItemLongClickListener = onItemLongClickListener;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener = onCheckedChangeListener;
        }

        @Override
        public NoteGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_main_grid, parent, false);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemLongClickListener != null) {
                        return mOnItemLongClickListener.onItemLongClick(v);
                    }
                    return false;
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v);
                    }
                }
            });
            NoteGridViewHolder holder = new NoteGridViewHolder(view);
            view.setTag(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(final NoteGridViewHolder holder, int position) {
            DetailNoteInfo detailNote = mList.get(position);

            if (detailNote == null) {
                return;
            }

            holder.itemView.setTag(R.integer.item_tag_data, holder.getAdapterPosition());
            holder.mCbCheck.setTag(holder.getAdapterPosition());

            //重置文字的颜色
            resetTextColor(holder, mItemColor);

            if (mIsChooseMode) {
                SystemUtil.hideView(holder.mIvOverflow);
                SystemUtil.showView(holder.mCbCheck);

                holder.mCbCheck.setOnCheckedChangeListener(null);

                boolean checked = mSelectedList != null && mSelectedList.size() > 0 &&  mSelectedList.contains(detailNote);
                holder.mCbCheck.setChecked(checked);

                holder.mCbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (mOnCheckedChangeListener != null) {
                            mOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                        }
                    }
                });
            } else {
                SystemUtil.showView(holder.mIvOverflow);
                SystemUtil.hideView(holder.mCbCheck);
                holder.mIvOverflow.setOnClickListener(new GridItemClickListener(detailNote));
            }

            NoteInfo note = detailNote.getNoteInfo();

            Attach attach = null;
            int attachType = 0;
            if (note.hasAttach() && detailNote.getLastAttach() != null) {
                attach = detailNote.getLastAttach();
                attachType = attach.getType();
            }


            CharSequence title = note.getAttachShowTitle(mContext, attachType);
            /*if (note.isDetailNote() && TextUtils.isEmpty(title)) {
                title = getResources().getString(R.string.no_title);
            }*/

            holder.mTvTitle.setText(title);
            holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getShowTime(isModifyTimeSort())));
            holder.mTvSummary.setText(note.getStyleContent(detailNote.getDetailList()));

            if (attach != null) {   //有附件
                int attachIcon = attach.getAttachIcon();
                if (attachIcon != 0) {
                    SystemUtil.setViewVisibility(holder.mIvAttachIcon, View.VISIBLE);
                    holder.mIvAttachIcon.setImageResource(attachIcon);
                }
                ImageUtil.displayImage(attach.getLocalPath(), new NoteItemViewAware(holder.mItemContainer), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (loadedImage != null) {
                            doInbackground(new PaletteItemColorTask(holder.getAdapterPosition(), loadedImage));
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        /**
         * 初始化各项的文字颜色
         * @return
         */
        public ItemColor initItemColor() {
            Resources resources = getResources();
            int titleColor = ResourcesCompat.getColor(resources, R.color.text_title_color, getContext().getTheme());
            int contentColor = ResourcesCompat.getColor(resources, R.color.text_content_color, getContext().getTheme());
            int timeColor = ResourcesCompat.getColor(resources, R.color.text_time_color, getContext().getTheme());

            ItemColor itemColor = new ItemColor();
            itemColor.titleColor = titleColor;
            itemColor.contentColor = contentColor;
            itemColor.timeColor = timeColor;

            return itemColor;
        }

        /**
         * 重置文字的颜色
         * @param holder
         * @param itemColor 每项的颜色
         */
        public void resetTextColor(RecyclerView.ViewHolder holder, ItemColor itemColor) {
            NoteGridViewHolder gridHolder = (NoteGridViewHolder) holder;
            gridHolder.mTvTitle.setTextColor(itemColor.titleColor);
            gridHolder.mTvSummary.setTextColor(itemColor.contentColor);
            gridHolder.mTvTime.setTextColor(itemColor.timeColor);

            gridHolder.mIvOverflow.setImageResource(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                gridHolder.mItemContainer.setBackground(null);
            } else {
                gridHolder.mItemContainer.setBackgroundDrawable(null);
            }

            SystemUtil.setViewVisibility(gridHolder.mIvAttachIcon, View.GONE);
        }

        class ItemColor {
            int titleColor;
            int contentColor;
            int timeColor;
        }

        /**
         * 给每一项文本着色的任务
         * @author huanghui1
         * @update 2016/8/15 16:33
         * @version: 1.0.0
         */
        class PaletteItemColorTask implements Runnable {
            private int position;

            private Bitmap bitmap;

            public PaletteItemColorTask(int position, Bitmap bitmap) {
                this.position = position;
                this.bitmap = bitmap;
            }

            @Override
            public void run() {
                if (bitmap == null || bitmap.isRecycled() || !mIsGridStyle) {
                    return;
                }

                Palette.Builder builder = new Palette.Builder(bitmap);
                Palette palette = builder.generate();
                List<Palette.Swatch> swatches = palette.getSwatches();
                if (swatches.size() == 0) {
                    return;
                }
                Palette.Swatch swatch = swatches.get(swatches.size() - 1);
                if (swatch == null) {
                    return;
                }
                int titleColor = swatch.getTitleTextColor();
                int bodyColor = swatch.getBodyTextColor();
                Message msg = mHandler.obtainMessage();
                msg.arg1 = titleColor;
                msg.arg2 = bodyColor;
                msg.obj = position;
                msg.what = MSG_PALETTE_COLOR;
                mHandler.sendMessage(msg);
            }
        }

        /**
         * 网格的每一项点击事件
         * @author huanghui1
         * @update 2016/6/21 11:40
         * @version: 1.0.0
         */
        class GridItemClickListener implements View.OnClickListener {
            private DetailNoteInfo detailNote;

            public GridItemClickListener(DetailNoteInfo detailNote) {
                this.detailNote = detailNote;
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.iv_overflow:
                        int menuRes = 0;
                        if (mIsTrash) { //回收站的菜单
                            menuRes = R.menu.note_trash_grid_item;
                        } else {
                            menuRes = R.menu.note_grid_item;
                        }
                        PopupMenu itemMenu = createPopMenu(v, menuRes, false, new ItemMenuClickListener(detailNote));
                        boolean hasMoreFolder = FolderCache.getInstance().hasMoreFolder();
                        if (!mIsTrash && !hasMoreFolder) {   //删除“移动”菜单项
                            Menu menu = itemMenu.getMenu();
                            if (menu != null) {
                                menu.removeItem(R.id.action_move);
                            }
                        }
                        itemMenu.show();
                        break;
                }
            }
        }

        /**
         * 菜单的每一项点击监听器
         * @author huanghui1
         * @update 2016/6/21 14:45
         * @version: 1.0.0
         */
        class ItemMenuClickListener implements PopupMenu.OnMenuItemClickListener {
            private DetailNoteInfo detailNote;

            public ItemMenuClickListener(DetailNoteInfo detailNote) {
                this.detailNote = detailNote;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:    //删除
                        handleDeleteNote(detailNote);
                        break;
                    case R.id.action_info:  //详情
                        NoteUtil.showInfo(mContext, detailNote.getNoteInfo());
                        break;
                    case R.id.action_move:  //移动
                        moveNote(detailNote);
                        break;
                    case R.id.action_share: //分享
                        NoteUtil.shareNote(getContext(), detailNote);
                        break;
                    case R.id.action_restore:   //还原
                        handleUnDeleteNote(detailNote);
                        break;
                }
                return false;
            }
        }

    }

    private static class MyHandler extends BaseHandler<MainFragment> {

        public MyHandler(MainFragment target) {
            super(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainFragment target = mTarget.get();
            if (target != null) {
                switch (msg.what) {
                    case Constants.MSG_SUCCESS2:    //笔记内容加载成功
                        target.mRefresher.setRefreshing(false);
                        List<DetailNoteInfo> list = (List<DetailNoteInfo>) msg.obj;
                        if (!SystemUtil.isEmpty(list)) {  //有数据
                            if (!target.mNotes.isEmpty()) {
                                target.mNotes.clear();
                            }
                            target.mNotes.addAll(list);
                            target.refreshUI(target.mNotes, null);
                        } else {    //没有数据
                            if (!target.mNotes.isEmpty()) { //原来有数据
                                target.mNotes.clear();
                            }
                            target.refreshUI(target.mNotes, null);
                        }

                        break;
                    case MSG_PALETTE_COLOR: //给笔记的item着色，针对有背景图片的
                        int titleColor = msg.arg1;
                        int bodyColor = msg.arg2;
                        Integer position = (Integer) msg.obj;
                        if (position < 0) {
                            return;
                        }
                        RecyclerView.ViewHolder holder = target.getViewHolder(position);
                        if (holder != null && holder instanceof NoteGridViewHolder) {
                            NoteGridViewHolder gridHolder = (NoteGridViewHolder) holder;
                            
                            Integer tagPos = (Integer) gridHolder.itemView.getTag(R.integer.item_tag_data);
                            if (tagPos != null && tagPos.intValue() == position.intValue()) { //同一项，防止错位
                                gridHolder.mTvTitle.setTextColor(titleColor);
                                gridHolder.mTvSummary.setTextColor(bodyColor);
                                gridHolder.mTvTime.setTextColor(bodyColor);

                                Drawable drawable = gridHolder.mIvOverflow.getDrawable();
                                drawable = target.getTintDrawable(drawable, titleColor);
                                gridHolder.mIvOverflow.setImageDrawable(drawable);
                            }
                        }
                        break;
                }
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnMainFragmentInteractionListener {

        /**
         * action的状态
         * @param isOn 是否是开启状态，true：是开启状态
         */
        void setActionMode(boolean isOn);

        /**
         * 获取主界面内容区域的根布局
         * @return
         */
        View getContentMainView();
    }
}
