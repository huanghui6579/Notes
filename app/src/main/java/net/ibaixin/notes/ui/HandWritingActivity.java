package net.ibaixin.notes.ui;

import android.graphics.Color;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.ibaixin.notes.R;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;

//http://www.jianshu.com/p/7d3369b68785
public class HandWritingActivity extends BaseActivity {
    @Override
    protected int getContentView() {
        return R.layout.activity_hand_writing;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

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
}
