package net.ibaixin.notes.listener;

import android.view.View;

/**
 * 列表每一项的长按事件
 * @author huanghui1
 * @update 2016/6/25 11:31
 * @version: 0.0.1
 */
public interface OnItemLongClickListener {
    /**
     * 长按事件
     * @param view
     */
    public boolean onItemLongClick(View view);
}
