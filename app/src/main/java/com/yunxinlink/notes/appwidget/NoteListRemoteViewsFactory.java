package com.yunxinlink.notes.appwidget;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 桌面小部件列表的数据刷新的工厂
 * @author huanghui1
 * @update 2016/9/15 17:42
 * @version: 0.0.1
 */
public class NoteListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = "NoteListRemoteViewsFactory";
    
    private Context mContext;
    
    //笔记数据
    private List<DetailNoteInfo> mNoteInfoList = new ArrayList<>();
//    private List<String> mNoteInfoList = new ArrayList<>();

    //图片加载失败的图片
    private ItemAttachIcon mItemAttachIcon;

    //着色的颜色
    private int mTintColor;

    public NoteListRemoteViewsFactory(Context context, Intent intent) {
        this.mContext = context;
        mTintColor = initTintColor(context);
        mItemAttachIcon = new ItemAttachIcon();
    }

    /**
     * 初始化着色的颜色
     * @return
     */
    private int initTintColor(Context context) {
        Resources resources = context.getResources();
        return ResourcesCompat.getColor(resources, R.color.text_time_color, context.getTheme());
    }
    
    @Override
    public void onCreate() {
        KLog.d(TAG, "onCreate");
    }

    //可进行耗时操作
    @Override
    public void onDataSetChanged() {
        KLog.d(TAG, "onDataSetChanged");
        NoteApplication app = (NoteApplication) mContext.getApplicationContext();
        List<DetailNoteInfo> list = NoteManager.getInstance().getAllDetailNotes(app.getCurrentUser(), null);
        mNoteInfoList.clear();

        if (!SystemUtil.isEmpty(list)) {
            mNoteInfoList.addAll(list);
        }
    }

    @Override
    public void onDestroy() {
        KLog.d(TAG, "onDestroy");
        mNoteInfoList.clear();
    }

    @Override
    public int getCount() {
        KLog.d(TAG, "getCount");
        return mNoteInfoList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        KLog.d(TAG, "getViewAt  position:" + position);
        if (position < 0 || position >= mNoteInfoList.size() ) {
            return null;
        }
        DetailNoteInfo detailNote = mNoteInfoList.get(position);
        NoteInfo note = detailNote.getNoteInfo();

        if (note == null) {
            return null;
        }

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.item_widget_list);
        remoteViews.setTextViewText(R.id.tv_content, note.getStyleContent(true, detailNote.getDetailList()));
        remoteViews.setTextViewText(R.id.tv_time, TimeUtil.formatNoteTime(note.getShowTime(true)));
//        remoteViews.setTextViewText(R.id.tv_content, mNoteInfoList.get(position));
//        remoteViews.setTextViewText(R.id.tv_time, "时间爱");

//        Intent intent = new Intent(mContext, MainActivity.class);
//        intent.putExtra("Item",position+1);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        remoteViews.setOnClickFillInIntent(R.id.slide_layout, intent);

        if (note.hasAttach() && detailNote.getLastAttach() != null) {
            remoteViews.setViewVisibility(R.id.iv_icon, View.VISIBLE);
            Bitmap bitmap = getAttachIcon(mContext, detailNote);
            if (bitmap == null) {
                bitmap = getFailedImage(mContext, mTintColor);
            }
            remoteViews.setImageViewBitmap(R.id.iv_icon, bitmap);
        } else {
            remoteViews.setViewVisibility(R.id.iv_icon, View.GONE);
        }
        return remoteViews;
    }

    /**
     * 显示附件的图标
     * @param detailNote
     */
    private Bitmap getAttachIcon(Context context, DetailNoteInfo detailNote) {
        Attach lastAttach = detailNote.getLastAttach();
        if (lastAttach.isImage()) { //图片文件
            return ImageUtil.loadImageThumbnailsSync(lastAttach.getLocalPath(), new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_WIDTH));
        } else {
            return getAttachIcon(context, lastAttach.getType(), mTintColor);
        }
    }

    /**
     * 初始化图片加载时候的图片
     * @return
     */
    private Bitmap getAttachIcon(Context context, int attachType, int color) {
        Drawable drawable = null;
        int resId = 0;
        Resources resources = context.getResources();
        switch (attachType) {
            case Attach.VOICE:
                if (mItemAttachIcon.mMusicDrawable == null) {
                    resId = R.drawable.ic_library_music;
                    drawable = ResourcesCompat.getDrawable(resources, resId, context.getTheme());
                    mItemAttachIcon.mMusicDrawable = SystemUtil.getTintDrawable(context, drawable, color);
                }
                drawable = mItemAttachIcon.mMusicDrawable;
                break;
            case Attach.VIDEO:
                if (mItemAttachIcon.mVideoDrawable == null) {
                    resId = R.drawable.ic_library_music;
                    drawable = ResourcesCompat.getDrawable(resources, resId, context.getTheme());
                    mItemAttachIcon.mVideoDrawable = SystemUtil.getTintDrawable(context, drawable, color);
                }
                drawable = mItemAttachIcon.mVideoDrawable;
                break;
            case Attach.ARCHIVE:
                if (mItemAttachIcon.mArchiveDrawable == null) {
                    resId = R.drawable.ic_library_archive;
                    drawable = ResourcesCompat.getDrawable(resources, resId, context.getTheme());
                    mItemAttachIcon.mArchiveDrawable = SystemUtil.getTintDrawable(context, drawable, color);
                }
                drawable = mItemAttachIcon.mArchiveDrawable;
                break;
            case Attach.FILE:
                if (mItemAttachIcon.mFileDrawable == null) {
                    resId = R.drawable.ic_library_file;
                    drawable = ResourcesCompat.getDrawable(resources, resId, context.getTheme());
                    mItemAttachIcon.mFileDrawable = SystemUtil.getTintDrawable(context, drawable, color);
                }
                drawable = mItemAttachIcon.mFileDrawable;
                break;
        }
        if (drawable != null) {
            return ImageUtil.drawable2Bitmap(drawable);
        }
        return null;
    }

    /**
     * 初始化图片加载时候的图片
     * @return
     */
    private Bitmap getFailedImage(Context context, int color) {
        if (mItemAttachIcon.mFailedDrawable == null) {

            Resources resources = context.getResources();
            Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_broken_image, context.getTheme());
            drawable = SystemUtil.getTintDrawable(context, drawable, color);

            mItemAttachIcon.mFailedDrawable = drawable;
        }
        return ImageUtil.drawable2Bitmap(mItemAttachIcon.mFailedDrawable);
    }

    @Override
    public RemoteViews getLoadingView() {
        KLog.d(TAG, "getLoadingView");
        return null;
    }

    @Override
    public int getViewTypeCount() {
        KLog.d(TAG, "getViewTypeCount");
        return 1;
    }

    @Override
    public long getItemId(int position) {
        KLog.d(TAG, "getItemId position:" + position);
        return position;
    }

    @Override
    public boolean hasStableIds() {
        KLog.d(TAG, "hasStableId");
        return true;
    }

    /**
     * 附件的类型的图标
     */
    class ItemAttachIcon {
        //图片加载失败的图片
        Drawable mFailedDrawable;
        Drawable mMusicDrawable;
        //压缩文件
        Drawable mArchiveDrawable;
        //视频文件
        Drawable mVideoDrawable;
        Drawable mFileDrawable;

    }
}
