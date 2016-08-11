package com.yunxinlink.notes.widget;

import android.content.Context;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.yunxinlink.notes.R;

/**
 * 文本搜索的控件，对应的布局文件“layout_menu_search.xml”
 * @author huanghui1
 * @update 2016/8/11 19:57
 * @version: 0.0.1
 */
public class NoteSearchLayout extends RelativeLayout {
    
    private ImageView mIvPrevious;
    private ImageView mIvNext;
    private SearchView mSearchView;
    
    public NoteSearchLayout(Context context) {
        this(context, null);
    }

    public NoteSearchLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NoteSearchLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final int layoutResId = R.layout.layout_menu_search;
        inflater.inflate(layoutResId, this, true);

        mIvNext = (ImageView) findViewById(R.id.iv_next);
        mIvPrevious = (ImageView) findViewById(R.id.iv_previous);
        mSearchView = (SearchView) findViewById(R.id.search_view);

        //初始化
        init(true, getContext().getString(R.string.find_hint));
    }

    /**
     * 进行配置
     * @param expand 是否默认展开
     * @param queryHint 搜索的hint文字
     */
    public void init(boolean expand, String queryHint) {
        mSearchView.setQueryHint(queryHint);
        mSearchView.onActionViewExpanded();
    }
}
