package com.yunxinlink.notes.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.socks.library.KLog;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.helper.AdapterRefreshHelper;
import com.yunxinlink.notes.helper.ItemTouchHelperAdapter;
import com.yunxinlink.notes.helper.ItemTouchHelperViewHolder;
import com.yunxinlink.notes.helper.OnStartDragListener;
import com.yunxinlink.notes.helper.SimpleItemTouchHelperCallback;
import com.yunxinlink.notes.listener.OnItemClickListener;
import com.yunxinlink.notes.listener.OnItemLongClickListener;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.log.Log;
import com.yunxinlink.notes.widget.DividerItemDecoration;
import com.yunxinlink.notes.widget.LayoutManagerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 笔记文件夹的列表界面
 * @author huanghui1
 * @update 2016/6/23 11:45
 * @version: 1.0.0
 */
public class FolderListActivity extends BaseActivity implements OnStartDragListener {
    
    private FolderManager mFolderManager;
    
    private List<Folder> mFolders;

    private SwipeRefreshLayout mRefresher;

    private RecyclerView mRecyclerView;

    private RecyclerView.ItemDecoration mItemDecoration;
    
    private FolderAdapter mFolderAdapter;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;
    
    private FolderContentObserver mFolderObserver;

    private ItemTouchHelper mItemTouchHelper;
    
    private final Handler mHandler = new MyHandler(this);

    @Override
    protected int getContentView() {
        return R.layout.activity_folder_list;
    }

    @Override
    protected void initData() {
        mRefresher.post(new Runnable() {
            @Override
            public void run() {
                mRefresher.setRefreshing(true);
                mOnRefreshListener.onRefresh();
            }
        });

    }

    @Override
    protected void initView() {
        mFolderManager = FolderManager.getInstance();
        mFolders = new ArrayList<>();

        //初始化下拉刷新界面
        mRefresher = (SwipeRefreshLayout) findViewById(R.id.refresher);

        LayoutManagerFactory layoutManagerFactory = new LayoutManagerFactory();
        mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
        mRecyclerView.setLayoutManager(layoutManagerFactory.getLayoutManager(this, false));

        mItemDecoration = getItemDecoration(this);
        mRecyclerView.addItemDecoration(mItemDecoration);
        
        mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initFolder();
            }
        };

        if (mRefresher != null) {
            mRefresher.setColorSchemeResources(R.color.colorPrimary);
            mRefresher.setOnRefreshListener(mOnRefreshListener);
        }

        //注册观察者
        registContentObserver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.folder_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:   //添加笔记文件夹
                Intent intent = new Intent(mContext, FolderEditActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        //注销
        unregistContentObserver();
        super.onDestroy();
    }

    /**
     * 注册文件夹的监听器
     * @author huanghui1
     * @update 2016/6/23 16:02
     * @version: 1.0.0
     */
    private void registContentObserver() {
        if (mFolderObserver == null) {
            mFolderObserver = new FolderContentObserver(mHandler);
        }
        mFolderManager.addObserver(mFolderObserver);
    }
    
    /**
     * 注销数据库的监听
     * @author huanghui1
     * @update 2016/6/23 16:03
     * @version: 1.0.0
     */
    private void unregistContentObserver() {
        if (mFolderObserver != null) {
            mFolderManager.removeObserver(mFolderObserver);
        }
    }

    /**
     * 初始化文件夹
     * @author huanghui1
     * @update 2016/3/8 17:38
     * @version: 1.0.0
     */
    private void initFolder() {

        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Folder archive = new Folder();
                archive.setName(getString(R.string.default_archive));
                int count = mFolderManager.getNoteCount(null);
                archive.setCount(count);
                List<Folder> list = mFolderManager.getAllFolders(getCurrentUser(), null);
                
                if (!SystemUtil.isEmpty(list)) {
                    list.add(0, archive);
                } else {
                    list.add(archive);
                }
                Message msg = mHandler.obtainMessage();
                msg.what = Constants.MSG_SUCCESS;
                msg.obj = list;
                mHandler.sendMessage(msg);
            }
        });
    }

    /**
     * 获取列表的分隔线
     * @param context
     * @return
     */
    private RecyclerView.ItemDecoration getItemDecoration(Context context) {
        if (mItemDecoration == null) {
            mItemDecoration = new DividerItemDecoration(context, R.drawable.divider_horizontal, DividerItemDecoration.VERTICAL_LIST);
        }
        return mItemDecoration;
    }

    /**
     * 删除文件夹
     * @author huanghui1
     * @update 2016/6/25 11:43
     * @version: 1.0.0
     */
    private void sureDeleteFolder(final Folder folder) {
        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setTitle(R.string.prompt)
                .setMessage(R.string.confirm_fodler_to_trash)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean success = mFolderManager.deleteFolder(folder);
                        if (!success) { //删除失败
                            SystemUtil.makeShortToast(R.string.delete_result_error);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    /**
     * 显示文件夹的详情
     * @author huanghui1
     * @update 2016/6/29 20:57
     * @version: 1.0.0
     */
    private void showFolderInfo(final Folder folder) {
        String info = folder.getInfo(mContext);
        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setTitle(folder.getName())
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
    
    /**
     * 刷新界面
     * @param list
     * @param refreshHelper
     */
    private void refreshUI(List<Folder> list, AdapterRefreshHelper refreshHelper) {
        if (!SystemUtil.isEmpty(list)) {  //有数据
            if (mFolderAdapter == null) {
                initFolderAdapter(mContext, mFolders);
            }
            if (refreshHelper == null || refreshHelper.type == AdapterRefreshHelper.TYPE_NONE) {
                mFolderAdapter.notifyDataSetChanged();
            } else {
                refreshHelper.refresh(mFolderAdapter);
            }
        }
    }
    
    /**
     * 初始化文件夹的适配器
     * @author huanghui1
     * @update 2016/6/25 15:09
     * @version: 1.0.0
     */
    private void initFolderAdapter(final Context context, List<Folder> list) {
        mFolderAdapter = new FolderAdapter(context, list, this);
        mRecyclerView.setAdapter(mFolderAdapter);
        mFolderAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View view) {
                Integer pos = (Integer) view.getTag(R.integer.item_tag_data);
                if (pos == null) {
                    return false;
                }
                final Folder folder = mFolders.get(pos);
                if (folder != null) {
                    AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
                    builder.setTitle(folder.getName())
                            .setItems(R.array.menu_folder_list, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: //删除
                                            sureDeleteFolder(folder);
                                            break;
                                        case 1: //详情
                                            showFolderInfo(folder);
                                            break;
                                    }
                                }
                            }).show();
                }
                return false;
            }
        });
        mFolderAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view) {
                Integer pos = (Integer) view.getTag(R.integer.item_tag_data);
                if (pos == null) {
                    return;
                }
                final Folder folder = mFolders.get(pos);
                if (folder == null || (mFolders.size() == 1 && folder.isEmpty())) {
                    return;
                }
                if (folder.isEmpty()) { //所有的文件，则更新改文件夹的显示状态
                    mFolderManager.updateShowState(mContext, !isShowFolderAll());
                    FolderViewHolder holder = (FolderViewHolder) view.getTag();
                    if (holder == null) {
                        return;
                    }
                    //更新后
                    if (isShowFolderAll()) {    //显示“所有文件夹项”
                        holder.mIvState.setImageResource(R.drawable.ic_visibility);
                    } else {
                        holder.mIvState.setImageResource(R.drawable.ic_visibility_off);
                    }
                    
                } else {    //其他普通文件夹，则进入编辑文件夹的界面
                    Intent intent = new Intent(mContext, FolderEditActivity.class);
                    intent.putExtra(Constants.ARG_CORE_OBJ, folder);
                    startActivity(intent);
                }
            }
        });
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mFolderAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }
    
    /**
     * 添加笔记的文件夹
     * @author huanghui1
     * @update 2016/6/23 21:47
     * @version: 1.0.0
     */
    private void addFolder(Folder folder) {
        mFolders.add(folder);
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        if (folder.isDefault()) {
            refreshHelper.type = AdapterRefreshHelper.TYPE_NONE;
        } else {
            refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        }
        refreshHelper.position = mFolders.size() - 1;
        refreshUI(mFolders, refreshHelper);
    }

    /**
     * 删除文件夹
     * @author huanghui1
     * @update 2016/6/23 21:55
     * @version: 1.0.0
     */
    private void deleteFolder(Folder folder) {
        int index = mFolders.indexOf(folder);
        if (index != -1) {  //列表中存在
            mFolders.remove(index);
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
            refreshHelper.position = index;
            refreshUI(mFolders, refreshHelper);
        }
    }
    
    /**
     * 更新笔记的文件夹
     * @author huanghui1
     * @update 2016/6/23 21:47
     * @version: 1.0.0
     */
    private void updateFolder(Folder folder) {
        int index = mFolders.indexOf(folder);
        if (index != -1) {
            Folder tFolder = mFolders.get(index);
            tFolder.setCount(folder.getCount());
            tFolder.setSyncState(folder.getSyncState());
            tFolder.setDeleteState(folder.getDeleteState());
            tFolder.setSort(folder.getSort());
            tFolder.setName(folder.getName());
            tFolder.setIsLock(folder.isLock());
            tFolder.setModifyTime(folder.getModifyTime());
            
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            if (folder.isDefault()) {
                refreshHelper.type = AdapterRefreshHelper.TYPE_NONE;
            } else {
                refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
            }
            refreshHelper.position = index;
            refreshUI(mFolders, refreshHelper);
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    /**
     * 文件夹的viewholder
     */
    class FolderViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        
        ImageView mIvIcon;
        TextView mTvName;
        TextView mTvCount;
        ImageView mIvState;

        public FolderViewHolder(View itemView) {
            super(itemView);

            mIvIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            mTvName = (TextView) itemView.findViewById(R.id.tv_name);
            mTvCount = (TextView) itemView.findViewById(R.id.tv_count);
            mIvState = (ImageView) itemView.findViewById(R.id.iv_state);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundResource(R.drawable.common_background_selector);
        }
    }

    /**
     * 文件夹的适配器
     */
    class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> implements ItemTouchHelperAdapter {

        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<Folder> mList;
        
        private int mTextColor;
        
        private OnItemLongClickListener mOnItemLongClickListener;
        
        private OnItemClickListener mOnItemClickListener;

        private final OnStartDragListener mDragStartListener;
        
        public FolderAdapter(Context context, List<Folder> list, OnStartDragListener dragStartListener) {
            this.mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
            this.mList = list;
            this.mDragStartListener = dragStartListener;
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.mOnItemLongClickListener = onItemLongClickListener;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        @Override
        public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_folder_layout, parent, false);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Integer pos = (Integer) v.getTag(R.integer.item_tag_data);
                    if (pos == null) {
                        return false;
                    }
                    Folder folder = mList.get(pos);
                    if (folder != null && !folder.isEmpty()) {
                        if (mOnItemLongClickListener != null) {
                            return mOnItemLongClickListener.onItemLongClick(v);
                        }
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


            FolderViewHolder holder = new FolderViewHolder(view);

            holder.mIvState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Boolean isUnPreFolder = (Boolean) v.getTag(R.integer.item_tag_data);
                    if (isUnPreFolder == null) {
                        return;
                    }
                    if (isUnPreFolder) {
                        mFolderManager.updateShowState(mContext, !isShowFolderAll());
                        //更新后
                        if (isShowFolderAll()) {    //显示“所有文件夹项”
                            ((ImageView)v).setImageResource(R.drawable.ic_visibility);
                        } else {
                            ((ImageView)v).setImageResource(R.drawable.ic_visibility_off);
                        }
                    }
                }
            });

            // Start a drag whenever the handle view it touched
            holder.mIvState.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Boolean isUnPreFolder = (Boolean) v.getTag(R.integer.item_tag_data);
                    if (isUnPreFolder == null) {
                        return false;
                    }
                    FolderViewHolder viewHolder = (FolderViewHolder) v.getTag();
                    if (!isUnPreFolder && MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        mDragStartListener.onStartDrag(viewHolder);
                    }
                    return false;
                }
            });

            holder.mIvState.setTag(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(final FolderViewHolder holder, int position) {
            final Folder folder = mList.get(position);
            
            if (folder == null) {
                return;
            }

            KLog.d(TAG, "---onBindViewHolder---position--" + position);
            
            holder.itemView.setTag(holder);
            holder.itemView.setTag(R.integer.item_tag_data, holder.getAdapterPosition());
            //是否是保存在数据库的文件夹，false：没有保存，如比“所有文件夹”
            final boolean isUnPersistentFolder = folder.isEmpty();
            holder.mIvState.setTag(R.integer.item_tag_data, isUnPersistentFolder);
            if (mTextColor == 0) {
                mTextColor = holder.mTvName.getCurrentTextColor();
            }
            String defaultFoldersId = getDefaultFolderSid();
            
            if (isUnPersistentFolder) { //“所有文件夹”
                final boolean isShowAll = isShowFolderAll();
                if (isShowAll) {    //显示“所有文件夹项”
                    holder.mIvState.setImageResource(R.drawable.ic_visibility);
                } else {
                    holder.mIvState.setImageResource(R.drawable.ic_visibility_off);
                }
                
            } else {    //其他文件夹
                holder.mIvState.setImageResource(R.drawable.ic_reorder_grey);
                
            }
            
            holder.mTvName.setText(folder.getName());
            holder.mTvCount.setText(String.valueOf(folder.getCount()));
            if (!TextUtils.isEmpty(defaultFoldersId) && defaultFoldersId.equals(folder.getSId())) {   //默认的文件夹
                holder.mTvName.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
            } else if (mTextColor != 0) {
                holder.mTvName.setTextColor(mTextColor);
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            if (toPosition == 0) {
                return false;
            } else {
                Folder fromFolder = mList.get(fromPosition);
                Folder toFolder = mList.get(toPosition);
                Collections.swap(mList, fromPosition, toPosition);
                updateSort(fromFolder, toFolder);

                AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                refreshHelper.type = AdapterRefreshHelper.TYPE_SWAP;
                refreshHelper.fromPosition = fromPosition;
                refreshHelper.toPosition = toPosition;
                refreshUI(mList, refreshHelper);
                return true;
            }
        }

        @Override
        public void onItemDismiss(int position) {

        }
    }
    
    /**
     * 更新文件夹的排序
     * @param fromFolder 排序的文件夹
     * @param toFolder 排序后的文件夹                  
     * @author huanghui1
     * @update 2016/6/25 16:07
     * @version: 1.0.0
     */
    private void updateSort(final Folder fromFolder, final Folder toFolder) {
        int fromSort = fromFolder.getSort();
        int toSort = toFolder.getSort();
        fromFolder.setSort(toSort);
        toFolder.setSort(fromSort);
        
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                mFolderManager.sortFolder(fromFolder, toFolder);
            }
        });
        
    }
    
    /**
     * 文件夹的观察者
     * @author huanghui1
     * @update 2016/6/23 15:59
     * @version: 1.0.0
     */
    class FolderContentObserver extends ContentObserver {

        public FolderContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            Folder folder = null;
            switch (notifyFlag) {
                case Provider.FolderColumns.NOTIFY_FLAG:    //笔记文件夹的添加
                    if (data == null || !(data instanceof Folder)) {
                        return;
                    }
                    folder = (Folder) data;
                    switch (notifyType) {
                        case ADD:   //笔记添加
                            Log.d(TAG, "---addFolder--" + folder);
                            addFolder(folder);
                            break;
                        case UPDATE:    //更新
                            if (!folder.isEmpty()) {
                                Log.d(TAG, "---updateFolder--" + folder);
                                updateFolder(folder);
                            }
                            break;
                        case DELETE:    //删除
                            Log.d(TAG, "---deleteFolder--" + folder);
                            deleteFolder(folder);
                            break;
                    }
                    break;
            }
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<FolderListActivity> mTarget;

        public MyHandler(FolderListActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            FolderListActivity target = mTarget.get();
            if (target != null) {
                switch (msg.what) {
                    case Constants.MSG_SUCCESS:    //笔记内容加载成功
                        target.mRefresher.setRefreshing(false);
                        List<Folder> list = (List<Folder>) msg.obj;
                        target.mFolders.clear();
                        target.mFolders.addAll(list);
                        target.refreshUI(target.mFolders, null);
                        break;
                }
            }
        }
    }
}
