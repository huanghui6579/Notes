package net.ibaixin.notes.ui;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
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
import android.widget.TextView;

import com.socks.library.KLog;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.DetailList;
import net.ibaixin.notes.widget.LayoutManagerFactory;
import net.ibaixin.notes.widget.NoteEditText;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DetailListFragment.OnDetailInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DetailListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DetailListFragment extends Fragment implements TextView.OnEditorActionListener {
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
    private List<DetailList> mDoenDetails;

    //笔记的标题
    private CharSequence mTitle;

    //笔记的标题
    private NoteEditText mEtTitle;
    
    private RecyclerView mRecyclerView;
    
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

        mRecyclerView = (RecyclerView) view.findViewById(R.id.detail_list_view);
        
        LayoutManagerFactory factory = new LayoutManagerFactory();
        RecyclerView.LayoutManager layoutManager = factory.getLayoutManager(getContext(), false);

        initData();

        mRecyclerView.setLayoutManager(layoutManager);

        DetailListAdapter adapter = new DetailListAdapter(getContext(), mDetailLists);
        mRecyclerView.setAdapter(adapter);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        mDetailLists = new ArrayList<>();
        DetailList detail = new DetailList();
        mDetailLists.add(detail);
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
        super.onDetach();
        KLog.d(TAG, "--list--" + mDetailLists);
        mListener = null;
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
        mRecyclerView.getAdapter().notifyItemInserted(position);
        count = adapter.getItemCount();
        if (position < count - 1) { //不是最后一个元素
            mRecyclerView.getAdapter().notifyItemRangeChanged(position + 1, count - position - 1);
        }
        final int addPosition = position;
        focusItem(addPosition);
        KLog.d(TAG, "--onEditorAction---addPosition---" + position);
        return addPosition;
    }

    /**
     * 为文本删除中划线
     * @param isDone 该清单是否已完成，完成，则添加中划线
     */
    private void styleDetailView(TextView textView, boolean isDone) {
        if (isDone) {   //添加中划线
            if (textView.isEnabled()) {
                textView.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG); //中划线
                textView.setEnabled(false);
            }
        } else {
            if (!textView.isEnabled()) {
                textView.getPaint().setFlags(0); //删除中划线
                textView.setEnabled(true);
            }
        }
    }

    /**
     * 添加已完成的清单项
     * @param detail
     */
    private void putDownDetail(DetailList detail) {
        if (mDoenDetails == null) {
            mDoenDetails = new LinkedList<>();
        }
        mDoenDetails.add(detail);
    }
    

    /**
     * 移除已完成的清单项
     * @param detail
     */
    private void removeDownDetail(DetailList detail) {
        if (mDoenDetails != null) {
            mDoenDetails.remove(detail);
        }
    }

    /**
     * 清单是否全部完成
     */
    private boolean isAllDone() {
        return mDoenDetails != null && mDoenDetails.size() == mDetailLists.size();
    }

    /**
     * 改变清单的状态
     * @param detail
     */
    private void changeDetail(DetailList detail, int position) {
        KLog.d(TAG, "--changeDetail--detail--" + detail + "--position---" +position);
        DetailListAdapter.DetailListViewHolder holder = getDetailHolder(position);
        
        if (holder == null) {
            KLog.d(TAG, "--changeDetail----holder---is---null--detail--" + detail + "--position---" +position);
            return;
        }
        
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        int count = adapter.getItemCount();
        if (detail.isChecked()) {
            putDownDetail(detail);
            styleDetailView(holder.etTitle, true);
            boolean isAllDone = isAllDone();
            if (count > 1) {
                int toPosition = count - 1;

                if (!isAllDone) {
                    detail.setOldSort(position);
                    detail.setSort(toPosition);

                    for (int i = count - 1; i > position; i--) {
                        DetailList d = mDetailLists.get(i);
                        int sort = d.getSort();
                        if (sort > 0) {
                            d.setOldSort(sort);
                            d.setSort(sort - 1);
                        }
                    }
                } else {
                    for (DetailList d : mDetailLists) {
                        d.setOldSort(d.getSort());
                    }
                }
                mDetailLists.remove(position);
                mDetailLists.add(detail);

                adapter.notifyItemMoved(position, toPosition);  //移到最后
                adapter.notifyItemRangeChanged(position - 1, toPosition - position);
            } else {
                if (isAllDone) {
                    detail.setOldSort(detail.getSort());
                }
            }
        } else {
            removeDownDetail(detail);
            styleDetailView(holder.etTitle, false);

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
                mDetailLists.remove(fromPosition);
                mDetailLists.add(toPosition, detail);

                adapter.notifyItemMoved(fromPosition, toPosition);  //移到之前的位置
                adapter.notifyItemRangeChanged(fromPosition - 1, toPosition - fromPosition);
            }
        }
        KLog.d(TAG, "--changeDetail---mDetailLists--" + mDetailLists);
    }

    /**
     * 清单列表的适配器
     */
    class DetailListAdapter extends RecyclerView.Adapter<DetailListAdapter.DetailListViewHolder> {
        private Context context;

        private List<DetailList> list;

        private LayoutInflater inflater;

        private int mSelectPosition;
        
        public DetailListAdapter(Context context, List<DetailList> list) {
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public DetailListAdapter.DetailListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_detail_list, parent, false);
            DetailListViewHolder holder = new DetailListViewHolder(view, new DetailTextWatcher());
            holder.etTitle.setOnEditorActionListener(new DetailEditorActionListener());
            return holder;
        }

        @Override
        public void onBindViewHolder(DetailListViewHolder holder, int position) {
            holder.mDetailTextWatcher.setPosition(holder.getAdapterPosition());
            DetailList detail = list.get(position);

            holder.checkBox.setOnCheckedChangeListener(null);
            
            holder.checkBox.setChecked(detail.isChecked());

            styleDetailView(holder.etTitle, detail.isChecked());
            
            holder.etTitle.setText(detail.getTitle());
            holder.etTitle.setOnTouchListener(new DetailOnTouchListener(holder.getAdapterPosition()));
            
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

        /**
         * 清单列表的holder
         */
        class DetailListViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            EditText etTitle;

            private DetailTextWatcher mDetailTextWatcher;

            public DetailListViewHolder(View itemView, DetailTextWatcher detailTextWatcher) {
                super(itemView);

                checkBox = (CheckBox) itemView.findViewById(R.id.detail_check);
                etTitle = (EditText) itemView.findViewById(R.id.et_detail_title);

                this.mDetailTextWatcher = detailTextWatcher;

                etTitle.addTextChangedListener(detailTextWatcher);
                
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
                    changeDetail(detail, position);
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
            private int position;

            public DetailOnTouchListener(int position) {
                this.position = position;
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    focusItem(position);
                }
                return false;
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
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //回车换行
                    addItem(-1, null);
                    return true;
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
