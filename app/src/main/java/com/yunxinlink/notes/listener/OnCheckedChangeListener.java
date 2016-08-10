package com.yunxinlink.notes.listener;

import android.widget.CompoundButton;

/**
 * 复选框选择的监听器
 * @author huanghui1
 * @update 2016/7/11 14:51
 * @version: 0.0.1
 */
public interface OnCheckedChangeListener {
    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The compound button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
}
