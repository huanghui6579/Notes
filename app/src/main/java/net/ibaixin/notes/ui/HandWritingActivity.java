package net.ibaixin.notes.ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.paint.PaintData;
import net.ibaixin.notes.paint.PaintRecord;
import net.ibaixin.notes.paint.Painter;
import net.ibaixin.notes.paint.ui.PaintFragment;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.NotePopupWindow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

//http://www.jianshu.com/p/7d3369b68785
public class HandWritingActivity extends BaseActivity implements PaintFragment.OnFragmentInteractionListener {
    
    private int mPopupMaxWidth;
    
    //画笔的基准尺寸
    private int mBaseSize;
    //画笔的尺寸
    private int mPaintSize = PaintRecord.DEFAULT_STROKE_SIZE;
    //画笔的类型
    private int mPaintType = PaintRecord.PAINT_TYPE_DRAW;
    //之前的画笔类型
    private int mPrePainType = mPaintType;
    //默认的画笔颜色
    private int mPaintColor = Color.BLACK;
    //画笔颜色的alpha
    private int mPaintAlpha = PaintRecord.DEFAULT_STROKE_ALPHA;
    //默认的橡皮檫尺寸
    private int mEraseSize = PaintRecord.DEFAULT_ERASER_SIZE;
    
    //画笔数据记录的集合
    private List<PaintData> mPaintList = new ArrayList<>();
    
    //画笔的弹窗
    private PopupWindow mStrokePopupWindow;
    private PopupWindow mErasePopupWindow;
    
    //画笔的面板
    private PaintFragment mPaintFragment;
    
    //画笔的菜单控件
    private ActionMenuItemView mMenuBrushView;
    //橡皮檫的菜单控件
    private ActionMenuItemView mMenuEraseView;
    
    //撤销菜单项
    private MenuItem mUndoItem;
    //前进菜单项
    private MenuItem mRedoItem;
    
    private Handler mHandler = new Handler();
    
    @Override
    protected int getContentView() {
        return R.layout.activity_hand_writing;
    }

    @Override
    protected void initData() {
        initPaintParams();

        PaintFragment paintFragment = attachContainer();

        PaintData paintData = new PaintData();
        mPaintList.add(paintData);

        paintFragment.setPaintData(paintData);
    }

    /**
     * 填充内容
     * @return
     */
    private PaintFragment attachContainer() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Painter painter = new Painter(mBaseSize, mPaintColor, mPaintType, mPaintAlpha, mEraseSize);

        PaintFragment paintFragment = PaintFragment.newInstance(painter);
        transaction.replace(R.id.content_container, paintFragment, paintFragment.getClass().getSimpleName());

        transaction.commit();
        
        return paintFragment;
    }

    @Override
    protected void initView() {
    }

    /**
     * 初始化画笔的参数
     */
    private void initPaintParams() {
        Resources resources = getResources();
        mPopupMaxWidth = Math.max(resources.getDisplayMetrics().widthPixels / 2,
                resources.getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_config_prefDialogWidth));

        mBaseSize = resources.getDimensionPixelSize(R.dimen.stroke_base_size);

        //默认画笔类型
        mPaintType = PaintRecord.PAINT_TYPE_DRAW;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.paint_edit, menu);
        
        MenuItem penItem = menu.findItem(R.id.action_pen);
        
        Drawable drawable = tintPenDrawable(mPaintColor);
        penItem.setIcon(drawable);

        int disableColor = SystemUtil.adjustAlpha(Color.WHITE, Constants.MENU_ITEM_COLOR_ALPHA);
        
        mUndoItem = menu.findItem(R.id.action_undo);
        
        tintDoMenuIcon(mUndoItem, R.drawable.ic_content_undo_normal, disableColor);
        
        mRedoItem = menu.findItem(R.id.action_redo);

        tintDoMenuIcon(mRedoItem, R.drawable.ic_content_redo_normal, disableColor);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View child = mToolBar.findViewById(item.getItemId());
        View view = getToolBarMenuView(child);
        if (view != null) {
            child.setSelected(true);
            switch (item.getItemId()) {
                case R.id.action_pen:
                    onStrokeChosed(view);
                    break;
                case R.id.action_eraser:
                    onEraseChosed(child, view);
                    break;
                case R.id.action_undo:  //撤销
                    undo();
                    break;
                case R.id.action_redo:  //前进
                    redo();
                    break;
            }
            
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示橡皮檫
     * @param view 菜单的父类控件
     */
    private void onStrokeChosed(View view) {
        PaintFragment paintFragment = getPaintFragment();
        //设置画笔的图标
        setupBrushMenuIcon();
        if (mMenuEraseView != null) {
            mMenuEraseView.setSelected(false);
        }
        if (paintFragment.isEraseType()) {  //之前选择的是橡皮檫，则此次选择画笔，不弹窗
            mPaintType = mPrePainType;
            paintFragment.setPaintType(mPaintType);
        } else {    //之前选择的是画笔，则此次弹窗
            showStrokeWindow(view);
        }
    }

    /**
     * 撤销
     */
    private void undo() {
        PaintFragment paintFragment = getPaintFragment();
        paintFragment.undo();
    }

    /**
     * 前进
     */
    private void redo() {
        PaintFragment paintFragment = getPaintFragment();
        paintFragment.redo();
    }

    /**
     * 显示橡皮檫
     * @param child 菜单的控件
     * @param view 菜单的父类控件
     */
    private void onEraseChosed(View child, View view) {
        PaintFragment paintFragment = getPaintFragment();
        if (mMenuEraseView == null) {
            mMenuEraseView = (ActionMenuItemView) child;
        }
        //设置画笔的图标
        setupBrushMenuIcon();
        mMenuBrushView.setSelected(false);
        if (!paintFragment.isEraseType()) {  //之前选择的是画笔，则此次选择橡皮檫，不弹窗
            mPrePainType = mPaintType;
            mPaintType = PaintRecord.PAINT_TYPE_ERASE;
            paintFragment.setPaintType(mPaintType);
        } else {    //之前选择的是橡皮檫，则此次弹窗
            showEraseWindow(view);
        }
    }

    /**
     * 设置画笔的图标
     */
    private void setupBrushMenuIcon() {
        if (mMenuBrushView == null) {
            Log.d(TAG, "--setupBrushMenuIcon----in--");
            mMenuBrushView = getBrushMenuItem();
            Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_bar_item_brush, getTheme());
            mMenuBrushView.setIcon(drawable);
        }
    }

    /**
     * 给画笔的菜单设置图标
     * @param drawable
     */
    private void setupBrushMenuIcon(Drawable drawable) {
        mMenuBrushView = getBrushMenuItem();
        mMenuBrushView.setIcon(drawable);
    }

    /**
     * 获取画笔的菜单项
     * @return
     */
    private ActionMenuItemView getBrushMenuItem() {
        if (mMenuBrushView == null) {
            mMenuBrushView = (ActionMenuItemView) mToolBar.findViewById(R.id.action_pen);
        }
        return mMenuBrushView;
    }
    
    /**
     * 显示画笔的弹窗
     * @param author
     */
    private void showStrokeWindow(View author) {

        if (mStrokePopupWindow == null) {
            mStrokePopupWindow = createStrokeWindow(R.layout.popup_brush_panel);
        }
        mStrokePopupWindow.showAsDropDown(author);
//        PopupWindowCompat.showAsDropDown(mStrokePopupWindow, author, 0, 0, Gravity.TOP | Gravity.START);
    }
    
    private void showEraseWindow(View author) {
        if (mErasePopupWindow == null) {
            mErasePopupWindow = createEraseWindow(R.layout.popup_erase_panel);
        }
        mErasePopupWindow.showAsDropDown(author);
//        PopupWindowCompat.showAsDropDown(mErasePopupWindow, author, 0, 0, Gravity.TOP | Gravity.START);
    }

    /**
     * 设置画笔尺寸或者橡皮檫的尺寸
     * @param view
     * @param progress 进度
     */
    private int calcSeekBarProgress(final View view, int progress) {
        int calProgress = progress > 1 ?  progress : 1;
        final int calSize = Math.round((mBaseSize / 100f) * calProgress);
        final int offset = Math.round((mBaseSize - calSize) / 2);
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.height = params.width = calSize;
        params.setMargins(offset, offset, offset, offset);
        view.setLayoutParams(params);
        
        return calSize;
    }

    /**
     * 设置画笔或者橡皮檫的尺寸大小
     * @param view
     * @param progress
     * @param paintType
     */
    private void setPaintSize(View view, int progress, int paintType) {
        int calcSize = calcSeekBarProgress(view, progress);
        PaintFragment paintFragment = getPaintFragment();
        if (paintFragment != null) {
            paintFragment.setPaintSize(calcSize, paintType);
        }
    }

    /**
     * 给LayerDrawable的第一个drawable着色
     * @param drawable
     * @param color 目标颜色
     * @return
     */
    private Drawable tintLayerDrawable(Drawable drawable, int color) {
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            Drawable firstDrawable = layerDrawable.getDrawable(0);
            getTintDrawable(firstDrawable, color);
            
            return layerDrawable;
        }
        return null;
    }

    /**
     * 给画笔的菜单图标着色
     * @param color
     * @return
     */
    private Drawable tintPenDrawable(int color) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_bar_item_brush_checked, getTheme());
        return tintLayerDrawable(drawable, color);
    }

    /**
     * 为撤销、前进的菜单设置图标
     * @param item 菜单项
     * @param resId 图标的id
     * @param disableColor 不可用的颜色
     */
    private void tintDoMenuIcon(MenuItem item, int resId, int disableColor) {
        StateListDrawable listDrawable = new StateListDrawable();

        Drawable normalDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme());
        getTintDrawable(normalDrawable, Color.WHITE);

        Drawable disableDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme()).mutate();
        getTintDrawable(disableDrawable, disableColor);

        listDrawable.addState(new int[] {-android.R.attr.state_enabled}, disableDrawable);
        listDrawable.addState(new int[] {}, normalDrawable);

        item.setIcon(listDrawable);
    }

    /**
     * 更新画笔的图标
     * @param resId 原始图标的资源id
     * @param color 着色的颜色
     */
    private void updateBrushMenuIcon(int resId, int color) {
        Resources resources = getResources();
        Drawable[] drawables = new Drawable[2];
        Drawable firstDrawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
        drawables[0] = firstDrawable;
        drawables[1] = ResourcesCompat.getDrawable(resources, R.drawable.ic_action_spinner_ink, getTheme());

        getTintDrawable(firstDrawable, color);

        LayerDrawable layerDrawable = new LayerDrawable(drawables);

        StateListDrawable listDrawable = new StateListDrawable();
        listDrawable.addState(new int[] {android.R.attr.state_checked}, layerDrawable);
        listDrawable.addState(new int[] {android.R.attr.state_selected}, layerDrawable);
        listDrawable.addState(new int[] {}, firstDrawable);

        setupBrushMenuIcon(listDrawable);
    }

    /**
     * 更新菜单图标的颜色
     * @param color
     */
    private void updateBrushMenuColor(int color) {
        mMenuBrushView = getBrushMenuItem();
        Drawable[] drawables = mMenuBrushView.getCompoundDrawables();
        if (drawables.length > 0 && drawables[0] instanceof StateListDrawable) {
            StateListDrawable listDrawable = (StateListDrawable) drawables[0];
            //给没有选择状态的图标着色
            tintNormalDrawable(listDrawable, color);
            //给选中的层级图标着色
            tintSelectedDrawable(listDrawable, color);
        }
    }

    /**
     * 创建弹窗
     * @param layoutId
     * @return
     */
    private PopupWindow createStrokeWindow(int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootPanel = inflater.inflate(layoutId, null);

        final Resources resources = getResources();
        
        int checkedColor = Color.BLACK;
        int normalColor = SystemUtil.adjustAlpha(checkedColor, 0.3f);

        RadioGroup strokeTypeGroup = (RadioGroup) rootPanel.findViewById(R.id.stroke_type_group);
        RadioButton strokeDraw = (RadioButton) rootPanel.findViewById(R.id.stroke_draw);
        RadioButton strokeLine = (RadioButton) rootPanel.findViewById(R.id.stroke_line);
        RadioButton strokeCircle = (RadioButton) rootPanel.findViewById(R.id.stroke_circle);
        RadioButton strokeRectangle = (RadioButton) rootPanel.findViewById(R.id.stroke_rectangle);
        RadioButton strokeText = (RadioButton) rootPanel.findViewById(R.id.stroke_text);

        strokeTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mPrePainType = mPaintType;
                
                int resId = 0;
                switch (checkedId) {
                    case R.id.stroke_draw:  //画笔
                        mPaintType = PaintRecord.PAINT_TYPE_DRAW;
                        resId = R.drawable.ic_action_brush;
                        break;
                    case R.id.stroke_line:  //直线
                        mPaintType = PaintRecord.PAINT_TYPE_LINE;
                        resId = R.drawable.ic_stroke_line;
                        break;
                    case R.id.stroke_circle:  //圆形
                        mPaintType = PaintRecord.PAINT_TYPE_CIRCLE;
                        resId = R.drawable.ic_stroke_circle;
                        break;
                    case R.id.stroke_rectangle:  //矩形
                        mPaintType = PaintRecord.PAINT_TYPE_RECTANGLE;
                        resId = R.drawable.ic_stroke_rectangle;
                        break;
                    case R.id.stroke_text:  //文字
                        mPaintType = PaintRecord.PAINT_TYPE_TEXT;
                        resId = R.drawable.ic_stroke_text;
                        break;
                }

                updateBrushMenuIcon(resId, mPaintColor);
                
                PaintFragment paintFragment = getPaintFragment();
                paintFragment.setPaintType(mPaintType);
            }
        });

        final ImageView ivStrokeSizeTip = (ImageView) rootPanel.findViewById(R.id.stroke_size_tip);
        SeekBar sbStrokeSize = (SeekBar) rootPanel.findViewById(R.id.sb_stroke_size);
        sbStrokeSize.setProgress(mPaintSize);
        setPaintSize(ivStrokeSizeTip, mPaintSize, PaintRecord.PAINT_TYPE_DRAW);

        sbStrokeSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setPaintSize(ivStrokeSizeTip, progress, PaintRecord.PAINT_TYPE_DRAW);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                
            }
        });
        
        final int redColor = ResourcesCompat.getColor(resources, android.R.color.holo_red_light, getTheme());
        final int greenColor = ResourcesCompat.getColor(resources, android.R.color.holo_green_light, getTheme());
        final int orangeColor = ResourcesCompat.getColor(resources, android.R.color.holo_orange_light, getTheme());
        final int blueColor = ResourcesCompat.getColor(resources, android.R.color.holo_blue_light, getTheme());
        
        RadioGroup strokeColorGroup = (RadioGroup) rootPanel.findViewById(R.id.stroke_color_group);

        strokeColorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int color = Color.BLACK;
                switch (checkedId) {
                    case R.id.stroke_black:
                        color = Color.BLACK;
                        break;
                    case R.id.stroke_red:
                        color = redColor;
                        break;
                    case R.id.stroke_green:
                        color = greenColor;
                        break;
                    case R.id.stroke_orange:
                        color = orangeColor;
                        break;
                    case R.id.stroke_blue:
                        color = blueColor;
                        break;
                }
                
                mPaintColor = color;

                updateBrushMenuColor(color);

                int alpha = (mPaintAlpha * 255) / 100;
                
                //合成实际的颜色
                int relColor = SystemUtil.calculColor(alpha, color);
                
                PaintFragment paintFragment = getPaintFragment();
                paintFragment.setPaintColor(color);
                paintFragment.setPaintRealColor(relColor);
                
            }
        });
        
        final ImageView ivStrokeAlpha = (ImageView) rootPanel.findViewById(R.id.stroke_alpha_tip);
        SeekBar sbStrokeAlpha = (SeekBar) rootPanel.findViewById(R.id.sb_stroke_alpha);

        sbStrokeAlpha.setProgress(mPaintAlpha);
        sbStrokeAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                
                int alpha = (progress * 255) / 100;//百分比转换成255级透明度
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ivStrokeAlpha.setImageAlpha(alpha);
                } else {
                    ivStrokeAlpha.setAlpha(alpha);
                }

                mPaintAlpha = progress;
                
                //合成实际的颜色
                int relColor = SystemUtil.calculColor(alpha, mPaintColor);
                
                updateBrushMenuColor(relColor);
                PaintFragment paintFragment = getPaintFragment();
                
                paintFragment.setPaintAlpha(alpha);
                paintFragment.setPaintRealColor(relColor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        
        Drawable drawDrawable = getCheckDrawable(R.drawable.ic_action_brush, normalColor, checkedColor);
        strokeDraw.setButtonDrawable(drawDrawable);
        
        Drawable lineDrawable = getCheckDrawable(R.drawable.ic_stroke_line, normalColor, checkedColor);
        strokeLine.setButtonDrawable(lineDrawable);
        
        Drawable circleDrawable = getCheckDrawable(R.drawable.ic_stroke_circle, normalColor, checkedColor);
        strokeCircle.setButtonDrawable(circleDrawable);
        
        Drawable rectangleDrawable = getCheckDrawable(R.drawable.ic_stroke_rectangle, normalColor, checkedColor);
        strokeRectangle.setButtonDrawable(rectangleDrawable);
        
        Drawable textDrawable = getCheckDrawable(R.drawable.ic_stroke_text, normalColor, checkedColor);
        strokeText.setButtonDrawable(textDrawable);

        int[] size = SystemUtil.measureContentSize(rootPanel, mPopupMaxWidth);

        NotePopupWindow popupWindow = new NotePopupWindow(mContext);
        popupWindow.setContentView(rootPanel);
        popupWindow.setWidth(size[0]);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        
        return popupWindow;
    }

    /**
     * 给没有选择状态的图标着色
     * @param listDrawable 状态的图标
     * @param color 要着色的颜色
     * @return
     */
    private void tintNormalDrawable(StateListDrawable listDrawable, int color) {
        //给默认的图标着色
        try {
            Method method = StateListDrawable.class.getDeclaredMethod("getStateDrawableIndex", int[].class);
            if (method != null) {
                method.setAccessible(true);
                int index = (int) method.invoke(listDrawable, StateSet.NOTHING);
                method = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                if (method != null) {
                    method.setAccessible(true);

                    Drawable normalDrawable = (Drawable) method.invoke(listDrawable, index);
                    getTintDrawable(normalDrawable, color);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "---tintNormalDrawable----error---" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * //给选中的层级图标着色
     * @param listDrawable
     * @param color
     */
    private void tintSelectedDrawable(StateListDrawable listDrawable, int color) {
        Drawable drawable = listDrawable.getCurrent();
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable;
            Drawable firstDrawable = layerDrawable.getDrawable(0);
            getTintDrawable(firstDrawable, color);
        }
    }

    /**
     * 获取画笔的面板
     * @return
     */
    private PaintFragment getPaintFragment() {
        if (mPaintFragment == null) {
            mPaintFragment = (PaintFragment) getSupportFragmentManager().findFragmentByTag(PaintFragment.class.getSimpleName());
        }
        return mPaintFragment;
    }

    /**
     * 创建橡皮檫的弹窗
     * @param layoutId
     * @return
     */
    private PopupWindow createEraseWindow(int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootPanel = inflater.inflate(layoutId, null);
        
        final ImageView ivEraseSizeTip = (ImageView) rootPanel.findViewById(R.id.erase_size_tip);
        SeekBar sbEraseSize = (SeekBar) rootPanel.findViewById(R.id.sb_erase_size);
        TextView tvClear = (TextView) rootPanel.findViewById(R.id.tv_clear);
        sbEraseSize.setProgress(mEraseSize);
        setPaintSize(ivEraseSizeTip, mEraseSize, PaintRecord.PAINT_TYPE_ERASE);
        sbEraseSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setPaintSize(ivEraseSizeTip, progress, PaintRecord.PAINT_TYPE_ERASE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int[] size = SystemUtil.measureContentSize(rootPanel, mPopupMaxWidth);

        NotePopupWindow popupWindow = new NotePopupWindow(mContext);
        popupWindow.setContentView(rootPanel);
        popupWindow.setWidth(size[0]);
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);

        return popupWindow;
    }

    /** 获取一个selector */
    public Drawable getCheckDrawable(int resId, int normalColor, int checkedColor) {

        Resources resources = getResources();
        Drawable normalDrawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
        getTintDrawable(normalDrawable, normalColor);

        Drawable checkedDrawable = ResourcesCompat.getDrawable(resources, resId, getTheme()).mutate();
        getTintDrawable(checkedDrawable, checkedColor);
        
        StateListDrawable listDrawable = new StateListDrawable();
        listDrawable.addState(new int[] { -android.R.attr.state_checked }, normalDrawable);
        listDrawable.addState(new int[] { android.R.attr.state_checked }, checkedDrawable);
        return listDrawable;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        
    }

    @Override
    public void onDrawChange(int undoSize, int redoSize) {
        if (undoSize > 0) {
            if (mUndoItem != null && !mUndoItem.isEnabled()) {
                mUndoItem.setEnabled(true);
            }
        } else {
            if (mUndoItem != null && mUndoItem.isEnabled()) {
                mUndoItem.setEnabled(false);
            }
        }
        if (redoSize > 0) {
            if (mRedoItem != null && !mRedoItem.isEnabled()) {
                mRedoItem.setEnabled(true);
            }
        } else {
            if (mRedoItem != null && mRedoItem.isEnabled()) {
                mRedoItem.setEnabled(false);
            }
        }
    }
}
