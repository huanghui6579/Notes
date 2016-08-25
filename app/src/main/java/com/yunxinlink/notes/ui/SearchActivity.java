package com.yunxinlink.notes.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.listener.OnItemClickListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;
import com.yunxinlink.notes.widget.DividerItemDecoration;
import com.yunxinlink.notes.widget.LayoutManagerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索的界面
 * @author huanghui1
 * @update 2016/8/25 11:08
 * @version: 1.0.0
 */
public class SearchActivity extends BaseActivity implements SearchView.OnQueryTextListener, OnItemClickListener {
    private static final int MSG_SEARCH = 1;

    /**
     * 搜索的单线程队列
     */
    private Handler mSearchHandler;

    /**
     * 刷新界面的handler
     */
    private Handler mHandler;
    
    private List<DetailNoteInfo> mNotes;
    private NoteAdapter mNoteAdapter;
    private RecyclerView mRecyclerView;

    @Override
    protected int getContentView() {
        return R.layout.activity_search;
    }

    @Override
    protected void initData() {
        mNotes = new ArrayList<>();
    }
    
    @Override
    protected void initView() {
        initSearchThread();
        
        mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_search, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        initSearchMenu(searchItem);
        
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 初始化搜索的线程
     */
    private void initSearchThread() {
        mHandler = new MyHandler(this);

        //初始化搜索的线程
        HandlerThread handlerThread = new HandlerThread("SearchThread");
        handlerThread.start();

        mSearchHandler = new SearchHandler(handlerThread.getLooper());
    }

    /**
     * 显示笔记
     * @param list
     */
    private void showNotes(List<DetailNoteInfo> list) {
        mNotes.clear();
        if (list != null && list.size() > 0) {
            mNotes.addAll(list);
        }
        
        if (mNoteAdapter == null) {
            mNoteAdapter = new NoteAdapter(mNotes, mContext);
            mNoteAdapter.setOnItemClickListener(this);

            LayoutManagerFactory managerFactory = new LayoutManagerFactory();
            RecyclerView.LayoutManager layoutManager = managerFactory.getLayoutManager(mContext, false);
            mRecyclerView.setLayoutManager(layoutManager);
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContext, LinearLayoutManager.HORIZONTAL);
            mRecyclerView.addItemDecoration(dividerItemDecoration);
            mRecyclerView.setAdapter(mNoteAdapter);
        }
        mNoteAdapter.notifyDataSetChanged();
    }

    /**
     * 初始化搜索菜单
     * @param menuItem
     */
    private void initSearchMenu(MenuItem menuItem) {
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        //默认展开
        searchView.setIconified(false);
        
        searchView.setQueryHint(getString(R.string.note_search_hint));

        searchView.setOnQueryTextListener(this);

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        KLog.d(TAG, "----query----" + query);
        searchNotes(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        KLog.d(TAG, "----newText----" + newText);
        searchNotes(newText);
        return true;
    }

    /**
     * 提交搜索笔记的后台任务
     * @param keyword 笔记的关键字
     */
    private void searchNotes(String keyword) {
        //先移除队列中已存在的任务
        mSearchHandler.removeMessages(MSG_SEARCH);
        if (TextUtils.isEmpty(keyword)) {   //清除
            showNotes(null);
            return;
        }

        Message msg = mSearchHandler.obtainMessage();
        msg.what = MSG_SEARCH;
        msg.obj = keyword;
        mSearchHandler.sendMessage(msg);
    }

    /**
     * 搜索笔记
     * @param keyword 关键字
     * @return
     */
    private List<DetailNoteInfo> findNotes(String keyword) {
        User user = getCurrentUser();
        NoteManager noteManager = NoteManager.getInstance();
        return noteManager.findNotes(user, keyword);
    }

    @Override
    public void onItemClick(View view) {
        
    }

    /**
     * 笔记列表的holder
     */
    class NoteViewHolder extends RecyclerView.ViewHolder {

        ImageView mIvIcon;
        TextView mTvContent;
        TextView mTvTime;

        public NoteViewHolder(View itemView) {
            super(itemView);

            mIvIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            mTvContent = (TextView) itemView.findViewById(R.id.tv_content);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
        }
    }

    /**
     * 笔记列表的适配器
     */
    class NoteAdapter extends RecyclerView.Adapter<NoteViewHolder> {
        private List<DetailNoteInfo> mList;
        
        private LayoutInflater mInflater;

        private OnItemClickListener mOnItemClickListener;

        //图片加载失败的图片
        private ItemAttachIcon mItemAttachIcon;
        //着色的颜色
        private int mTintColor;

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        public NoteAdapter(List<DetailNoteInfo> list, Context context) {
            this.mList = list;
            mInflater = LayoutInflater.from(context);

            mTintColor = initTintColor();

            mItemAttachIcon = new ItemAttachIcon();
        }

        @Override
        public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_search_list, parent, false);

            NoteViewHolder viewHolder = new NoteViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v);
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(NoteViewHolder holder, int position) {
            DetailNoteInfo detailNote = mList.get(position);
            NoteInfo note = detailNote.getNoteInfo();

            if (note == null) {
                return;
            }
            
            holder.itemView.setTag(holder.getAdapterPosition());

            //重置附件图标的各种状态
            resetAttachView(holder);

            holder.mTvContent.setText(note.getStyleContent(true, detailNote.getDetailList()));
            holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getShowTime(true)));

            if (note.hasAttach() && detailNote.getLastAttach() != null) {
                SystemUtil.setViewVisibility(holder.mIvIcon, View.VISIBLE);
                showAttachIcon(detailNote, holder);
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        /**
         * 初始化着色的颜色
         * @return
         */
        private int initTintColor() {
            Resources resources = getResources();
            return ResourcesCompat.getColor(resources, R.color.text_time_color, getTheme());
        }

        /**
         * 重置附件图标的各种状态
         * @param holder
         */
        private void resetAttachView(RecyclerView.ViewHolder holder) {
            NoteViewHolder listHolder = (NoteViewHolder) holder;
            listHolder.mIvIcon.setImageResource(0);
            SystemUtil.setViewVisibility(listHolder.mIvIcon, View.GONE);
        }

        /**
         * 显示附件的图标
         * @param detailNote
         * @param holder
         */
        private void showAttachIcon(DetailNoteInfo detailNote, NoteViewHolder holder) {
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
         * 初始化图片加载时候的图片
         * @return
         */
        private Drawable initFailedImage(int color) {
            Resources resources = getResources();
            Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_broken_image, getTheme());
            drawable = getTintDrawable(drawable, color);
            return drawable;
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
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mMusicDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mMusicDrawable;
                    break;
                case Attach.VIDEO:
                    if (mItemAttachIcon.mVideoDrawable == null) {
                        resId = R.drawable.ic_library_music;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mVideoDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mVideoDrawable;
                    break;
                case Attach.ARCHIVE:
                    if (mItemAttachIcon.mArchiveDrawable == null) {
                        resId = R.drawable.ic_library_archive;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mArchiveDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mArchiveDrawable;
                    break;
                case Attach.FILE:
                    if (mItemAttachIcon.mFileDrawable == null) {
                        resId = R.drawable.ic_library_file;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mFileDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mFileDrawable;
                    break;
            }
            return drawable;
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

    /**
     * 更新界面的handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<SearchActivity> mTarget;

        public MyHandler(SearchActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            SearchActivity target = mTarget.get();
            switch (msg.what) {
                case Constants.MSG_SUCCESS2:    //搜索完成，更新界面
                    List<DetailNoteInfo> list = (List<DetailNoteInfo>) msg.obj;
                    target.showNotes(list);
                    break;
            }
        }
    }

    /**
     * 搜索的handler
     */
    class SearchHandler extends Handler {

        public SearchHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEARCH:    //搜索
                    String keyword = (String) msg.obj;
                    List<DetailNoteInfo> list = findNotes(keyword);
                    
                    //搜索到结果后，在ui线程里更新
                    Message message = mHandler.obtainMessage();
                    message.what = Constants.MSG_SUCCESS2;
                    message.obj = list;
                    mHandler.sendMessage(message);
                    break;
            }
        }
    }
    
}
