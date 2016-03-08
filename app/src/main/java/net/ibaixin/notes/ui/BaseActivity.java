package net.ibaixin.notes.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.R;
import net.ibaixin.notes.model.User;
import net.ibaixin.notes.util.SystemUtil;

import java.lang.reflect.Field;

/**
 * @author huanghui1
 * @update 2016/2/24 17:30
 * @version: 0.0.1
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected static String TAG = null;
    
    protected Context mContext;
    
    protected Toolbar mToolBar;
    
    /**
     * 是否显示返回箭头
     */
    private boolean mShowHomeUp;
    
    public BaseActivity() {
        TAG = this.getClass().getSimpleName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        
        setContentView(getContentView());
        
        mShowHomeUp = showHomeUp();

        initToolBar();

        initView();

        initData();
    }
    
    /**
     * 判断当前用户是否登录
     * @author huanghui1
     * @update 2016/3/8 17:27
     * @version: 1.0.0
     */
    protected boolean isLogin() {
        return ((NoteApplication) getApplication()).getCurrentUser() != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                beforeBack();
                onBack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 执行界面的返回操作
     * @author huanghui1
     * @update 2016/3/2 10:53
     * @version: 1.0.0
     */
    protected void onBack() {
        finish();
    }
    
    /**
     * 界面返回前的操作
     * @author tiger
     * @update 2016/3/5 15:09
     * @version 1.0.0
     */
    protected void beforeBack() {
        
    }
    
    /**
     * 创建popuMenu
     * @param showIcon 是否强制否显示图标
     * @author huanghui1
     * @update 2016/3/2 11:37
     * @version: 1.0.0
     */
    protected PopupMenu createPopuMenu(View auchor, int menuResId, boolean showIcon, PopupMenu.OnMenuItemClickListener itemClickListener) {
        if (auchor != null) {
            PopupMenu popupMenu = new PopupMenu(this, auchor);
            popupMenu.inflate(menuResId);
            popupMenu.setOnMenuItemClickListener(itemClickListener);
            if (showIcon) {
                showPopuMenuIcon(popupMenu);
            }
            return popupMenu;
        } else {
            return null;
        }
    }

    /**
     * 获取toolbar上各菜单项的view
     * @author huanghui1
     * @update 2016/3/2 14:22
     * @version: 1.0.0
     */
    protected View getToolBarMenuView(int menuItemId) {
        if (mToolBar != null) {
            View child = mToolBar.findViewById(menuItemId);
            View parent = null;
            if (child != null) {
                ViewParent viewParent = child.getParent();
                if (viewParent instanceof ViewGroup) {
                    parent = (ViewGroup) viewParent;
                } else {
                    parent = child;
                }
            }
            return parent;
        } else {
            return null;
        }
    }
    
    /**
     * 强制显示popupMenu的图标
     * @author huanghui1
     * @update 2016/3/2 11:31
     * @version: 1.0.0
     */
    protected void showPopuMenuIcon(PopupMenu popupMenu) {
        try {
            Field field = popupMenu.getClass().getDeclaredField("mPopup");
            if (field != null) {
                field.setAccessible(true);
                Object obj = field.get(popupMenu);
                if (obj instanceof MenuPopupHelper) {
                    MenuPopupHelper popupHelper = (MenuPopupHelper) obj;
                    popupHelper.setForceShowIcon(true);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置菜单图标的颜色
     * @author tiger
     * @update 2016/2/27 16:00
     * @version 1.0.0
     */
    protected void setMenuOverFlowTint(MenuItem menuItem) {
        if (menuItem != null) {
            TypedArray a = obtainStyledAttributes(android.support.v7.appcompat.R.style.Widget_AppCompat_ActionButton_Overflow, new int[]{android.R.attr.src});
            Drawable drawable = a.getDrawable(0);
//            a = obtainStyledAttributes(R.style.AppTheme_PopupOverlay, new int[] {R.attr.colorButtonNormal});
            int tint = SystemUtil.getColor(this, R.color.colorButtonControl);
            if (drawable != null) {
                DrawableCompat.setTint(drawable, tint);
                menuItem.setIcon(drawable);
            }
            a.recycle();
        }
    }

    /**
     * 为菜单的图标着色
     * @author huanghui1
     * @update 2016/3/2 16:05
     * @version: 1.0.0
     */
    protected void setMenuTint(MenuItem item, int color) {
        if (item != null) {
            Drawable icon = item.getIcon();
            if (icon != null) {
                int tint = color;
                if (tint == 0) {
                    tint = getPrimaryColor();
                }
                DrawableCompat.setTint(icon, tint);
                item.setIcon(icon);
            }
        }
    }
    
    /**
     * 获取应用的主题色
     * @author huanghui1
     * @update 2016/3/2 16:07
     * @version: 1.0.0
     */
    protected int getPrimaryColor() {
        TypedArray a = obtainStyledAttributes(R.style.AppTheme, new int[] {R.attr.colorPrimary});
        int defautColor = SystemUtil.getColor(this, R.color.colorPrimary);
        int color = a.getColor(0, defautColor);
        a.recycle();
        return color;
    }
    
    protected abstract int getContentView();
    
    /**
     * 是否显示向上的返回箭头，默认显示
     * @author huanghui1
     * @update 2016/3/2 10:46
     * @version: 1.0.0
     */
    protected boolean showHomeUp() {
        return true;
    }
    
    /**
     * 初始化toolbar
     * @author huanghui1
     * @update 2016/2/24 17:39
     * @version: 1.0.0
     */
    protected void initToolBar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolBar != null) {
            setSupportActionBar(mToolBar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null && mShowHomeUp) {
                actionBar.setDisplayHomeAsUpEnabled(mShowHomeUp);
            }
        }
    }
    
    /**
     * 获取当前登录的用户
     * @author huanghui1
     * @update 2016/3/8 18:14
     * @version: 1.0.0
     */
    protected User getCurrentUser() {
        NoteApplication app = (NoteApplication) getApplication();
        return app.getCurrentUser();
    }
    
    /**
     * 初始化数据
     * @author huanghui1
     * @update 2016/2/24 17:35
     * @version: 1.0.0
     */
    protected abstract void initData();
    
    /**
     * 初始化控件
     * @author huanghui1
     * @update 2016/2/24 17:35
     * @version: 1.0.0
     */
    protected abstract void initView();
}
