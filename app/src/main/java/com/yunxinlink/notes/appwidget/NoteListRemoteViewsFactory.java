package com.yunxinlink.notes.appwidget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 桌面小部件列表的数据刷新的工厂
 * @author huanghui1
 * @update 2016/9/15 17:42
 * @version: 0.0.1
 */
public class NoteListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    
    private Context mContext;
    
    //笔记数据
    private List<DetailNoteInfo> mNoteInfoList = new ArrayList<>();

    public NoteListRemoteViewsFactory(Context context, Intent intent) {
        this.mContext = context;
    }
    
    @Override
    public void onCreate() {
        
    }

    //可进行耗时操作
    @Override
    public void onDataSetChanged() {
        NoteApplication app = (NoteApplication) mContext.getApplicationContext();
        List<DetailNoteInfo> list = NoteManager.getInstance().getAllDetailNotes(app.getCurrentUser(), null);
        mNoteInfoList.clear();
        
        if (!SystemUtil.isEmpty(list)) {
            mNoteInfoList.addAll(list);
        }
    }

    @Override
    public void onDestroy() {
        mNoteInfoList.clear();
    }

    @Override
    public int getCount() {
        return mNoteInfoList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || getCount() < 0) {
            return null;
        }
        DetailNoteInfo detailNote = mNoteInfoList.get(position);
        NoteInfo note = detailNote.getNoteInfo();

        if (note == null) {
            return null;
        }

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.item_search_list);
//        remoteViews.setTextViewText();

        /*holder.itemView.setTag(holder.getAdapterPosition());

        //重置附件图标的各种状态
        resetAttachView(holder);

        holder.mTvContent.setText(note.getStyleContent(true, detailNote.getDetailList(), mKeyword, mHighlightColor));
        holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getShowTime(true)));

        if (note.hasAttach() && detailNote.getLastAttach() != null) {
            SystemUtil.setViewVisibility(holder.mIvIcon, View.VISIBLE);
            showAttachIcon(detailNote, holder);
        }*/
        return null;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
