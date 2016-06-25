package net.ibaixin.notes.ui;

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

import net.ibaixin.notes.R;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.ContentObserver;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.helper.AdapterRefreshHelper;
import net.ibaixin.notes.helper.ItemTouchHelperAdapter;
import net.ibaixin.notes.helper.ItemTouchHelperViewHolder;
import net.ibaixin.notes.helper.OnStartDragListener;
import net.ibaixin.notes.helper.SimpleItemTouchHelperCallback;
import net.ibaixin.notes.listener.OnItemClickListener;
import net.ibaixin.notes.listener.OnItemLongClickListener;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.persistent.FolderManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.DividerItemDecoration;
import net.ibaixin.notes.widget.LayoutManagerFactory;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
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
            public boolean onItemLongClick(View view, int position) {
                final Folder folder = mFolders.get(position);
                if (folder != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(folder.getName())
                            .setItems(R.array.menu_folder_list, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case 0: //删除
                                            sureDeleteFolder(folder);
                                            break;
                                        case 1: //详情
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
            public void onItemClick(View view, int position) {
                final Folder folder = mFolders.get(position);
                if (folder == null) {
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
            return new FolderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final FolderViewHolder holder, int position) {
            final Folder folder = mList.get(position);
            holder.itemView.setTag(holder);
            if (folder != null) {
                //是否是保存在数据库的文件夹，false：没有保存，如比“所有文件夹”
                final boolean isUnPersistentFolder = folder.isEmpty();
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (!isUnPersistentFolder) {
                            if (mOnItemLongClickListener != null) {
                                return mOnItemLongClickListener.onItemLongClick(v, holder.getAdapterPosition());
                            }
                        }
                        return false;
                    }
                });
                
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(v, holder.getAdapterPosition());
                        }
                    }
                });
                
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

                holder.mIvState.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isUnPersistentFolder) {
                            mFolderManager.updateShowState(mContext, !isShowFolderAll());
                            //更新后
                            if (isShowFolderAll()) {    //显示“所有文件夹项”
                                holder.mIvState.setImageResource(R.drawable.ic_visibility);
                            } else {
                                holder.mIvState.setImageResource(R.drawable.ic_visibility_off);
                            }
                        }
                    }
                });
                
                // Start a drag whenever the handle view it touched
                holder.mIvState.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (!isUnPersistentFolder && MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                            mDragStartListener.onStartDrag(holder);
                        }
                        return false;
                    }
                });
                
                holder.mTvName.setText(folder.getName());
                holder.mTvCount.setText(String.valueOf(folder.getCount()));
                if (!TextUtils.isEmpty(defaultFoldersId) && defaultFoldersId.equals(folder.getSId())) {   //默认的文件夹
                    holder.mTvName.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
                } else if (mTextColor != 0) {
                    holder.mTvName.setTextColor(mTextColor);
                }
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
                notifyItemMoved(fromPosition, toPosition);
                updateSort(fromFolder, toFolder);
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
                    if (data != null) {
                        folder = (Folder) data;
                    }
                    switch (notifyType) {
                        case ADD:   //笔记添加
                            if (folder != null) {
                                Log.d(TAG, "---addFolder--" + folder);
                                addFolder(folder);
                            }
                            break;
                        case UPDATE:    //更新
                            if (folder != null && !folder.isEmpty()) {
                                Log.d(TAG, "---updateFolder--" + folder);
                                updateFolder(folder);
                            }
                            break;
                        case DELETE:    //删除
                            if (folder != null) {
                                Log.d(TAG, "---deleteFolder--" + folder);
                                deleteFolder(folder);
                            }
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
