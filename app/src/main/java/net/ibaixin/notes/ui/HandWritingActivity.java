package net.ibaixin.notes.ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.support.v4.widget.PopupWindowCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;

import net.ibaixin.notes.R;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.widget.NotePopupWindow;

//http://www.jianshu.com/p/7d3369b68785
public class HandWritingActivity extends BaseActivity {


    private int mPopupMaxWidth;
    
    //画笔的弹窗
    private PopupWindow mStrokePopupWindow;
    
    @Override
    protected int getContentView() {
        return R.layout.activity_hand_writing;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {
        Resources res = getResources();
        mPopupMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2,
                res.getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_config_prefDialogWidth));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.paint_edit, menu);
        
        MenuItem penItem = menu.findItem(R.id.action_pen);

        setMenuTint(penItem, Color.WHITE);

        int disableColor = SystemUtil.adjustAlpha(Color.WHITE, Constants.MENU_ITEM_COLOR_ALPHA);
        
        MenuItem undoItem = menu.findItem(R.id.action_undo);
        setMenuTint(undoItem, disableColor);

        MenuItem redoItem = menu.findItem(R.id.action_redo);
        setMenuTint(redoItem, disableColor);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        View view = getToolBarMenuView(R.id.action_pen);
        
        if (view != null) {
            showStrokeWindow(view);
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示画笔的弹窗
     * @param author
     */
    private void showStrokeWindow(View author) {

        if (mStrokePopupWindow == null) {
            mStrokePopupWindow = createPopuWindow(R.layout.layout_brush_panel);
        }

        PopupWindowCompat.showAsDropDown(mStrokePopupWindow, author, 0, 0, Gravity.TOP | Gravity.START);
    }

    /**
     * 创建弹窗
     * @param layoutId
     * @return
     */
    private PopupWindow createPopuWindow(int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootPanel = inflater.inflate(layoutId, null);

        int popuWidth = SystemUtil.measureContentWidth(rootPanel, mPopupMaxWidth);

        NotePopupWindow popupWindow = new NotePopupWindow(mContext);
        popupWindow.setContentView(rootPanel);
        popupWindow.setWidth(popuWidth);
        popupWindow.setFocusable(true);
        
        return popupWindow;
    }
}
