package com.yunxinlink.notes.ui;

import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.List;

/**
 * 垃圾桶中的笔记列表
 * @author huanghui1
 * @update 2016/8/23 11:51
 * @version: 1.0.0
 */
public class TrashActivity extends BaseActivity implements MainFragment.OnMainFragmentInteractionListener {
    
    private Handler mHandler = new Handler();
    
    //笔记的观察者
    private NoteContentObserver mNoteObserver;

    @Override
    protected int getContentView() {
        return R.layout.activity_trash;
    }

    @Override
    protected void initData() {
        //注册笔记的观察者
        registerContentObserver();
    }

    @Override
    protected void initView() {
        attachFragment();
    }

    /**
     * 加载fragment
     */
    private void attachFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = MainFragment.newInstance(null, true);
        transaction.replace(R.id.content_container, fragment, fragment.getClass().getSimpleName()).commit();
    }

    @Override
    public void setActionMode(boolean isOn) {
        
    }

    @Override
    public View getContentMainView() {
        return null;
    }

    @Override
    protected void onDestroy() {
        //注销笔记的观察者
        unregisterContentObserver();
        super.onDestroy();
    }

    /**
     * 注册观察者的监听
     * @author huanghui1
     * @update 2016/3/9 18:10
     * @version: 1.0.0
     */
    private void registerContentObserver() {
        mNoteObserver = new NoteContentObserver(mHandler);
        NoteManager.getInstance().addObserver(mNoteObserver);
        FolderManager.getInstance().addObserver(mNoteObserver);
    }

    /**
     * 注销观察者
     * @author huanghui1
     * @update 2016/3/9 18:11
     * @version: 1.0.0
     */
    private void unregisterContentObserver() {
        if (mNoteObserver != null) {
            NoteManager.getInstance().removeObserver(mNoteObserver);
            FolderManager.getInstance().removeObserver(mNoteObserver);
        }
    }

    /**
     * 获取该activity的fragment
     * @return
     */
    public MainFragment getMainFragment() {
        return (MainFragment) getSupportFragmentManager().findFragmentByTag(MainFragment.class.getSimpleName());
    }

    /**
     * 笔记的观察者
     */
    class NoteContentObserver extends ContentObserver {

        public NoteContentObserver(Handler handler) {
            super(handler);
        }

        //对于笔记的添加和删除，在回收站中是反的，也就是，当删除笔时，回收站就需要添加，当添加笔记时，回收站就需要删除
        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.NoteColumns.NOTIFY_FLAG:  //笔记的通知
                    DetailNoteInfo detailNote = null;
                    List<DetailNoteInfo> list = null;
                    MainFragment mainFragment = getMainFragment();
                    if (data != null) {
                        if (data instanceof NoteInfo) {
                            NoteInfo noteInfo = (NoteInfo) data;
                            detailNote = new DetailNoteInfo();
                            detailNote.setNoteInfo(noteInfo);
                        } else if (data instanceof DetailNoteInfo) {
                            detailNote = ((DetailNoteInfo) data);
                        } else if (data instanceof List) {
                            list = (List<DetailNoteInfo>) data;
                        }
                    }
                    switch (notifyType) {
                        case ADD:   //笔记的添加，回收站则是移除界面上的该笔记
                        case REMOVE:   //笔记彻底删除
                            if (mainFragment != null) {
                                if (detailNote != null) {   //只有一个笔记
                                    mainFragment.deleteNote(detailNote, false);
                                } else if (list != null) {  //多条笔记
                                    mainFragment.deleteNotes(list, false);
                                } else {    //清除的是所有，则直接清空并刷新界面
                                    mainFragment.clearNotes();
                                }
                            }
                            break;
                        case DELETE:    //笔记删除，则回收站界面添加该笔记
                            if (mainFragment != null) {
                                if (detailNote != null) {   //只有一个笔记
                                    mainFragment.addNote(detailNote);
                                } else if (list != null) {  //多条笔记
                                    mainFragment.addNotes(list);
                                }
                            }
                            break;
                        case UPDATE:    ///更新笔记
                            KLog.d(TAG, "update note in trash ui:" + detailNote);
                            if (detailNote != null) {
                                mainFragment.updateNote(detailNote);
                            }
                            break;
                        case BATCH_UPDATE:  //同时更新了多条记录
                            if (!SystemUtil.isEmpty(list)) {
                                KLog.d(TAG, "------update note list in trash size:" + list.size());
                                mainFragment.updateNotes(list);
                            }
                            break;
                        case MERGE: //合并笔记
                            if (!SystemUtil.isEmpty(list)) {
                                KLog.d(TAG, "------merge note list in trash size:" + list.size());
                                mainFragment.mergeNotes(list);
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
