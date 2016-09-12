package com.yunxinlink.notes.test;

import android.content.Context;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.helper.AdapterRefreshHelper;
import com.yunxinlink.notes.helper.ItemTouchHelperAdapter;
import com.yunxinlink.notes.helper.ItemTouchHelperViewHolder;
import com.yunxinlink.notes.helper.OnStartDragListener;
import com.yunxinlink.notes.helper.SimpleItemTouchHelperCallback;
import com.yunxinlink.notes.listener.OnItemClickListener;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.widget.LayoutManagerFactory;
import com.yunxinlink.notes.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestWidgetActivity extends BaseActivity implements OnStartDragListener, OnItemClickListener, View.OnClickListener {
    
    private RecyclerView mRecyclerView;
    
    private List<WidgetItem> items;

    private ItemTouchHelper mItemTouchHelper;
    
    private Button btnOk;
    
    private String[] nameArray = null;
    private int[] resArray = {
            R.drawable.ic_note_add,
            R.drawable.ic_action_camera,
            R.drawable.ic_action_voice,
            R.drawable.ic_action_brush,
            R.drawable.ic_action_photo,
            R.drawable.ic_action_insert_file,
            R.drawable.ic_action_search
    };

    @Override
    protected void initWindow() {
        AppCompatDelegate appCompatDelegate = getDelegate();
        appCompatDelegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_test_widget;
    }
    
    private void loadData() {
        items = new ArrayList<>();
        int size = resArray.length;
        for (int i = 0; i < size; i++) {
            WidgetItem item = new WidgetItem();
            item.name = nameArray[i];
            item.resId = resArray[i];
            item.sort = i + 1;
            
            if (i < 5) {
                item.isChecked = true;
            }
            
            items.add(item);
        }

        WidgetAdapter adapter = new WidgetAdapter(items, this, this);
        LayoutManagerFactory layoutManagerFactory = new LayoutManagerFactory();
        layoutManagerFactory.setGridSpanCount(4);

        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(spacingInPixels));
        mRecyclerView.setLayoutManager(layoutManagerFactory.getLayoutManager(this, true));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(this);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    @Override
    protected void initData() {
        loadData();
    }

    @Override
    protected void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
        btnOk = (Button) findViewById(R.id.btn_ok);
        nameArray = getResources().getStringArray(R.array.widget_item_names);

        btnOk.setOnClickListener(this);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onItemClick(View view) {
        Integer position = (Integer) view.getTag(R.integer.item_tag_data);
        if (position == null) return;
        WidgetViewHolder tagHolder = (WidgetViewHolder) view.getTag();
        if (tagHolder == null) return;
        WidgetItem item = items.get(position);
        if (item.isChecked) {  //由选中变为没选中
            int lastIndex = getLastSelected();
            item.isChecked = false;
            tagHolder.itemView.setSelected(false);
            if (lastIndex != -1 && position != lastIndex - 1) {
                int toPosition = lastIndex - 1;
                toPosition = toPosition < 0 ? 0 : toPosition;
                swapItem(items, position, toPosition);
                mRecyclerView.getAdapter().notifyItemRangeChanged(position, Math.abs(toPosition - position) + 1);
            }
            SystemUtil.setViewEnable(btnOk, false);
        } else {    //由没选中到选中
            item.isChecked = true;
            tagHolder.itemView.setSelected(true);
            if (hasMaxItem()) {
                SystemUtil.setViewEnable(btnOk, true);
            }
        }
        
    }

    /**
     * 选中是否达到最大数额
     * @return
     */
    private boolean hasMaxItem() {
        int size = 0;
        for (WidgetItem item : items) {
            if (item.isChecked) {
                size ++;
            }
        }
        return size >= 5;
    }

    /**
     * 元素交换
     * @param list
     * @param fromPosition
     * @param toPosition
     * @return
     */
    private boolean swapItem(List<WidgetItem> list, int fromPosition, int toPosition) {
        Collections.swap(list, fromPosition, toPosition);

        //最小的位置
        int fromPos = Math.min(fromPosition, toPosition);
        int size = list.size() - fromPos;
        for (int i = fromPos; i < size; i++) {
            WidgetItem item = list.get(i);
            item.sort = i + 1;
        }

        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.type = AdapterRefreshHelper.TYPE_SWAP;
        refreshHelper.fromPosition = fromPosition;
        refreshHelper.toPosition = toPosition;
        refreshHelper.notify = false;
        refreshHelper.refresh(mRecyclerView.getAdapter());
        KLog.d(TAG, "list:" + list);
        return true;
    }

    /**
     * 获取最后一个选中的索引
     * @return
     */
    private int getLastSelected() {
        int size = items.size();
        int index = -1;
        for (int i = 0; i < size; i++) {
            WidgetItem item = items.get(i);
            if (!item.isChecked) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    public void onClick(View v) {
        
    }

    class WidgetItem {
        String name;
        int resId;
        int sort;
        
        boolean isChecked;

        @Override
        public String toString() {
            return "WidgetItem{" +
                    "name='" + name + '\'' +
                    ", sort=" + sort +
                    ", isChecked=" + isChecked +
                    '}';
        }
    }
    
    class WidgetViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        ImageView ivIcon;
        TextView tvDesc;
        View itemLayout;

        public WidgetViewHolder(View itemView) {
            super(itemView);

            itemLayout = itemView.findViewById(R.id.item_layout);
            ivIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            tvDesc = (TextView) itemView.findViewById(R.id.tv_desc);
        }

        @Override
        public void onItemSelected() {
            
        }

        @Override
        public void onItemClear() {

        }
    }
    
    class WidgetAdapter extends RecyclerView.Adapter<WidgetViewHolder>  implements ItemTouchHelperAdapter {
        private List<WidgetItem> mList;
        private LayoutInflater mInflater;
        private final OnStartDragListener mDragStartListener;
        private OnItemClickListener mOnItemClickListener;
        
        public WidgetAdapter(List<WidgetItem> list, Context context, OnStartDragListener dragStartListener) {
            this.mList = list;
            this.mDragStartListener = dragStartListener;
            mInflater = LayoutInflater.from(context);
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        @Override
        public WidgetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.test_item_widget, parent, false);
            WidgetViewHolder holder = new WidgetViewHolder(view);
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Integer pos = (Integer) v.getTag(R.integer.item_tag_data);
                    if (pos == null) {
                        return false;
                    }
                    WidgetItem item = mList.get(pos);
                    if (!item.isChecked) {
                        return false;
                    }
                    WidgetViewHolder tagHolder = (WidgetViewHolder) v.getTag();
                    if (tagHolder != null && mDragStartListener != null) {
                        mDragStartListener.onStartDrag(tagHolder);
                    }
                    return true;
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v);
                    }
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(WidgetViewHolder holder, int position) {
            holder.itemView.setTag(holder);
            holder.itemView.setTag(R.integer.item_tag_data, position);
            WidgetItem item = mList.get(position);
            holder.ivIcon.setImageResource(item.resId);
            holder.tvDesc.setText(item.name);

            holder.itemView.setSelected(item.isChecked);
            
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            WidgetItem toItem = mList.get(toPosition);
            if (!toItem.isChecked) {
                return false;
            }
            return swapItem(mList, fromPosition, toPosition);
        }

        @Override
        public void onItemDismiss(int position) {

        }

        @Override
        public void onItemCompleted() {
            KLog.d(TAG, "---onItemCompleted---");
            notifyDataSetChanged();
        }
    }
}
