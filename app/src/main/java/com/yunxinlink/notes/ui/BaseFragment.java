package com.yunxinlink.notes.ui;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.PopupMenu;
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
        NoteApplication app = (NoteApplication) getActivity().getApplication();
        return app.getCurrentUser();
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
