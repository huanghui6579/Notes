package com.yunxinlink.notes.appwidget;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.SystemUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 桌面小部件的缓存
 * @author huanghui1
 * @update 2016/9/15 14:58
 * @version: 0.0.1
 */
public class WidgetItemCache {
    
    private static WidgetItemCache mInstance = null;

    /**
     * 小部件的集合
     */
    private List<WidgetItem> mWidgetItems = new ArrayList<>();
    
    private WidgetItemCache() {}
    
    public static WidgetItemCache getInstance() {
        if (mInstance == null) {
            synchronized (WidgetItemCache.class) {
                if (mInstance == null) {
                    mInstance = new WidgetItemCache();
                }
            }
        }
        return mInstance;
    }

    /**
     * 重新设置数据
     * @param list
     */
    public void setWidgetItems(List<WidgetItem> list) {
        mWidgetItems.clear();
        if (!SystemUtil.isEmpty(list)) {
            mWidgetItems.addAll(list);
        }
    }

    /**
     * 获取集合数据
     * @return
     */
    public List<WidgetItem> getWidgetItems() {
        return mWidgetItems;
    }

    /**
     * 清空
     */
    public void clear() {
        mWidgetItems.clear();
    }

    /**
     * 加载桌面小部件的快速创建工具栏
     */
    public List<WidgetItem> loadWidgetItems() {
        List<WidgetItem> list =  WidgetManager.getInstance().getAllWidgetItems();
        if (SystemUtil.isEmpty(list)) {
            KLog.d("load widget items list is null");
        } else {
            KLog.d("load widget items list is not null");
        }
        return list;
    }
}
