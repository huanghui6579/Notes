package net.ibaixin.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.style.DynamicDrawableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.TimeUtil;
import net.ibaixin.notes.util.log.Log;

import java.io.File;

/**
 * 语音的span
 * @author huanghui1
 * @update 2016/7/15 9:49
 * @version: 0.0.1
 */
public class FileSpan extends DynamicDrawableSpan {

    private static final java.lang.String TAG = "FileSpan";
    private Context mContext;
    private Attach mAttach;
    private Drawable mdDrawable;
    
    public FileSpan(Context context, Attach attach, int width) {
        this.mAttach = attach;
        this.mContext = context;

        Log.d(TAG, "--FileSpan---width---" + width);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view  = inflater.inflate(R.layout.layout_voice, null);
        
        RelativeLayout relativeLayout = (RelativeLayout) view.findViewById(R.id.span_container);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        int space = context.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int viewWidth = width - space;
        params.width = viewWidth;
        relativeLayout.setLayoutParams(params);
        
        TextView titleView = (TextView) view.findViewById(R.id.tv_title);
        TextView summaryView = (TextView) view.findViewById(R.id.tv_summary);
        ImageView ivIcon = (ImageView) view.findViewById(R.id.iv_icon);
        ImageView ivPlay = (ImageView) view.findViewById(R.id.iv_play);

        File file = new File(attach.getLocalPath());
        
        int fileType = SystemUtil.guessFileType(file);
        int optIcon = 0;
        switch (fileType) {
            case Attach.VOICE:  //语音
                optIcon = R.drawable.ic_play_circle;
                break;
            case Attach.FILE:
            case Attach.ARCHIVE:
            case Attach.IMAGE:
            case Attach.VIDEO:
                optIcon = R.drawable.ic_file_open;
                break;
            default:
                optIcon = R.drawable.ic_file_open;
                break;
        }

        ivIcon.setImageResource(R.drawable.ic_library_music);
        ivPlay.setImageResource(optIcon);
        
        titleView.setMaxWidth((int) (viewWidth * 0.6));
        
        titleView.setText(attach.getFilename());
        String summary = SystemUtil.formatFileSize(attach.getSize());
        if (Attach.VOICE == mAttach.getType() && mAttach.getDescription() != null) {    //录音
            summary += "  " + TimeUtil.formatMillis(Long.parseLong(attach.getDescription()));
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
