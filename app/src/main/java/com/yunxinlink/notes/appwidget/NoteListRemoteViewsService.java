package com.yunxinlink.notes.appwidget;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.socks.library.KLog;

/**
 * @author huanghui1
 * @update 2016/9/15 18:06
 * @version: 0.0.1
 */
public class NoteListRemoteViewsService extends RemoteViewsService {
    private static final String TAG = "NoteListRemoteViewsService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        KLog.d(TAG, "onGetViewFactory");
        return new NoteListRemoteViewsFactory(getApplicationContext(), intent);
    }
}
