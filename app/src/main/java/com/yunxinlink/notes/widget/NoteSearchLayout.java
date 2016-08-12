package com.yunxinlink.notes.widget;

import android.content.Context;
import android.support.v7.widget.SearchView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.yunxinlink.notes.R;

/**
 * 文本搜索的控件，对应的布局文件“layout_menu_search.xml”
 * @author huanghui1
 * @update 2016/8/11 19:57
 * @version: 0.0.1
 */
public class NoteSearchLayout extends RelativeLayout implements View.OnClickListener {
    
    private ImageView mIvPrevious;
    private ImageView mIvNext;
    private SearchView mSearchView;
    
    private OnSearchActionListener mOnSearchActionListener;

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

        mIvPrevious.setOnClickListener(this);
        mIvNext.setOnClickListener(this);

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
        if (expand) {
            mSearchView.onActionViewExpanded();
        }
    }

    /**
     * 退出搜索模式
     */
    public void outSearch() {
        mSearchView.clearFocus();
    }

    /**
     * 切换到搜索模式
     */
    public void inSearch() {
        mSearchView.requestFocus();
    }

    /**
     * 清除文本
     */
    public void clearText() {
        mSearchView.setQuery("", false);
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param listener the listener object that receives callbacks when the user performs
     * actions in the SearchView such as clicking on buttons or typing a query.
     */
    public void setOnQueryTextListener(SearchView.OnQueryTextListener listener) {
        mSearchView.setOnQueryTextListener(listener);
    }

    /**
     * 设置搜索处理的监听
     * @param actionListener
     */
    public void setOnSearchActionListener(OnSearchActionListener actionListener) {
        this.mOnSearchActionListener = actionListener;
    }

    @Override
    public void onClick(View v) {
        if (mOnSearchActionListener != null) {
            switch (v.getId()) {
                case R.id.iv_previous:
                    mOnSearchActionListener.onPrevious();
                    break;
                case R.id.iv_next:
                    mOnSearchActionListener.onNext();
                    break;
            }
        }
    }
    
    /**
     * 处理搜索的相关事件
     * @author huanghui1
     * @update 2016/8/12 14:48
     * @version: 1.0.0
     */
    public interface OnSearchActionListener {
        /**
         * onPrevious
         */
        void onPrevious();

        /**
         * 查看后面的结果
         */
        void onNext();
    }
}
