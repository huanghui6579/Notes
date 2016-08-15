package com.yunxinlink.notes.helper;

import android.support.v7.widget.RecyclerView;

/**
 * @author huanghui1
 * @update 2016/6/23 10:37
 * @version: 0.0.1
 */
public class AdapterRefreshHelper {
    //adapter的刷新方式
    public static final int TYPE_NONE = 0;
    public static final int TYPE_ADD = 1;
    public static final int TYPE_UPDATE = 2;
    public static final int TYPE_DELETE = 3;
    public static final int TYPE_SWAP = 4;
    
    //刷新方式0：全部刷新，1：添加，2：更新单个，3：删除
    public int type;
    
    //刷新的位置
    public int position;
    
    public int fromPosition;
    
    public int toPosition;
    
    public void refresh(RecyclerView.Adapter adapter) {
        switch (type) {
            case AdapterRefreshHelper.TYPE_ADD:
                adapter.notifyItemInserted(position);
                adapter.notifyItemRangeChanged(position, adapter.getItemCount() - position);
                break;
            case AdapterRefreshHelper.TYPE_UPDATE:
                adapter.notifyItemChanged(position);
                break;
            case AdapterRefreshHelper.TYPE_DELETE:
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, adapter.getItemCount() - position);
                break;
            case AdapterRefreshHelper.TYPE_SWAP:
                adapter.notifyItemMoved(fromPosition, toPosition);
                adapter.notifyItemRangeChanged(fromPosition, toPosition - fromPosition + 1);
                break;
            default:
                adapter.notifyItemRangeChanged(0, adapter.getItemCount());
//                adapter.notifyDataSetChanged();
                break;
        }
    }
}
