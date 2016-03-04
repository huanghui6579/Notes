package net.ibaixin.notes.ui;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;

import net.ibaixin.notes.R;

import java.lang.ref.WeakReference;

/**
 * 笔记编辑界面
 * @author huanghui1
 * @update 2016/3/2 10:13
 * @version: 1.0.0
 */
public class NoteEditActivity extends BaseActivity {
    
    private static final int MSG_INIT_BOOTOM_TOOL_BAR = 1;

    private PopupMenu mAttachPopu;
    private PopupMenu mCameraPopu;
    private PopupMenu mOverflowPopu;
    
    private EditText mEtContent;
    
    private TextView mToolbarTitleView;
    
    private Handler mHandler = new MyHandler(this);
    
    private static class MyHandler extends Handler {
        private final WeakReference<NoteEditActivity> mTarget;

        public MyHandler(NoteEditActivity target) {
            mTarget = new WeakReference<>(target);
        }
        
        @Override
        public void handleMessage(Message msg) {
            NoteEditActivity activity = mTarget.get();
            switch (msg.what) {
                case MSG_INIT_BOOTOM_TOOL_BAR:
                    ViewStub viewStub = (ViewStub) activity.findViewById(R.id.bottom_tool_bar);
                    viewStub.inflate();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private void setCustomTitle(CharSequence title, int iconResId) {
        if (!TextUtils.isEmpty(title)) {
            if (mToolbarTitleView == null) {
                LayoutInflater inflater = LayoutInflater.from(this);
                mToolbarTitleView = (TextView) inflater.inflate(R.layout.edit_custom_title, null);
            }
            if (mToolBar != mToolbarTitleView.getParent()) {
                mToolBar.addView(mToolbarTitleView);
            }
            mToolbarTitleView.setText(title);

        } else {
            if (mToolbarTitleView != null) {
                mToolBar.removeView(mToolbarTitleView);
            }
        }
    }
    
    @Override
    protected void initToolBar() {
        super.initToolBar();
        CharSequence title = getTitle();
        setTitle(null);
        setCustomTitle(title, 0);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_note_edit;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {
        mEtContent = (EditText) findViewById(R.id.et_content);
        mHandler.sendEmptyMessage(MSG_INIT_BOOTOM_TOOL_BAR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_edit, menu);
        MenuItem item = menu.findItem(R.id.action_more);
        setMenuOverFlowTint(item);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View attachView = null;
        switch (item.getItemId()) {
            case R.id.action_attach:    //添加附件
                attachView = getToolBarMenuView(R.id.action_attach);
                createPopuMenu(attachView, mAttachPopu, R.menu.edit_attach, true);
                break;
            case R.id.action_more:  //更多
                attachView = getToolBarMenuView(R.id.action_more);
                final PopupMenu attachPopu = createPopuMenu(attachView, mAttachPopu, R.menu.edit_overflow, false);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Menu menu = attachPopu.getMenu();

                        MenuItem shareItem = menu.findItem(R.id.action_share);
                        setMenuTint(shareItem, 0);

                        MenuItem searchItem = menu.findItem(R.id.action_search);
                        setMenuTint(searchItem, 0);

                        MenuItem deleteItem = menu.findItem(R.id.action_delete);
                        setMenuTint(deleteItem, 0);

                        if (attachPopu != null) {
                            attachPopu.show();
                        }
                    }
                });

                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 创建附件的弹出菜单
     * @param showImmediate 是否立刻显示，调用show方法
     * @author huanghui1
     * @update 2016/3/2 14:15
     * @version: 1.0.0
     */
    private PopupMenu createPopuMenu(View aucher, PopupMenu popupMenu, int menuResId, boolean showImmediate) {
        if (aucher != null) {
            if (popupMenu == null) {
                popupMenu = createPopuMenu(aucher, menuResId, true, new OnPopuMenuItemClickListener());
            }
            if (showImmediate) {
                popupMenu.show();
            }
            return popupMenu;
        } else {
            return null;
        }
    }
    
    class OnPopuMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return false;
        }
    }
    
}
