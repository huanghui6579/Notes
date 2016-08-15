package com.yunxinlink.notes.widget;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import com.nostra13.universalimageloader.core.imageaware.ViewAware;
import com.yunxinlink.notes.util.ImageUtil;

/**
 * @author huanghui1
 * @update 2016/8/15 11:08
 * @version: 0.0.1
 */
public class NoteItemViewAware extends ViewAware {
    public NoteItemViewAware(View view) {
        super(view, false);
    }

    public NoteItemViewAware(View view, boolean checkActualViewSize) {
        super(view, checkActualViewSize);
    }

    @Override
    protected void setImageDrawableInto(Drawable drawable, View view) {
        if (drawable != null && view != null) {
            setBackground(view, drawable);
        }
    }

    @Override
    protected void setImageBitmapInto(Bitmap bitmap, View view) {
        if (bitmap != null && view != null) {
            Drawable drawable = ImageUtil.bitmap2Drawable(view.getContext(), bitmap);
            setBackground(view, drawable);
        }
    }

    /**
     * 设置背景
     * @param view
     * @param background
     */
    private void setBackground(View view, Drawable background) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
    }
}
