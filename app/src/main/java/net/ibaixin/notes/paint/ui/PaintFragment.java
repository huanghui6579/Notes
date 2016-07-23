package net.ibaixin.notes.paint.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import net.ibaixin.notes.R;
import net.ibaixin.notes.paint.PaintData;
import net.ibaixin.notes.paint.PaintRecord;
import net.ibaixin.notes.paint.Painter;
import net.ibaixin.notes.paint.widget.PaintView;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.NotePopupWindow;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PaintFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PaintFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PaintFragment extends Fragment implements PaintView.OnDrawChangedListener, PaintView.TextWindowCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PAINTER = "arg_painter";
    private static final java.lang.String TAG = "PaintFragment";

    // TODO: Rename and change types of parameters
    private Painter mPainter;

    private OnFragmentInteractionListener mListener;
    
    //当前面板的画笔编辑数据
    private PaintData mPaintData;
    
    //画板
    private PaintView mPaintView;
    
    //画本文的弹窗
    private PopupWindow mTextWindow;
    
    //画板的高度
    private int mPintViewHeight;
    private int mPintViewWidth;
    
    /*//软键盘的高度
    private int mKeyboardHeight;
    //文本的偏移量
    private int mTextOffX;
    private int mTextOffY;*/

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
     * 设置画笔的实际颜色
     * @param color
     */
    public void setPaintRealColor(int color) {
        if (mPaintView != null) {
            mPaintView.setPaintRealColor(color);
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
     * 撤销
     */
    public void undo() {
        if (mPaintView != null) {
            mPaintView.undo();
        }
    }

    /**
     * 前进
     */
    public void redo() {
        if (mPaintView != null) {
            mPaintView.redo();
        }
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
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        mPaintView = (PaintView) view.findViewById(R.id.paint_view);

//        getPaintViewSize(mPaintView);

        mPaintView.setPaintData(mPaintData);

        mPaintView.setTextWindowCallback(this);

        mPaintView.setOnDrawChangedListener(this);

    }

    /**
     * 计算画板的尺寸
     * @param view
     */
    @Deprecated
    public void getPaintViewSize(final View view) {
        ViewTreeObserver vto = view.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                if (mPintViewHeight == 0 && mPintViewWidth == 0) {
                    int height = view.getMeasuredHeight();
                    int width = view.getMeasuredWidth();
                    mPintViewHeight = height;
                    mPintViewWidth = width;
                    Log.d(TAG, "--getPaintViewSize---mPintViewWidth--" + mPintViewWidth + "---mPintViewHeight---" + mPintViewHeight);
                }
                return true;
            }
        });
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
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        if (mPaintView != null) {
            mPaintView.setOnDrawChangedListener(null);
        }
        super.onDestroy();
    }

    @Override
    public void onDrawChanged() {
       if (mListener != null) {
           mListener.onDrawChange(mPaintView.getUndoCount(), mPaintView.getRedoCount());
       }
    }

    @Override
    public void onText(View view, PaintRecord paintRecord) {
        showTextWindow(view, paintRecord);
    }

    /**
     * 显示文本的弹窗
     * @param author
     * @param record
     */
    private void showTextWindow(View author, final PaintRecord record) {
        Context context = getContext();
        if (mTextWindow == null) {
            mTextWindow = new NotePopupWindow(context);

            final EditText editText = new EditText(context);
            editText.setMaxEms(20);
            editText.setMinEms(10);
            editText.setTextSize(16.0f);
            editText.setSelectAllOnFocus(true);

            mTextWindow.setContentView(editText);
            mTextWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            mTextWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            mTextWindow.setFocusable(true);
            mTextWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        }
        mTextWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                EditText editText = (EditText) mTextWindow.getContentView();
                if (editText.getText() != null && !TextUtils.isEmpty(editText.getText())) {
                    record.text = editText.getText().toString();
                    record.textPaint.setTextSize(editText.getTextSize());
                    record.textWidth = editText.getWidth();
                    mPaintView.addRecord(record);
                }
            }
        });
        mTextWindow.showAtLocation(author, Gravity.CENTER, 0, 0);
        mTextWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        SystemUtil.toggleSoftInput(context);
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

        /**
         * 画笔的步骤数量变化
         * @param undoSize 撤销的步骤数量
         * @param redoSize 前进的步骤数量                
         */
        void onDrawChange(int undoSize, int redoSize);

    }
}
