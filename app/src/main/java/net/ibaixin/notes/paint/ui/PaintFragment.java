package net.ibaixin.notes.paint.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.ibaixin.notes.R;
import net.ibaixin.notes.paint.PaintData;
import net.ibaixin.notes.paint.PaintRecord;
import net.ibaixin.notes.paint.Painter;
import net.ibaixin.notes.paint.widget.PaintView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PaintFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PaintFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PaintFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PAINTER = "arg_painter";

    // TODO: Rename and change types of parameters
    private Painter mPainter;

    private OnFragmentInteractionListener mListener;
    
    //当前面板的画笔编辑数据
    private PaintData mPaintData;
    
    //画板
    private PaintView mPaintView;

    public PaintFragment() {
        // Required empty public constructor
    }

    public void setPaintData(PaintData paintData) {
        this.mPaintData = paintData;
        if (mPaintView != null) {
            mPaintView.setPaintData(paintData);
        }
    }

    /**
     * 设置画笔颜色的alpha
     * @param alpha
     */
    public void setPaintAlpha(int alpha) {
        if (mPaintView != null) {
            mPaintView.setPaintAlpha(alpha);
        }
    }

    /**
     * 设置画笔的颜色
     * @param color
     */
    public void setPaintColor(int color) {
        if (mPaintView != null) {
            mPaintView.setPaintColor(color);
        }
    }

    /**
     * 设置画笔的尺寸大小
     * @param size
     */
    public void setPaintSize(int size) {
        if (mPaintView != null) {
            mPaintView.setPaintSize(size);
        }
    }

    /**
     * 设置画笔的尺寸大小
     * @param size
     * @param paintType
     */
    public void setPaintSize(int size, int paintType) {
        if (paintType == PaintRecord.PAINT_TYPE_ERASE) {
            setEraseSize(size);
        } else {
            setPaintSize(size);
        }
    }

    /**
     * 设置橡皮檫的尺寸大小
     * @param size
     */
    public void setEraseSize(int size) {
        if (mPaintView != null) {
            mPaintView.setPaintSize(size, PaintRecord.PAINT_TYPE_ERASE);
        }
    }

    /**
     * 设置画笔的类型
     * @param type
     */
    public void setPaintType(int type) {
        if (mPaintView != null) {
            mPaintView.setPaintType(type);
        }
    }

    /**
     * 是否是橡皮檫模式
     * @return
     */
    public boolean isEraseType() {
        return mPaintView.getPaintType() == PaintRecord.PAINT_TYPE_ERASE;
    }
    
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param painter Parameter painter.
     * @return A new instance of fragment PaintFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PaintFragment newInstance(Painter painter) {
        PaintFragment fragment = new PaintFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PAINTER, painter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPainter = getArguments().getParcelable(ARG_PAINTER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_paint, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mPaintView = (PaintView) view.findViewById(R.id.paint_view);
        mPaintView.setPaintData(mPaintData);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
