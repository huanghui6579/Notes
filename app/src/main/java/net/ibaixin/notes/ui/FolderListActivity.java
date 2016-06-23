package net.ibaixin.notes.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.ContentObserver;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.helper.AdapterRefreshHelper;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.persistent.FolderManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.DividerItemDecoration;
import net.ibaixin.notes.widget.LayoutManagerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 笔记文件夹的列表界面
 * @author huanghui1
 * @update 2016/6/23 11:45
 * @version: 1.0.0
 */
public class FolderListActivity extends BaseActivity {
    
    private FolderManager mFolderManager;
    
    private List<Folder> mFolders;

    private SwipeRefreshLayout mRefresher;

    private RecyclerView mRecyclerView;

    private RecyclerView.ItemDecoration mItemDecoration;
    
    private FolderAdapter mFolderAdapter;

    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;
    
    private FolderContentObserver mFolderObserver;
    
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
     * 刷新界面
     * @param list
     * @param refreshHelper
     */
    private void refreshUI(List<Folder> list, AdapterRefreshHelper refreshHelper) {
        if (!SystemUtil.isEmpty(list)) {  //有数据
            if (mFolderAdapter == null) {
                mFolderAdapter = new FolderAdapter(mContext, list);
                mRecyclerView.setAdapter(mFolderAdapter);
            }
            if (refreshHelper == null || refreshHelper.type == AdapterRefreshHelper.TYPE_NONE) {
                mFolderAdapter.notifyDataSetChanged();
            } else {
                refreshHelper.refresh(mFolderAdapter);
            }
        }
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
        refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
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
            tFolder.setIsDefault(folder.isDefault());
            tFolder.setIsHidden(folder.isHidden());
            tFolder.setIsLock(folder.isLock());
            tFolder.setModifyTime(folder.getModifyTime());
            
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
            refreshHelper.position = index;
            refreshUI(mFolders, refreshHelper);
        }
    }

    /**
     * 文件夹的viewholder
     */
    class FolderViewHolder extends RecyclerView.ViewHolder {
        
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
    }

    /**
     * 文件夹的适配器
     */
    class FolderAdapter extends RecyclerView.Adapter<FolderViewHolder> {

        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<Folder> mList;
        
        public FolderAdapter(Context context, List<Folder> list) {
            this.mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
            this.mList = list;
        }

        @Override
        public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_folder_layout, parent, false);
            return new FolderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FolderViewHolder holder, int position) {
            Folder folder = mList.get(position);
            if (folder != null) {
                holder.mTvName.setText(folder.getName());
                holder.mTvCount.setText(String.valueOf(folder.getCount()));
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
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
                            if (folder != null) {
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
