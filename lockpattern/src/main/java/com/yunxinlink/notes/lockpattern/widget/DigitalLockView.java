package com.yunxinlink.notes.lockpattern.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.lockpattern.R;
import com.yunxinlink.notes.lockpattern.utils.ResourceUtils;
import com.yunxinlink.notes.lockpattern.utils.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 数字密码的控件
 * @author tiger
 * @version 1.0.0
 * @update 2016/9/3 16:05
 */
public class DigitalLockView extends RelativeLayout {
    /**
     * 4位密码
     */
    private static final int MAX_NUMBERS = 4;

    /**
     * 单元格
     */
    private List<Cell> mCells;

    //密码输入框
    private LinearLayout mInputLayout;

    //4位密码数字
    private List<Integer> mDigitals = new ArrayList<>(MAX_NUMBERS);

    /**
     * 密码输入状态的监听器
     */
    private OnInputChangedListener mInputChangedListener;

    public DigitalLockView(Context context) {
        this(context, null);
    }

    public DigitalLockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DigitalLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private View init(Context context) {
        LinearLayout inputLayout = initDigitalInput(context);
        int viewId = ViewUtil.generateViewId();
        inputLayout.setId(viewId);
        RelativeLayout.LayoutParams inputParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        inputLayout.setLayoutParams(inputParams);
        inputParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        addView(inputLayout);

        GridView gridView = initGridView(context);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        params.addRule(RelativeLayout.BELOW, viewId);

        params.topMargin = 16;

        gridView.setLayoutParams(params);

        addView(gridView);

        return null;
    }

    /**
     * 初始化数字九宫格
     * @param context
     * @return
     */
    private GridView initGridView(Context context) {
        GridView gridView = new GridView(context);
        gridView.setNumColumns(3);
        gridView.setCacheColorHint(getResources().getColor(android.R.color.transparent));

        final List<Cell> cells = initCells();

        if (mCells == null) {
            mCells = new ArrayList<>();
        } else {
            mCells.clear();
        }

        mCells.addAll(cells);

        GridItemClickListener gridItemClickListener = new GridItemClickListener();

        GridAdapter adapter = new GridAdapter(mCells, context);

        adapter.setOnItemClickListener(gridItemClickListener);

        gridView.setAdapter(adapter);

        return gridView;
    }

    /**
     * 初始化数字输入框
     * @param context
     * @return
     */
    private LinearLayout initDigitalInput(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < MAX_NUMBERS; i++) {

            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            CheckBox checkBox = new CheckBox(context);
            checkBox.setBackgroundResource(0);
            checkBox.setClickable(false);
            checkBox.setFocusable(false);
            checkBox.setButtonDrawable(R.drawable.alp_digital_check_color_dark);
            frameParams.gravity = Gravity.CENTER;
            checkBox.setLayoutParams(frameParams);

            frameParams.leftMargin = 32;
            frameParams.rightMargin = 32;

            linearLayout.addView(checkBox);
        }

        mInputLayout = linearLayout;
        return linearLayout;
    }

    /**
     * 初始化数字
     * @return
     */
    private List<Cell> initCells() {
        List<Cell> list = new ArrayList<>();
        //先添加9个基本数字
        int size = 9;
        for (int i = 1; i <= size; i++) {
            Cell cell = new Cell();
            cell.type = Cell.TYPE_NUMBER;
            cell.name = String.valueOf(i);
            list.add(cell);
        }
        //添加取消按钮
        Cell cancelCell = new Cell();
        cancelCell.type = Cell.TYPE_CANCEL;
        cancelCell.name = "";
        list.add(cancelCell);
        //添加0按钮
        Cell zeroCell = new Cell();
        zeroCell.type = Cell.TYPE_NUMBER;
        zeroCell.name = String.valueOf(0);
        list.add(zeroCell);
        //添加删除按钮
        Cell delCell = new Cell();
        delCell.type = Cell.TYPE_DEL;
        delCell.resId = R.drawable.ic_content_backspace;
        list.add(delCell);
        KLog.d("list:" + list);
        return list;
    }

    public void setInputChangedListener(OnInputChangedListener inputChangedListener) {
        this.mInputChangedListener = inputChangedListener;
    }

    /**
     * 获取输入的密码
     * @return
     */
    public String getDigital() {
        if (mDigitals != null && mDigitals.size() > 0) {    //有密码
            String text = "";
            for (Integer number : mDigitals) {
                text += number;
            }
            return text;
        }
        return null;
    }

    /**
     * 清除密码
     */
    private void resetDigital() {
        mDigitals.clear();
        if (mInputLayout != null) {
            int size = mInputLayout.getChildCount();
            for (int i = 0; i < size; i++) {
                CheckBox checkBox = (CheckBox) mInputLayout.getChildAt(i);
                checkBox.setChecked(false);
            }
        }
        if (mInputChangedListener != null) {
            mInputChangedListener.onInputCleared();
        }
        KLog.d("resetDigital mDigitals:" + mDigitals);
    }

    /**
     * 设置密码输入框的选中状态
     * @param index 需要改变的索引
     * @param isChecked 是否选中
     */
    private void setCheckState(int index, boolean isChecked) {
        if (mInputLayout != null) {
            CheckBox checkBox = (CheckBox) mInputLayout.getChildAt(index);
            checkBox.setChecked(isChecked);
        }
    }

    /**
     * 一个个删除密码，从最后一个开始删除
     */
    private void delDigital() {
        if (mDigitals.size() > 0) {
            //最后一个
            int index = mDigitals.size() - 1;
            if (mInputLayout != null) {
                CheckBox checkBox = (CheckBox) mInputLayout.getChildAt(index);
                checkBox.setChecked(false);
            }
            mDigitals.remove(index);
            setCheckState(index, false);
            if (mInputChangedListener != null) {
                String text = getDigital();
                mInputChangedListener.onInput(text);
            }
            KLog.d("delDigitals mDigitals:" + mDigitals);
        } else {
            if (mInputChangedListener != null) {
                mInputChangedListener.onInputCleared();
            }
            KLog.d("delDigitals no digital");
        }
    }

    /**
     * 添加密码数字
     * @param number 当前输入的数字
     * @return
     */
    private void addDigital(int number) {
        if (mDigitals.size() < MAX_NUMBERS) {//没有输入完成
            mDigitals.add(number);
            int index = mDigitals.size() - 1;

            setCheckState(index, true);
            if (mDigitals.size() == MAX_NUMBERS) {  //输入完成
                if (mInputChangedListener != null) {
                    String text = getDigital();
                    mInputChangedListener.onInputCompleted(text);
                }
            } else {    //没有输入完成
                if (mInputChangedListener != null) {
                    String text = getDigital();
                    mInputChangedListener.onInput(text);
                }
            }
        } else {
            if (mInputChangedListener != null) {
                String text = getDigital();
                mInputChangedListener.onInputCompleted(text);
            }
        }
        KLog.d("addDigital mDigitals:" + mDigitals);
    }

    /**
     * 九宫格的适配器
     */
    class GridAdapter extends BaseAdapter {
        private List<Cell> mCells;
        private Context mContext;

        private OnItemClickListener mOnItemClickListener;

        int mViewType = 3;

        public GridAdapter(List<Cell> list, Context context) {
            this.mCells = list;
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return mCells.size();
        }

        @Override
        public Object getItem(int position) {
            return mCells.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return mViewType;
        }

        @Override
        public int getItemViewType(int position) {
            Cell cell = mCells.get(position);

            return cell.type;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Cell cell = mCells.get(position);
            int colorId = ResourceUtils.resolveResourceId(mContext, R.attr.colorPrimary);
            int color = ResourcesCompat.getColor(getResources(), colorId, mContext.getTheme());
            int viewType = getItemViewType(position);
            if (viewType == Cell.TYPE_DEL) {    //删除项
                GridDelViewHolder delHolder = null;
                if (convertView == null) {
                    delHolder = new GridDelViewHolder();

                    SquareFrameLayout frameLayout = new SquareFrameLayout(mContext);

                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

                    params.gravity = Gravity.CENTER;
                    ImageView imageView = new ImageView(mContext);

                    imageView.setLayoutParams(params);
                    frameLayout.addView(imageView);

                    delHolder.imageView = imageView;

                    convertView = frameLayout;
                } else {
                    delHolder = (GridDelViewHolder) convertView.getTag();
                }
                int resId = cell.resId;
                if (resId != 0) {
                    Drawable drawable = ResourcesCompat.getDrawable(getResources(), resId, mContext.getTheme());
                    if (drawable != null) {

                        DrawableCompat.setTint(drawable, color);

                        delHolder.imageView.setImageDrawable(drawable);
                    }
                }
            } else {
                GridViewHolder holder = null;
                if (convertView == null) {
                    holder = new GridViewHolder();

                    TextView textView = new SquareTextView(mContext);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(48);
                    textView.setTextColor(color);

                    holder.textView = textView;

                    convertView = textView;

                    convertView.setTag(holder);

                } else {
                    holder = (GridViewHolder) convertView.getTag();
                }

                String name = cell.name;
                if (TextUtils.isEmpty(name)) {
                    holder.textView.setVisibility(View.GONE);
                } else {
                    holder.textView.setVisibility(View.VISIBLE);
                    holder.textView.setText(name);
                }
                KLog.d("name:" + name + ", position:" + position);
            }
            convertView.setFocusable(true);
            convertView.setClickable(true);
            convertView.setBackgroundResource(R.drawable.list_item_borderless_selector);
            convertView.setOnClickListener(new OnViewClickListener(convertView, position, mOnItemClickListener));
            convertView.setOnLongClickListener(new GridItemLongClickListener(position));
            return convertView;
        }
    }

    /**
     * 九宫格的点击事件
     */
    class GridItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(View view, int position) {
            Cell cell = mCells.get(position);
            if (cell != null && !cell.isCancelBtn()) {
                if (cell.isDelBtn()) {  //清除密码
                    delDigital();
                } else {    //数字
                    try {
                        addDigital(Integer.parseInt(cell.name));
                    } catch (NumberFormatException e) {
                        KLog.e("format error : " + e.getMessage());
                    }
                }
                KLog.d("item click:" + cell + ", position:" + position);
            }
        }
    }

    /**
     * 九宫格每一项长按事件
     */
    class GridItemLongClickListener implements OnLongClickListener {
        private int position;

        public GridItemLongClickListener(int position) {
            this.position = position;
        }

        @Override
        public boolean onLongClick(View v) {
            Cell cell = mCells.get(position);
            if (cell != null && cell.isDelBtn()) {  //长按清除密码
                resetDigital();
                return true;
            }
            return false;
        }
    }

    class GridViewHolder {
        TextView textView;
    }

    class GridDelViewHolder {
        ImageView imageView;
    }

    interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    /**
     * 密码输入变化的监听器
     */
    interface OnInputChangedListener {
        /**
         * 输入完成后的回调
         * @param digital 输入的密码字符串
         */
        void onInputCompleted(String digital);

        /**
         * 输入中，还没有输入完成
         * @param digital 已经输入的密码
         */
        void onInput(String digital);

        /**
         * 密码清除后的回调
         */
        void onInputCleared();
    }

    class OnViewClickListener implements OnClickListener {

        private View view;
        private int position;

        private OnItemClickListener mOnItemClickListener;

        public OnViewClickListener(View view, int position, OnItemClickListener onItemClickListener) {
            this.view = view;
            this.position = position;
            this.mOnItemClickListener = onItemClickListener;
        }

        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view, position);
            }
        }
    }

    /**
     * 单元格的实体
     */
    class Cell {
        //数字类型
        static final int TYPE_NUMBER = 0;
        //删除类型
        static final int TYPE_DEL = 1;
        //取消类型
        static final int TYPE_CANCEL = 2;
        /**
         * 单元格类型
         */
        int type;

        /**
         * 单元格的名称
         */
        String name;

        /**
         * 单元格的图标资源id
         */
        int resId;

        /**
         * 是否是取消按钮
         * @return
         */
        public boolean isCancelBtn() {
            return type == TYPE_CANCEL;
        }

        /**
         * 是否是删除按钮
         * @return
         */
        public boolean isDelBtn() {
            return type == TYPE_DEL;
        }

        @Override
        public String toString() {
            return "Cell{" +
                    "type=" + type +
                    ", name='" + name + '\'' +
                    ", resId=" + resId +
                    '}';
        }
    }
}
