package net.ibaixin.notes.listener;

import android.view.View;

/**
 * 列表每一项点击的监听器
 * @author huanghui1
 * @update 2016/2/24 19:19
 * @version: 0.0.1
 */
public interface OnItemClickListener {
    /**
     * 列表每一项点击的事件
     * @param view item 的view
     * @param position 点击的位置索引            
     * @author huanghui1
     * @update 2016/2/24 19:20
     * @version: 1.0.0
     */
    public void onItemClick(View view, int position);
}
