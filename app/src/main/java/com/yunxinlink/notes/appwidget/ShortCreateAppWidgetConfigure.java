package com.yunxinlink.notes.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.widget.LayoutManagerFactory;
import com.yunxinlink.notes.widget.SpacesItemDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * 快速创建编辑项widget的配置界面
 * @author huanghui1
 * @update 2016/9/13 19:02
 * @version: 1.0.0
 */
public class ShortCreateAppWidgetConfigure extends BaseActivity implements OnStartDragListener, OnItemClickListener, View.OnClickListener {
    
    private RecyclerView mRecyclerView;
    
    private static List<WidgetItem> mWidgetItems;

    private ItemTouchHelper mItemTouchHelper;
    
    private Button mBtnOk;
    
    private String[] mNameArray = null;
    private int[] mResArray = {
            R.drawable.ic_note_add,
            R.drawable.ic_action_camera,
            R.drawable.ic_action_voice,
            R.drawable.ic_action_brush,
            R.drawable.ic_action_photo,
            R.drawable.ic_action_insert_file,
            R.drawable.ic_action_search
    };
    
    private int[] mWidgetTypeArray = {
            WidgetAction.NOTE_TEXT.ordinal(),
            WidgetAction.NOTE_CAMERA.ordinal(),
            WidgetAction.NOTE_VOICE.ordinal(),
            WidgetAction.NOTE_BRUSH.ordinal(),
            WidgetAction.NOTE_PHOTO.ordinal(),
            WidgetAction.NOTE_FILE.ordinal(),
            WidgetAction.NOTE_SEARCH.ordinal()
    };

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    @Override
    protected boolean hasLockedController() {
        return false;
    }

    @Override
    protected int getContentView() {
        return R.layout.appwidget_conf_short_create;
    }
    
    private void loadData() {
        mWidgetItems = new ArrayList<>();
        int size = mResArray.length;
        for (int i = 0; i < size; i++) {
            WidgetItem item = new WidgetItem();
            item.setName(mNameArray[i]);
            item.setResId(mResArray[i]);
            item.setSort(i + 1);
            item.setType(mWidgetTypeArray[i]);
            if (i < Constants.MAX_WIDGET_ITEM_SIZE) {
                item.setChecked(true);
            }
            
            mWidgetItems.add(item);
        }

        WidgetAdapter adapter = new WidgetAdapter(mWidgetItems, this, this);
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
        onCanceledConfigure();

        loadData();
    }

    @Override
    protected void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mNameArray = getResources().getStringArray(R.array.widget_item_names);

        mBtnOk.setOnClickListener(this);
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
        WidgetItem item = mWidgetItems.get(position);
        int firstUnIndex = getFirstUnSelected();
        if (item.isChecked()) {  //由选中变为没选中
            item.setChecked(false);
            tagHolder.itemView.setSelected(false);
            if (firstUnIndex != -1 && position != firstUnIndex - 1) {
                int toPosition = firstUnIndex - 1;
                toPosition = toPosition < 0 ? 0 : toPosition;
                swapItem(mWidgetItems, position, toPosition);
                mRecyclerView.getAdapter().notifyItemRangeChanged(position, Math.abs(toPosition - position) + 1);
            }
            SystemUtil.setViewEnable(mBtnOk, false);
        } else {    //由没选中到选中
            int selectedSize = getSelectedSize();
            if (selectedSize >= Constants.MAX_WIDGET_ITEM_SIZE) {   //已经选了5个了
                SystemUtil.makeShortToast(R.string.widget_item_max_tip);
                return;
            }
            item.setChecked(true);
            tagHolder.itemView.setSelected(true);
            
            if (position != firstUnIndex) {
                int toPosition = firstUnIndex;
                toPosition = toPosition >= mWidgetItems.size() ? mWidgetItems.size() - 1 : toPosition;
                swapItem(mWidgetItems, position, toPosition);
                mRecyclerView.getAdapter().notifyItemRangeChanged(toPosition, Math.abs(toPosition - position) + 1);
            }
            
            selectedSize ++;
            if (selectedSize >= Constants.MAX_WIDGET_ITEM_SIZE) {    //目前刚好满5个
                SystemUtil.setViewEnable(mBtnOk, true);
            }
        }
        
    }

    /**
     * 完成配置
     */
    private void onCompletedConfigure() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);//从intent中得出widgetId  
            //通知 appwidget 的配置已完成  
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            ShortCreateAppWidget.updateAppWidget(mContext, AppWidgetManager.getInstance(mContext), widgetId);
            setResult(RESULT_OK, result);
            KLog.d(TAG, "onCompletedConfigure invoke");
            finish();
        }
    }

    /**
     * 取消配置
     */
    private void onCanceledConfigure() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);//从intent中得出widgetId 
            //通知 appwidget 的配置已取消  
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_CANCELED, result);
            KLog.d(TAG, "onCanceledConfigure invoke");
        }
    }

    /**
     * 获取选中的数量
     * @return
     */
    private int getSelectedSize() {
        int size = 0;
        for (WidgetItem item : mWidgetItems) {
            if (item.isChecked()) {
                size ++;
            }
        }
        return size;
    }

    /**
     * 获取选择的item项数组
     * @return
     */
    static WidgetItem[] getSelectedItems() {
        if (SystemUtil.isEmpty(mWidgetItems)) {
            KLog.d(TAG, "getSelectedItems mWidgetItems is null");
            return null;
        }
        WidgetItem[] items = new WidgetItem[Constants.MAX_WIDGET_ITEM_SIZE];
        for (int i = 0; i < Constants.MAX_WIDGET_ITEM_SIZE; i++) {
            items[i] = mWidgetItems.get(i);
        }
        return items;
    }

    /**
     * 元素交换
     * @param list
     * @param fromPosition
     * @param toPosition
     * @return
     */
    private boolean swapItem(List<WidgetItem> list, int fromPosition, int toPosition) {
        WidgetItem fromItem = list.get(fromPosition);
        list.remove(fromPosition);
        list.add(toPosition, fromItem);
//        Collections.swap(list, fromPosition, toPosition);

        //最小的位置
        int fromPos = Math.min(fromPosition, toPosition);
        int size = list.size() - fromPos;
        for (int i = fromPos; i < size; i++) {
            WidgetItem item = list.get(i);
            item.setSort(i + 1);
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
     * 获取第一个没有选中的索引
     * @return
     */
    private int getFirstUnSelected() {
        int size = mWidgetItems.size();
        int index = -1;
        for (int i = 0; i < size; i++) {
            WidgetItem item = mWidgetItems.get(i);
            if (!item.isChecked()) {
                index = i;
                break;
            }
        }
        return index;
    }

    @Override
    public void onClick(View v) {
        onCompletedConfigure();
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
            View view = mInflater.inflate(R.layout.item_appwidget_conf_short_create, parent, false);
            WidgetViewHolder holder = new WidgetViewHolder(view);
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Integer pos = (Integer) v.getTag(R.integer.item_tag_data);
                    if (pos == null) {
                        return false;
                    }
                    WidgetItem item = mList.get(pos);
                    if (!item.isChecked()) {
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
            holder.ivIcon.setImageResource(item.getResId());
            holder.tvDesc.setText(item.getName());

            holder.itemView.setSelected(item.isChecked());
            
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            WidgetItem toItem = mList.get(toPosition);
            if (!toItem.isChecked()) {
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
