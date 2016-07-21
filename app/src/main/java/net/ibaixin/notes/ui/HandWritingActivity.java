package net.ibaixin.notes.ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v7.view.menu.ActionMenuItemView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import net.ibaixin.notes.widget.NotePopupWindow;

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

        setMenuTint(penItem, Color.WHITE);
        
//        MenuItem eraseItem = menu.findItem(R.id.action_eraser);

        int disableColor = SystemUtil.adjustAlpha(Color.WHITE, Constants.MENU_ITEM_COLOR_ALPHA);
        
        MenuItem undoItem = menu.findItem(R.id.action_undo);
        setMenuTint(undoItem, disableColor);

        MenuItem redoItem = menu.findItem(R.id.action_redo);
        setMenuTint(redoItem, disableColor);
        /*
        if (mPaintType == PaintRecord.PAINT_TYPE_ERASE) {   //橡皮檫模式
            eraseItem.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_bar_item_erase_checked, getTheme()));
        } else {
            Drawable drawable = getTintDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_bar_item_brush_checked, getTheme()), Color.WHITE);
            penItem.setIcon(drawable);
        }*/
        
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
            mMenuBrushView = (ActionMenuItemView) mToolBar.findViewById(R.id.action_pen);
            mMenuBrushView.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_bar_item_brush, getTheme()));
        }
    }
    
    /**
     * 显示画笔的弹窗
     * @param author
     */
    private void showStrokeWindow(View author) {

        if (mStrokePopupWindow == null) {
            mStrokePopupWindow = createStrokeWindow(R.layout.popup_brush_panel);
        }

        PopupWindowCompat.showAsDropDown(mStrokePopupWindow, author, 0, 0, Gravity.TOP | Gravity.START);
    }
    
    private void showEraseWindow(View author) {
        if (mErasePopupWindow == null) {
            mErasePopupWindow = createEraseWindow(R.layout.popup_erase_panel);
        }
        PopupWindowCompat.showAsDropDown(mErasePopupWindow, author, 0, 0, Gravity.TOP | Gravity.START);
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
     * 创建弹窗
     * @param layoutId
     * @return
     */
    private PopupWindow createStrokeWindow(int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootPanel = inflater.inflate(layoutId, null);

        Resources resources = getResources();
        
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
                
                switch (checkedId) {
                    case R.id.stroke_draw:  //画笔
                        mPaintType = PaintRecord.PAINT_TYPE_DRAW;
                        break;
                    case R.id.stroke_line:  //直线
                        mPaintType = PaintRecord.PAINT_TYPE_LINE;
                        break;
                    case R.id.stroke_circle:  //圆形
                        mPaintType = PaintRecord.PAINT_TYPE_CIRCLE;
                        break;
                    case R.id.stroke_rectangle:  //矩形
                        mPaintType = PaintRecord.PAINT_TYPE_RECTANGLE;
                        break;
                    case R.id.stroke_text:  //文字
                        mPaintType = PaintRecord.PAINT_TYPE_TEXT;
                        break;
                }
                Drawable[] drawables = new Drawable[2];
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
                PaintFragment paintFragment = getPaintFragment();
                paintFragment.setPaintColor(color);
                
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
                PaintFragment paintFragment = getPaintFragment();
                paintFragment.setPaintAlpha(alpha);
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

        int popuWidth = SystemUtil.measureContentWidth(rootPanel, mPopupMaxWidth);

        NotePopupWindow popupWindow = new NotePopupWindow(mContext);
        popupWindow.setContentView(rootPanel);
        popupWindow.setWidth(popuWidth);
        popupWindow.setFocusable(true);
        
        return popupWindow;
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

        int popuWidth = SystemUtil.measureContentWidth(rootPanel, mPopupMaxWidth);

        NotePopupWindow popupWindow = new NotePopupWindow(mContext);
        popupWindow.setContentView(rootPanel);
        popupWindow.setWidth(popuWidth);
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
}
