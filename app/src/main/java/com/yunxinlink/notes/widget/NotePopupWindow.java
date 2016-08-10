package com.yunxinlink.notes.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatPopupWindow;
import android.util.AttributeSet;

/**
 * @author huanghui1
 * @update 2016/7/19 20:50
 * @version: 0.0.1
 */
public class NotePopupWindow extends AppCompatPopupWindow {

    public NotePopupWindow(Context context) {
        this(context, null, android.support.v7.appcompat.R.attr.popupMenuStyle);
    }
    public NotePopupWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
