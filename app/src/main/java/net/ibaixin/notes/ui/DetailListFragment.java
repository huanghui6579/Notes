package net.ibaixin.notes.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.socks.library.KLog;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.DetailList;
import net.ibaixin.notes.widget.LayoutManagerFactory;
import net.ibaixin.notes.widget.NoteEditText;

import java.util.ArrayList;
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
    private void focusItem(int position) {
        DetailListAdapter.DetailListViewHolder holder = getDetailHolder(position);
        if (holder != null) {
            mRecyclerView.scrollToPosition(0);
            holder.etTitle.requestFocus();
        }
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
                    if (isFirstDetailEmpty()) { //第一项为空，则第一项将获取焦点
                        focusItem(0);
                    } else {    //第一项不为空，则在第一项之前再插入一项
                        DetailList detail = new DetailList();
                        mDetailLists.add(0, detail);
                        mRecyclerView.getAdapter().notifyItemInserted(0);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                focusItem(0);
                            }
                        });
                        
                    }
                    return true;
                case R.id.et_detail_title:  //编辑的是清单的标题，则在临近的后面追加一项
                    break;
            }
        }
        return false;
    }

    /**
     * 清单列表的适配器
     */
    class DetailListAdapter extends RecyclerView.Adapter<DetailListAdapter.DetailListViewHolder> {
        private Context context;

        private List<DetailList> list;

        private LayoutInflater inflater;
        
        public DetailListAdapter(Context context, List<DetailList> list) {
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public DetailListAdapter.DetailListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_detail_list, parent, false);
            DetailListViewHolder holder = new DetailListViewHolder(view);
            holder.etTitle.addTextChangedListener(new DetailTextWatcher(0));
            holder.etTitle.setOnEditorActionListener(DetailListFragment.this);
            return holder;
        }

        @Override
        public void onBindViewHolder(DetailListViewHolder holder, int position) {
            
            DetailList detail = list.get(position);
            holder.checkBox.setChecked(detail.isChecked());
            holder.etTitle.setText(detail.getTitle());
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        /**
         * 清单列表的holder
         */
        class DetailListViewHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            EditText etTitle;

            public DetailListViewHolder(View itemView) {
                super(itemView);

                checkBox = (CheckBox) itemView.findViewById(R.id.detail_check);
                etTitle = (EditText) itemView.findViewById(R.id.et_detail_title);
                
            }
        }
        
        class DetailTextWatcher implements TextWatcher {
            private int position;

            public DetailTextWatcher(int position) {
                this.position = position;
            }

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
                KLog.d(TAG, "--position---" + position + "---title---" + detail.getTitle());
            }

            @Override
            public void afterTextChanged(Editable s) {

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
