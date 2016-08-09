package net.ibaixin.notes.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.socks.library.KLog;

import net.ibaixin.notes.R;
import net.ibaixin.notes.helper.ItemTouchHelperAdapter;
import net.ibaixin.notes.helper.ItemTouchHelperViewHolder;
import net.ibaixin.notes.helper.OnStartDragListener;
import net.ibaixin.notes.helper.SimpleItemTouchHelperCallback;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.model.DetailList;
import net.ibaixin.notes.model.DetailNoteInfo;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.widget.LayoutManagerFactory;
import net.ibaixin.notes.widget.NoteEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DetailListFragment.OnDetailInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DetailListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailListFragment extends Fragment implements TextView.OnEditorActionListener, OnStartDragListener, TextWatcher, ActionFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    
    private static final String TAG = "DetailListFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnDetailInteractionListener mListener;

    private List<DetailList> mDetailLists;
    //已经完成的清单
    private List<DetailList> mDoneDetails;

    //笔记的标题
    private CharSequence mTitle;

    //笔记的标题
    private NoteEditText mEtTitle;
    
    private RecyclerView mRecyclerView;

    //拖拽的帮助器
    private ItemTouchHelper mItemTouchHelper;
    
    //笔记
    private NoteInfo mNote;
    
    private Handler mHandler = new Handler();

    public DetailListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DetailListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DetailListFragment newInstance() {
        DetailListFragment fragment = new DetailListFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_detail_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mEtTitle = (NoteEditText) view.findViewById(R.id.et_title);
        mEtTitle.requestFocus();

        mEtTitle.setOnEditorActionListener(this);
        mEtTitle.addTextChangedListener(this);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.detail_list_view);
        
        LayoutManagerFactory factory = new LayoutManagerFactory();
        RecyclerView.LayoutManager layoutManager = factory.getLayoutManager(getContext(), false);

        initData(null, mNote);

        mRecyclerView.setLayoutManager(layoutManager);

        DetailListAdapter adapter = new DetailListAdapter(getContext(), mDetailLists, this);
        mRecyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
        
        if (!TextUtils.isEmpty(mTitle)) {
            mEtTitle.setText(mTitle);
        }
    }

    /**
     * 初始化数据
     * @param list 已有的数据
     */
    private void initData(List<DetailList> list, NoteInfo note) {
        if (mDetailLists == null || mDetailLists.size() == 0) {
            mDetailLists = new ArrayList<>();
            if (list != null && list.size() > 0) {
                mDetailLists.addAll(list);
            } else {
                DetailList detail = new DetailList();
                if (note != null) {
                    detail.setNoteId(note.getSId());
                }
                mDetailLists.add(detail);
            }
        } else {
            if (list != null && list.size() > 0) {
                mDetailLists.clear();
                mDetailLists.addAll(list);
            }
        }
    }

    /**
     * 获取清单项
     * @return
     */
    public List<DetailList> getDetailLists() {
        return mDetailLists;
    }

    /**
     * 是否有清单项，只要有一项有内容，都算有
     * @return
     */
    public boolean hasDetailList() {
        boolean hasDetail = false;
        if (mDetailLists != null && mDetailLists.size() > 0) {
            hasDetail = true;
            /*for (DetailList detail : mDetailLists) {
                hasDetail = !TextUtils.isEmpty(detail.getTitle());
                if (hasDetail) {
                    break;
                }
            }*/
        }
        return hasDetail;
    }

    /**
     * 清单第一项是否为空
     * @return
     */
    private boolean isFirstDetailEmpty() {
        DetailList detail = mDetailLists.get(0);
        return detail == null || detail.isEmptyText();
    }

    /**
     * 获取指定位置的清单列表的holder
     * @param position 数据的位置
     * @return
     */
    private DetailListAdapter.DetailListViewHolder getDetailHolder(int position) {
        return (DetailListAdapter.DetailListViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
    }

    /**
     * 将清单的标题获取焦点
     * @param position
     */
    private void focusItem(final int position) {
        mRecyclerView.scrollToPosition(position);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                DetailListAdapter.DetailListViewHolder holder = getDetailHolder(position);
                KLog.d(TAG, "----focusItem---position----" + position + "---holder--" + holder);
                if (holder != null) {
                    holder.mDetailTextWatcher.setPosition(position);
                    DetailListAdapter adapter = (DetailListAdapter) mRecyclerView.getAdapter();
                    adapter.setSelectPosition(position);
                    if (!holder.etTitle.hasFocus()) {
                        holder.etTitle.requestFocus();
                    }
                }
            }
        });
        
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDetailInteractionListener) {
            mListener = (OnDetailInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mNote = null;
        mListener = null;
        KLog.d(TAG, "--list--" + mDetailLists);
        super.onDetach();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //回车换行
            switch (v.getId()) {
                case R.id.et_title: //笔记的标题，则移到第一个清单或者在第一位添加一项清单
                    KLog.d(TAG, "--onEditorAction---et_title---");
                    if (isFirstDetailEmpty()) { //第一项为空，则第一项将获取焦点
                        focusItem(0);
                    } else {    //第一项不为空，则在第一项之前再插入一项
                        addItem(0, null);
                    }
                    return true;
            }
        }
        return false;
    }

    /**
     * 添加一行
     * @param position 当前位置
     * @param text 文本内容                
     * @return
     */
    private int addItem(int position, CharSequence text) {
        DetailListAdapter adapter = (DetailListAdapter) mRecyclerView.getAdapter();
        if (position != 0) {
            position = adapter.getSelectPosition();
            position = position + 1;
        }
        int count = adapter.getItemCount();
        DetailList detail = new DetailList();
        if (mNote != null) {
            detail.setNoteId(mNote.getSId());
        }
        if (!TextUtils.isEmpty(text)) {
            detail.setTitle(text.toString());
        }
        detail.setSort(position);
        detail.setOldSort(position);
        if (position == count) {    //最后一个
            mDetailLists.add(detail);
        } else {    //中间的某个
            mDetailLists.add(position, detail);
        }
        adapter.notifyItemInserted(position);
        count = adapter.getItemCount();
        if (position < count - 1) { //不是最后一个元素
            adapter.notifyItemRangeChanged(position + 1, count - position - 1);
        }
        final int addPosition = position;
        focusItem(addPosition);
        KLog.d(TAG, "--onEditorAction---addPosition---" + position);
        return addPosition;
    }

    /**
     * 移除一项清单
     * @return
     */
    private boolean removeItem() {
        DetailListAdapter adapter = (DetailListAdapter) mRecyclerView.getAdapter();
        int position = adapter.getSelectPosition();
        if (position == 0 || adapter.getItemCount() == 1) {
            KLog.d(TAG, "---position---" + position + "--or--size--1--can---not---remove---");
            return false;
        }
        
        mDetailLists.remove(position);
        removeDoneDetail(position);
        
        adapter.notifyItemRemoved(position);

        int count = adapter.getItemCount();

        resetSort(0, count);
        
        KLog.d(TAG, "--removeItem--mDetailLists---" + mDetailLists);
        
        adapter.notifyItemRangeChanged(position, count - 1);

        position = position >= count ? count - 1 : position; 
        
        focusItem(position);
        return true;
    }

    /**
     * 为文本删除中划线
     * @param isDone 该清单是否已完成，完成，则添加中划线
     */
    private void styleDetailView(DetailListAdapter.DetailListViewHolder holder, boolean isDone) {
        if (isDone) {   //添加中划线
            if (holder.etTitle.isEnabled()) {
                holder.etTitle.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG); //中划线
                holder.etTitle.setEnabled(false);
            }
            if (holder.etTitle.hasFocus()) {
                mEtTitle.requestFocus();
            }
            if (holder.ivSort.getVisibility() == View.VISIBLE) {
                holder.ivSort.setVisibility(View.GONE);
            }
        } else {
            if (!holder.etTitle.isEnabled()) {
                holder.etTitle.getPaint().setFlags(0); //删除中划线
                holder.etTitle.setEnabled(true);
            }
            if (holder.ivSort.getVisibility() != View.VISIBLE) {
                holder.ivSort.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 添加已完成的清单项
     * @param detail
     */
    private void putDoneDetail(DetailList detail) {
        if (mDoneDetails == null) {
            mDoneDetails = new LinkedList<>();
        }
        mDoneDetails.add(detail);
    }
    

    /**
     * 移除已完成的清单项
     * @param detail
     */
    private void removeDoneDetail(DetailList detail) {
        if (mDoneDetails != null) {
            mDoneDetails.remove(detail);
        }
    }

    /**
     * 移除已完成的清单项
     * @param position
     */
    private void removeDoneDetail(int position) {
        if (mDoneDetails != null && mDoneDetails.size() > position) {
            mDoneDetails.remove(position);
        }
    }
    
    /**
     * 清单是否全部完成
     */
    private boolean isAllDone() {
        return mDoneDetails != null && mDoneDetails.size() == mDetailLists.size();
    }

    /**
     * 重置指定区域的排序
     * @param fromPosition 开始区间，包含该边界
     * @param toPosition 结束区间，不包含该边界
     */
    private void resetSort(int fromPosition, int toPosition) {
        for (int i = fromPosition; i < toPosition; i++) {
            DetailList d = mDetailLists.get(i);
            d.setSort(i);
            d.setOldSort(i);
        }
    }

    /**
     * 更新排序
     * @param fromDetail
     * @param toDetail
     */
    private void updateSort(DetailList fromDetail, DetailList toDetail) {
        int fromSort = fromDetail.getSort();
        int toSort = toDetail.getSort();
        fromDetail.setSort(toSort);
        toDetail.setSort(fromSort);
    }

    /**
     * 设置当前编辑的笔记
     * @param note
     */
    public void setNoteInfo(NoteInfo note) {
        this.mNote = note;
    }

    /**
     * 设置文本
     * @param text
     */
    public void setText(CharSequence text, NoteInfo note) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        String sid = note == null ? null : note.getSId();
        List<DetailList> list = analysisText(text.toString(), sid);
        if (list != null && list.size() > 0) {
            initData(list, note);
        }
    }

    /**
     * 获取纯文本，包含标题和清单各项内容
     * @param hasDetail 是否包含清单内容
     * @return
     */
    public CharSequence getText(boolean hasDetail) {
        CharSequence title = "";
        if (mTitle != null) {
            title = mTitle;
        }
        if (hasDetail) {
            StringBuilder builder = new StringBuilder();
            builder.append(title).append(Constants.TAG_NEXT_LINE);
            if (mDetailLists != null && mDetailLists.size() > 0) {
                for (DetailList detail : mDetailLists) {
                    title = detail.getTitle();
                    title = title == null ? "" : title;
                    builder.append(title).append(Constants.TAG_NEXT_LINE);
                }
            }
            builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_NEXT_LINE));
            return builder;
        } else {
            return title;
        }
    }

    /**
     * 获取纯文本,不包含清单的内容，只有标题
     * @return
     */
    @Override
    public CharSequence getText() {
        StringBuilder builder = new StringBuilder();
        if (mDetailLists != null && mDetailLists.size() > 0) {
            for (DetailList detail : mDetailLists) {
                String title = detail.getTitle();
                title = title == null ? "" : title;
                builder.append(title).append(Constants.TAG_NEXT_LINE);
            }
        }
        builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_NEXT_LINE));
        return builder;
    }

    @Override
    public CharSequence getTitle() {
        return mTitle;
    }

    @Override
    public NoteInfo.NoteKind getNoteType() {
        return NoteInfo.NoteKind.DETAILED_LIST;
    }
    
    /**
     * 将文本解析成清单
     * @param text
     * @param noteSid 笔记的id
     * @return
     */
    public List<DetailList> analysisText(String text, String noteSid) {
        int i = 0;
        StringTokenizer tokenizer = new StringTokenizer(text);
        String title = null;
        List<DetailList> list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if (i == 0) {
                title = line;
            } else {
                DetailList detail = new DetailList();
                detail.setTitle(line);
                detail.setNoteId(noteSid);
                list.add(detail);
            }
            i ++;
        }
        if (title == null) {
            mTitle = text;
        } else {
            mTitle = title;
        }
        return list;
    }

    /**
     * 改变清单的状态
     * @param detail
     */
    private void finishDetail(DetailList detail, int position) {
        DetailListAdapter.DetailListViewHolder holder = getDetailHolder(position);
        
        if (holder == null) {
            KLog.d(TAG, "--changeDetail----holder---is---null--detail--" + detail + "--position---" +position);
            return;
        }
        
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        int count = adapter.getItemCount();
        if (detail.isChecked()) {
            putDoneDetail(detail);
            styleDetailView(holder, true);
            boolean isAllDone = isAllDone();
            if (count > 1) {
                int toPosition = count - 1;
                
                detail.setOldSort(position);
                detail.setSort(toPosition);
                
                if (!isAllDone) {   //还有清单没有完成

                    for (int i = count - 1; i > position; i--) {
                        DetailList d = mDetailLists.get(i);
                        int sort = d.getSort();
                        if (sort > 0) {
                            d.setOldSort(sort);
                            d.setSort(sort - 1);
                        }
                    }

                    mDetailLists.remove(position);
                    mDetailLists.add(detail);

                } else {    //所有的清单都完成了，则重置清单的排序

                    mDetailLists.remove(position);
                    mDetailLists.add(detail);

                    resetSort(0, count);
                }
                KLog.d(TAG, "---changeDetail---check--fromPosition--" + position + "---toPosition--" + toPosition);
                adapter.notifyItemMoved(position, toPosition);  //移到最后
                adapter.notifyItemRangeChanged(position, toPosition - position + 1);
            }
        } else {
            removeDoneDetail(detail);
            styleDetailView(holder, false);

            if (count > 1) {
                int fromPosition = detail.getSort();
                int toPosition = detail.getOldSort();

                detail.setOldSort(fromPosition);
                detail.setSort(toPosition);

                for (int i = fromPosition - 1; i >= toPosition; i--) {
                    DetailList d = mDetailLists.get(i);
                    int sort = d.getSort();
                    if (sort > 0) {
                        d.setOldSort(sort);
                        d.setSort(sort + 1);
                    }
                }
                if (fromPosition != toPosition) {
                    mDetailLists.remove(fromPosition);
                    mDetailLists.add(toPosition, detail);
                    KLog.d(TAG, "---changeDetail--fromPosition--" + fromPosition + "---toPosition--" + toPosition);
                    adapter.notifyItemMoved(fromPosition, toPosition);  //移到之前的位置
                    adapter.notifyItemRangeChanged(fromPosition, toPosition - fromPosition + 1);
                }
            }
        }
        KLog.d(TAG, "--changeDetail---mDetailLists--" + mDetailLists);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder != null) {
            mItemTouchHelper.startDrag(viewHolder);
        } else {
            KLog.d(TAG, "---onStartDrag----viewHolder--is--null---");
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mTitle = s;
    }

    @Override
    public void showNote(DetailNoteInfo detailNote, Map<String, Attach> map) {
        KLog.d(TAG, "-----showNote---detailNote--" + detailNote);
        NoteInfo note = detailNote.getNoteInfo();
        String text = note.getContent();

        if (mTitle == null) {
            mTitle = note.getNoteTitle();
        }

        if (mEtTitle != null) {
            mEtTitle.setText(mTitle);
        }
        
        if (!TextUtils.isEmpty(text)) {
            if (detailNote.hasDetailList()) {   //已经有清单了
                initData(detailNote.getDetailList(), note);
            } else {
                setText(text, note);
            }
            
            if (mRecyclerView != null) {
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    /**
     * 清单列表的适配器
     */
    class DetailListAdapter extends RecyclerView.Adapter<DetailListAdapter.DetailListViewHolder> implements ItemTouchHelperAdapter {
        private Context context;

        private List<DetailList> list;

        private LayoutInflater inflater;

        private int mSelectPosition;

        private final OnStartDragListener mDragStartListener;
        
        //清除图标
        private Drawable mClearDrawable;
        
        public DetailListAdapter(Context context, List<DetailList> list, OnStartDragListener dragStartListener) {
            this.context = context;
            this.list = list;
            this.mDragStartListener = dragStartListener;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public DetailListAdapter.DetailListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_detail_list, parent, false);
            DetailListViewHolder holder = new DetailListViewHolder(view, new DetailTextWatcher());
            holder.etTitle.setOnEditorActionListener(new DetailEditorActionListener());
            holder.etTitle.setOnKeyListener(new DetailKeyListener());
            holder.etTitle.setOnFocusChangeListener(new DetailOnFocusChangeListener());
            DetailOnTouchListener touchListener = new DetailOnTouchListener();
            holder.etTitle.setOnTouchListener(touchListener);
            holder.ivSort.setOnTouchListener(touchListener);
            return holder;
        }

        @Override
        public void onBindViewHolder(DetailListViewHolder holder, int position) {
            int adapterPosition = holder.getAdapterPosition();
            holder.mDetailTextWatcher.setPosition(adapterPosition);
            holder.etTitle.setTag(adapterPosition);
            holder.ivSort.setTag(adapterPosition);
            
            DetailList detail = list.get(position);
            
            KLog.d(TAG, "-onBindViewHolder--adapterPosition---" + adapterPosition);

            holder.checkBox.setOnCheckedChangeListener(null);
            
            holder.checkBox.setChecked(detail.isChecked());

            styleDetailView(holder, detail.isChecked());

            holder.etTitle.setText(detail.getTitle());
            
            holder.checkBox.setOnCheckedChangeListener(new DetailOnCheckedChangeListener(holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        public int getSelectPosition() {
            return mSelectPosition;
        }

        public void setSelectPosition(int selectPosition) {
            this.mSelectPosition = selectPosition;
        }

        public Drawable getClearDrawable() {
            return mClearDrawable;
        }

        public void setClearDrawable(Drawable clearDrawable) {
            this.mClearDrawable = clearDrawable;
        }

        /**
         * 是否可以排序
         * @param position
         * @return true:可以排序
         */
        public boolean canSort(int position) {
            DetailList detail = list.get(position);
            return !detail.isChecked();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            if (!canSort(toPosition)) { //目的行不可排序
                KLog.d(TAG, "---drag---can--not----move---toPosition--" + toPosition);
                return false;
            }
            
            DetailList fromDetail = list.get(fromPosition);
            DetailList toDetail = list.get(toPosition);

            updateSort(fromDetail, toDetail);
            
            Collections.swap(list, fromPosition, toPosition);

            mEtTitle.requestFocus();

            notifyItemMoved(fromPosition, toPosition);
            
            notifyItemChanged(toPosition);
            notifyItemChanged(fromPosition);
            
            KLog.d(TAG, "---DetailLists----" + mDetailLists);
            
            return true;
        }

        @Override
        public void onItemDismiss(int position) {

        }

        /**
         * 清单列表的holder
         */
        class DetailListViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
            CheckBox checkBox;
            EditText etTitle;
            ImageView ivSort;

            private DetailTextWatcher mDetailTextWatcher;

            public DetailListViewHolder(View itemView, DetailTextWatcher detailTextWatcher) {
                super(itemView);

                checkBox = (CheckBox) itemView.findViewById(R.id.detail_check);
                etTitle = (EditText) itemView.findViewById(R.id.et_detail_title);
                ivSort = (ImageView) itemView.findViewById(R.id.iv_sort);

                this.mDetailTextWatcher = detailTextWatcher;

                etTitle.addTextChangedListener(detailTextWatcher);
                
            }

            @Override
            public void onItemSelected() {
                itemView.setBackgroundResource(R.drawable.drag_shadow);
            }

            @Override
            public void onItemClear() {
                itemView.setBackgroundResource(0);
            }
        }
        
        /**
         * 清单复选框选择的监听器
         * @author huanghui1
         * @update 2016/8/2 17:02
         * @version: 1.0.0
         */
        class DetailOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
            
            private int position;

            public DetailOnCheckedChangeListener(int position) {
                this.position = position;
            }

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DetailList detail = mDetailLists.get(position);
                if (detail != null) {
                    detail.setChecked(isChecked);
                    finishDetail(detail, position);
                } else {
                    KLog.d(TAG, "---DetailOnCheckedChangeListener---onCheckedChanged----detail---is---null---position--" + position);
                }
            }
        }

        /**
         * 清单列表项触摸的事件
         * @author huanghui1
         * @update 2016/8/2 16:50
         * @version: 1.0.0
         */
        class DetailOnTouchListener implements View.OnTouchListener {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Integer position = (Integer) v.getTag();
                if (position == null) {
                    return false;
                }
                switch (v.getId()) {
                    case R.id.et_detail_title:  //文本编辑框
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            focusItem(position);
                        }
                        break;
                    case R.id.iv_sort:  //排序按钮
                        DetailListAdapter adapter = (DetailListAdapter) mRecyclerView.getAdapter();
                        DetailListViewHolder holder = getDetailHolder(position);
                        if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {

                            if (holder != null && holder.etTitle.hasFocus() && adapter.getSelectPosition() == position) {  //当前行选中,是删除模式，则删除当前行
                                removeItem();
                            } else {
                                KLog.d(TAG, "-drag--position---" + position);
                                DetailListViewHolder viewHolder = getDetailHolder(position);
                                mDragStartListener.onStartDrag(viewHolder);
                            }
                        }
                        break;
                }
                
                return false;
            }
        }
        
        /**
         * 清单的焦点监听器
         * @author huanghui1
         * @update 2016/8/3 15:33
         * @version: 1.0.0
         */
        class DetailOnFocusChangeListener implements View.OnFocusChangeListener {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Integer position = (Integer) v.getTag();
                KLog.d(TAG, "---DetailOnFocusChangeListener---hasFocus---" + hasFocus + "--position----" + position);
                if (position == null || position == 0) {
                    return;
                }
                DetailListAdapter.DetailListViewHolder holder = getDetailHolder(position);
                if (holder != null) {
                    ImageView imageView = holder.ivSort;
                    if (hasFocus) {
                        DetailListAdapter adapter = (DetailListAdapter) mRecyclerView.getAdapter();
                        Drawable drawable = adapter.getClearDrawable();
                        if (drawable == null) {
                            int color = ResourcesCompat.getColor(getResources(), R.color.text_content_color, getContext().getTheme());
                            drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.abc_ic_clear_mtrl_alpha, getContext().getTheme());
                            drawable = DrawableCompat.wrap(drawable); //Wrap the drawable so that it can be tinted pre Lollipop
                            DrawableCompat.setTint(drawable, color);
                            adapter.setClearDrawable(drawable);
                        }
                        imageView.setImageDrawable(drawable);
                    } else {
                        imageView.setImageResource(R.drawable.ic_reorder_grey);
                    }
                }
            }
        }
        
        /**
         * 清单文本变化的监听器
         * @author huanghui1
         * @update 2016/8/2 16:51
         * @version: 1.0.0
         */
        class DetailTextWatcher implements TextWatcher {
            private int position;

            public void setPosition(int position) {
                this.position = position;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                DetailList detail = mDetailLists.get(position);
                detail.setTitle(s.toString());
//                KLog.d(TAG, "--position---" + position + "---title---" + detail.getTitle());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        }
        
        /**
         * 清单的回车键监听器
         * @author huanghui1
         * @update 2016/8/2 16:51
         * @version: 1.0.0
         */
        class DetailEditorActionListener implements TextView.OnEditorActionListener {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event == null) {
                    return false;
                }
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //回车换行
                    addItem(-1, null);
                    return true;
                }
                return false;
            }
        }
        
        /**
         * 清单列表项标题的键盘监听
         * @author huanghui1
         * @update 2016/8/3 10:11
         * @version: 1.0.0
         */
        class DetailKeyListener implements View.OnKeyListener {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event == null) {
                    return false;
                }
                if(event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {   //删除
                    return removeItem();
                }
                return false;
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDetailInteractionListener {
        // TODO: Update argument type and name
    }
}
