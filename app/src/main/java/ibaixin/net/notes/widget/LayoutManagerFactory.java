package ibaixin.net.notes.widget;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/2/28 13:48
 */
public class LayoutManagerFactory {
    private LinearLayoutManager mLinearLayoutManager;

    private GridLayoutManager mGridLayoutManager;

    /**
     * 默认表格每列显示的项数，默认是2
     */
    private int mSpanCount = 2;

    /**
     * 根据样式获取对应的列表管理器
     * @param isGrid 是否是列表样式
     * @author tiger
     * @update 2016/2/28 13:57
     * @version 1.0.0
     */
    public RecyclerView.LayoutManager getLayoutManager(Context context, boolean isGrid) {
        RecyclerView.LayoutManager layoutManager = null;
        if (isGrid) {
            if (mGridLayoutManager == null) {
                mGridLayoutManager = new GridLayoutManager(context, mSpanCount);
            }
            layoutManager = mGridLayoutManager;
        } else {
            if (mLinearLayoutManager == null) {
                mLinearLayoutManager = new LinearLayoutManager(context);
            }
            layoutManager = mLinearLayoutManager;
        }
        return layoutManager;
    }

    /**
     * 设置网格中每列显示的数量
     * @param spanCount 每列显示的数量
     * @author tiger
     * @update 2016/2/28 14:00
     * @version 1.0.0
     */
    public void setGridSpanCount(int spanCount) {
        this.mSpanCount = spanCount;
    }

}
