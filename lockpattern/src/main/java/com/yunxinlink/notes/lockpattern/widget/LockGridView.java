package com.yunxinlink.notes.lockpattern.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * 让GridView的高度为Wrap_content
 * Read more: http://blog.chengyunfeng.com/?p=444#ixzz4JFwVZlWx
 * @author tiger
 * @version 1.0.0
 * @update 2016/9/4 11:56
 */
public class LockGridView extends GridView {
    public LockGridView(Context context) {
        super(context);
    }

    public LockGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec;

        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            // The great Android "hackatlon", the love, the magic.
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            heightSpec = MeasureSpec.makeMeasureSpec(
                    Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        } else {
            // Any other height should be respected as is.
            heightSpec = heightMeasureSpec;
        }

        super.onMeasure(widthMeasureSpec, heightSpec);
    }
}
