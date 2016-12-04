package com.yunxinlink.notes.ui;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * @author huanghui1
 * @update 2016/8/22 10:29
 * @version: 0.0.1
 */
public class BaseFragment extends Fragment {
    protected static String TAG = null;
    
    public BaseFragment() {
        TAG = this.getClass().getSimpleName();
    }

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //在配置变化的时候将这个fragment保存下来
        setRetainInstance(true);
    }

    /**
     * 获取当前登录的用户
     * @author huanghui1
     * @update 2016/3/8 18:14
     * @version: 1.0.0
     */
    protected User getCurrentUser() {
        return getCurrentUser(false);
    }

    /**
     * 获取当前的用户
     * @param fromLocal 如果不存在，则从本地加载
     * @return
     */
    protected User getCurrentUser(boolean fromLocal) {
        NoteApplication app = (NoteApplication) getActivity().getApplication();
        User user = app.getCurrentUser();
        if (user == null && fromLocal) {
            user = app.initLocalUser(getContext());
        }
        return user;
    }

    /**
     * 是否有本地账号
     * @return
     */
    protected boolean hasUser() {
        User user = getCurrentUser();
        return user != null;
    }

    /**
     * 创建popuMenu
     * @param showIcon 是否强制否显示图标
     * @author huanghui1
     * @update 2016/3/2 11:37
     * @version: 1.0.0
     */
    protected PopupMenu createPopMenu(View author, int menuResId, boolean showIcon, PopupMenu.OnMenuItemClickListener itemClickListener) {
        if (author != null) {
            PopupMenu popupMenu = new PopupMenu(getContext(), author);
            popupMenu.inflate(menuResId);
            popupMenu.setOnMenuItemClickListener(itemClickListener);
            if (showIcon) {
                SystemUtil.showPopMenuIcon(popupMenu);
            }
            return popupMenu;
        } else {
            return null;
        }
    }

    /**
     * 在后台执行任务
     * @author huanghui1
     * @update 2016/6/27 21:02
     * @version: 1.0.0
     */
    protected void doInbackground(Runnable runnable) {
        SystemUtil.getThreadPool().execute(runnable);
    }

    /**
     * 为菜单的图标着色
     * @author huanghui1
     * @update 2016/3/2 16:05
     * @version: 1.0.0
     */
    protected void setMenuTint(MenuItem item, int color) {
        if (item != null) {
            Drawable icon = item.getIcon().mutate();
            getTintDrawable(icon, color);
            item.setIcon(icon);
        }
    }

    /**
     * 返回着色后的图标
     * @param srcIcon 原始图标
     * @param color
     * @return
     */
    protected Drawable getTintDrawable(Drawable srcIcon, int color) {
        if (srcIcon != null) {
            int tint = color;
            if (tint == 0) {
                tint = getPrimaryColor();
            }
            srcIcon = DrawableCompat.wrap(srcIcon);
            DrawableCompat.setTint(srcIcon, tint);
        }
        return srcIcon;
    }


    /**
     * 给menu的各项着色，默认的颜色是colorPrimary
     * @param menu
     */
    protected void tintMenu(Menu menu) {
        int size = menu.size();
        for (int i = 0; i < size; i++) {
            MenuItem menuItem = menu.getItem(i);
            setMenuTint(menuItem, 0);
        }
    }

    /**
     * 获取应用的主题色
     * @author huanghui1
     * @update 2016/3/2 16:07
     * @version: 1.0.0
     */
    protected int getPrimaryColor() {
        TypedArray a = getContext().obtainStyledAttributes(R.style.AppTheme, new int[] {R.attr.colorPrimary});
        int defaultColor = SystemUtil.getColor(getContext(), R.color.colorPrimary);
        int color = a.getColor(0, defaultColor);
        a.recycle();
        return color;
    }
}
