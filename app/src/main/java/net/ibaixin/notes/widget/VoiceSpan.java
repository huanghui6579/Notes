package net.ibaixin.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.TimeUtil;

/**
 * 语音的span
 * @author huanghui1
 * @update 2016/7/15 9:49
 * @version: 0.0.1
 */
public class VoiceSpan extends DynamicDrawableSpan {

    private static final java.lang.String TAG = "VoiceSpan";
    private Context mContext;
    private Attach mAttach;
    private Drawable mdDrawable;
    
    public VoiceSpan(Context context, Attach attach) {
        this.mAttach = attach;
        this.mContext = context;

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view  = inflater.inflate(R.layout.layout_voice, null);
        TextView titleView = (TextView) view.findViewById(R.id.tv_title);
        TextView summaryView = (TextView) view.findViewById(R.id.tv_summary);
        titleView.setText(attach.getFilename());
        String summary = SystemUtil.formatFileSize(attach.getSize());
        if (Attach.VOICE == mAttach.getType() && mAttach.getDecription() != null) {    //录音
            summary += "  " + TimeUtil.formatMillis(Long.parseLong(attach.getDecription()));
        }
        summaryView.setText(summary);

        updateCacheDrawable(view);
    }

    @Override
    public Drawable getDrawable() {
        return mdDrawable;
    }

    /**
     * 更新背景图片
     * @param view
     */
    private void updateCacheDrawable(View view) {
        view.setDrawingCacheEnabled(true);
        int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(measureSpec, measureSpec);
        view.layout(0, 0, view.getMeasuredWidth(),
                view.getMeasuredHeight());

        Bitmap bitmap = view.getDrawingCache();

        BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);

        mdDrawable = drawable;
    }
}
