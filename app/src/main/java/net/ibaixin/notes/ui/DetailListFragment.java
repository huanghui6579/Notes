package net.ibaixin.notes.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

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
public class DetailListFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnDetailInteractionListener mListener;

    private List<DetailList> mDetailLists;

    //笔记的标题
    private CharSequence mTitle;

    //笔记的标题
    private NoteEditText mEtTitle;

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

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.detail_list_view);
        LayoutManagerFactory factory = new LayoutManagerFactory();
        RecyclerView.LayoutManager layoutManager = factory.getLayoutManager(getContext(), false);

        intData();

        recyclerView.setLayoutManager(layoutManager);

        DetailListAdapter adapter = new DetailListAdapter(getContext(), mDetailLists);
        recyclerView.setAdapter(adapter);
    }

    private void intData() {
        mDetailLists = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            DetailList detail = new DetailList();
            detail.setTitle("发动机看风景个" + i);
            mDetailLists.add(detail);
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
        mListener = null;
    }

    /**
     * 清单列表的适配器
     */
    class DetailListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Context context;

        private List<DetailList> list;

        private LayoutInflater inflater;

        public DetailListAdapter(Context context, List<DetailList> list) {
            this.context = context;
            this.list = list;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.item_detail_list, parent, false);
            return new DetailListViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            DetailList detail = list.get(position);
            DetailListViewHolder detailHolder = (DetailListViewHolder) holder;
            detailHolder.checkBox.setChecked(detail.isChecked());
            detailHolder.tvTitle.setText(detail.getTitle());
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

            EditText tvTitle;
            public DetailListViewHolder(View itemView) {
                super(itemView);

                checkBox = (CheckBox) itemView.findViewById(R.id.cb_check);
                tvTitle = (EditText) itemView.findViewById(R.id.tv_title);
            }
        }

        /**
         * 笔记编辑的holder
         */
        class NoteEditViewHolder extends RecyclerView.ViewHolder {
            NoteEditText editText;

            public NoteEditViewHolder(View itemView) {
                super(itemView);

                editText = (NoteEditText) itemView.findViewById(R.id.et_content);
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
