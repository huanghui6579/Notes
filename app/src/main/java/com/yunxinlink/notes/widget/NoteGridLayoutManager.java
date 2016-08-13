package com.yunxinlink.notes.widget;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.socks.library.KLog;

/**
 * @author huanghui1
 * @update 2016/8/13 17:28
 * @version: 0.0.1
 */
public class NoteGridLayoutManager extends GridLayoutManager {
    public NoteGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NoteGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public NoteGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state, int widthSpec, int heightSpec) {

        try {
            int height = 0;
            int childCount = getItemCount();
            for (int i = 0; i < childCount; i++) {
                View child = recycler.getViewForPosition(i);
                measureChild(child, widthSpec, heightSpec);
                if (i % getSpanCount() == 0) {
                    int measuredHeight = child.getMeasuredHeight() + getDecoratedBottom(child);
                    height += measuredHeight;
                }
            }
            setMeasuredDimension(View.MeasureSpec.getSize(widthSpec), height);
        } catch (Exception e) {
            KLog.e(e);
            e.printStackTrace();
            super.onMeasure(recycler, state, widthSpec, heightSpec);
        }
    }
}
