package net.ibaixin.notes.ui;

import android.Manifest;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.view.menu.ActionMenuItemView;
import android.text.TextUtils;
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

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import net.ibaixin.notes.R;
import net.ibaixin.notes.paint.PaintData;
import net.ibaixin.notes.paint.PaintRecord;
import net.ibaixin.notes.paint.Painter;
import net.ibaixin.notes.paint.ui.PaintFragment;
import net.ibaixin.notes.richtext.AttachSpec;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.ImageUtil;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.NotePopupWindow;

import java.io.File;
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

    private MenuItem mBrushItem;

    //撤销菜单项
    private MenuItem mUndoItem;
    //前进菜单项
    private MenuItem mRedoItem;
    
    //附件对象
    private AttachSpec mAttachSpec;
    
    private Handler mHandler = new Handler();
    
    @Override
    protected int getContentView() {
        return R.layout.activity_hand_writing;
    }

    @Override
    protected void initData() {
        
        Intent intent = getIntent();
        mAttachSpec = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
        
        if (mAttachSpec == null) {
            mAttachSpec = new AttachSpec();
            mAttachSpec.noteSid = SystemUtil.generateNoteSid();
        }
        
        initPaintParams();

        PaintFragment paintFragment = attachContainer(TextUtils.isEmpty(mAttachSpec.filePath));

        PaintData paintData = new PaintData();
        mPaintList.add(paintData);

        paintFragment.setPaintData(paintData);
    }

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    /**
     * 填充内容
     * @return
     */
    private PaintFragment attachContainer(boolean isNew) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Painter painter = new Painter(mBaseSize, mPaintColor, mPaintType, mPaintAlpha, mEraseSize);

        if (!isNew) {
            painter.isNew = false;
        }
        painter.attachSpec = mAttachSpec;

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

        mBrushItem = menu.findItem(R.id.action_pen);

        int alpha = getRelAlpha(mPaintAlpha);
        Drawable drawable = tintPenDrawable(mPaintType, mPaintColor, alpha);
        mBrushItem.setIcon(drawable);

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
                    onStrokeChose(view);
                    break;
                case R.id.action_eraser:
                    onEraseChose(child, view);
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

    @Override
    protected void onBack() {
//        finish();
        savePaintImage();
    }

    @Override
    public void onBackPressed() {
        onBack();
    }

    /**
     * 显示橡皮檫
     * @param view 菜单的父类控件
     */
    private void onStrokeChose(View view) {
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
    private void onEraseChose(View child, View view) {
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

            Drawable drawable = getBrushMenuIcon(mPaintType, mPaintColor, mPaintAlpha);
            mBrushItem.setIcon(drawable);
//            mMenuBrushView.setIcon(drawable);
        }
    }

    /**
     * 给画笔的菜单设置图标
     * @param drawable
     */
    private void setupBrushMenuIcon(Drawable drawable) {
//        mMenuBrushView = getBrushMenuItem();
        mBrushItem.setIcon(drawable);
    }

    /**
     * 根据画笔的类型、颜色、alpha来设置画笔菜单的图标
     * @param paintType 画笔的类型
     * @param paintColor 画笔的颜色，选择的颜色，可能不是实际颜色
     * @param paintAlpha 画笔颜色的alpha，画笔的颜色有alpha和选择的颜色合成
     */
    private Drawable getBrushMenuIcon(int paintType, int paintColor, int paintAlpha) {
        int resId = getDrawableRes(paintType);
        
        int alpha = getRelAlpha(paintAlpha);

        //合成后的实际颜色
        int relColor = SystemUtil.calcColor(alpha, paintColor);
        return getBrushMenuIcon(resId, relColor);
    }

    /**
     * 根据画笔的类型、实际颜色
     * @param resId 画笔的图标资源id
     * @param relColor 画笔的实际颜色
     */
    private Drawable getBrushMenuIcon(int resId, int relColor) {
        //选中后的小角标图标
        Resources resources = getResources();
        Drawable spinnerInkDrawable = resources.getDrawable(R.drawable.ic_action_spinner_ink);

        //正常非点击状态下的图标颜色
        Drawable normalDrawable = resources.getDrawable(resId);

        //给正常的图标着色
//        getTintDrawable(normalDrawable, relColor);
        if (SystemUtil.hasSdkV21()) {
            getTintDrawable(normalDrawable, relColor);

            //合成的选中后的图标
            Drawable[] drawables = new Drawable[2];
            drawables[0] = normalDrawable;
            drawables[1] = spinnerInkDrawable;
            LayerDrawable layerDrawable = new LayerDrawable(drawables);

            StateListDrawable listDrawable = new StateListDrawable();
            listDrawable.addState(new int[] {android.R.attr.state_checked}, layerDrawable);
            listDrawable.addState(new int[] {android.R.attr.state_selected}, layerDrawable);
            listDrawable.addState(new int[] {}, normalDrawable);
            
            return listDrawable;
            
        } else {
            int[] colors = new int[] {relColor, relColor};

            Drawable srcDrawable = getStateListDrawable(normalDrawable, colors);

            colors = new int[] {Color.WHITE, 0};
            Drawable linkDrawable = getStateListDrawable(spinnerInkDrawable, colors);
            Log.d(TAG, "---srcDrawable---" + srcDrawable);
            Log.d(TAG, "---linkDrawable---" + linkDrawable);
            return layerDrawable(srcDrawable, linkDrawable);
        }

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
        int offsetY = 0;
        if (!SystemUtil.hasSdkV21()) {
            offsetY = -16;
        }
        mStrokePopupWindow.showAsDropDown(author, 0, offsetY);
//        PopupWindowCompat.showAsDropDown(mStrokePopupWindow, author, 0, 0, Gravity.TOP | Gravity.START);
    }
    
    private void showEraseWindow(View author) {
        if (mErasePopupWindow == null) {
            mErasePopupWindow = createEraseWindow(R.layout.popup_erase_panel);
        }
        int offsetY = 0;
        if (!SystemUtil.hasSdkV21()) {
            offsetY = -16;
        }
        mErasePopupWindow.showAsDropDown(author, 0, offsetY);
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
     * 给画笔的图标的着色
     * @param paintType 画笔的类型
     * @param paintColor 画笔的选中颜色
     * @param paintAlpha 画笔的alpha,[0...100]
     * @return
     */
    private Drawable tintPenDrawable(int paintType, int paintColor, int paintAlpha) {
        int resId = getDrawableRes(paintType);
        Resources resources = getResources();
        Drawable drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
        Drawable inkDrawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_action_spinner_ink, getTheme());

        Drawable[] drawables = new Drawable[2];
        drawables[0] = drawable;
        drawables[1] = inkDrawable;

        LayerDrawable layerDrawable = new LayerDrawable(drawables);

        int relColor = SystemUtil.calcColor(paintAlpha, paintColor);

        return tintLayerDrawable(layerDrawable, relColor);
    }

    /**
     * 根据画笔的类型获取对应类型的图标
     * @param paintType 画笔的类型
     * @return
     */
    private int getDrawableRes(int paintType) {
        int resId = 0;
        switch (paintType) {
            case PaintRecord.PAINT_TYPE_DRAW:
                resId = R.drawable.ic_action_brush;
                break;
            case PaintRecord.PAINT_TYPE_LINE:
                resId = R.drawable.ic_stroke_line;
                break;
            case PaintRecord.PAINT_TYPE_CIRCLE:
                resId = R.drawable.ic_stroke_circle;
                break;
            case PaintRecord.PAINT_TYPE_RECTANGLE:
                resId = R.drawable.ic_stroke_rectangle;
                break;
            case PaintRecord.PAINT_TYPE_TEXT:
                resId = R.drawable.ic_stroke_text;
                break;
        }
        return resId;
    }

    /**
     * 为撤销、前进的菜单设置图标
     * @param item 菜单项
     * @param resId 图标的id
     * @param disableColor 不可用的颜色
     */
    private void tintDoMenuIcon(MenuItem item, int resId, int disableColor) {
        Drawable normalDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme());

        int[][] states = new int[2][];

        states[0] = new int[] {-android.R.attr.state_enabled};
        states[1] = new int[] {};
        
        
        int[] colors = new int[] {disableColor, Color.WHITE};
        Drawable drawable = getStateListDrawable(normalDrawable, states, colors);
        
        /*StateListDrawable listDrawable = new StateListDrawable();

        getTintDrawable(normalDrawable, Color.WHITE);

        Drawable disableDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme()).mutate();
        getTintDrawable(disableDrawable, disableColor);

        listDrawable.addState(new int[] {-android.R.attr.state_enabled}, disableDrawable);
        listDrawable.addState(new int[] {}, normalDrawable);*/

        item.setIcon(drawable);
    }

    /**
     * 更新菜单图标的颜色
     * @param color
     */
    private void updateBrushMenuColor(int color) {
        if (mBrushItem == null) {
            return;
        }
        Drawable drawable = mBrushItem.getIcon();
        if (drawable instanceof StateListDrawable) {    //Android5.1或者之上
            StateListDrawable listDrawable = (StateListDrawable) drawable;
            //给没有选择状态的图标着色
            tintNormalDrawable(listDrawable, color);
            //给选中的层级图标着色
            tintSelectedDrawable(listDrawable, color);
        } else if (drawable instanceof LayerDrawable) { //Android5.1或者之下

            Drawable normalDrawable = getResources().getDrawable(getDrawableRes(mPaintType));
            Drawable spinnerInkDrawable = getResources().getDrawable(R.drawable.ic_action_spinner_ink);
            
            int[] colors = new int[] {color, color};
            Drawable srcDrawable = getStateListDrawable(normalDrawable, colors);

            colors = new int[] {Color.WHITE, 0};
            Drawable bgDrawable = getStateListDrawable(spinnerInkDrawable, colors);

            LayerDrawable layerDrawable = layerDrawable(srcDrawable, bgDrawable);
            mBrushItem.setIcon(layerDrawable);
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
                
                int alpha = getRelAlpha(mPaintAlpha);

                int relColor = SystemUtil.calcColor(alpha, mPaintColor);

                Drawable icon = getBrushMenuIcon(resId, relColor);

                setupBrushMenuIcon(icon);
                
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

                int alpha = getRelAlpha(mPaintAlpha);

                //合成实际的颜色
                int relColor = SystemUtil.calcColor(alpha, color);

                updateBrushMenuColor(relColor);

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
                int alpha = getRelAlpha(progress);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ivStrokeAlpha.setImageAlpha(alpha);
                } else {
                    ivStrokeAlpha.setAlpha(alpha);
                }

                mPaintAlpha = progress;

                //合成实际的颜色
                int relColor = SystemUtil.calcColor(alpha, mPaintColor);
                
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
     * alpha的百分比，[0...100]
     * @param alpha alpha的百分比，[0...100]
     * @return
     */
    private int getRelAlpha(int alpha) {
        return (alpha * 255) / 100;//百分比转换成255级透明度
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

    private Drawable getStateDrawable(StateListDrawable listDrawable, int[] state) {
        //给默认的图标着色
        Drawable drawable = null;
        try {
            Method method = StateListDrawable.class.getDeclaredMethod("getStateDrawableIndex", int[].class);
            if (method != null) {
                method.setAccessible(true);
                int index = (int) method.invoke(listDrawable, state);
                method = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                if (method != null) {
                    method.setAccessible(true);

                    drawable = (Drawable) method.invoke(listDrawable, index);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "---tintNormalDrawable----error---" + e.getMessage());
            e.printStackTrace();
        }
        return drawable;
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
    
//    private Drawable getSelected

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
     * 保存涂鸦的图片到本地
     */
    private void savePaintImage() {
        PaintFragment paintFragment = getPaintFragment();
        if (!paintFragment.hasPaintContent()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        PaintFragment paintFragment = getPaintFragment();
                        paintFragment.savePaintImage(mAttachSpec);
                    }
                });
            }

            @Override
            public void onDenied(String permission) {
                //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
                if (ActivityCompat.shouldShowRequestPermissionRationale(HandWritingActivity.this,
                        permission)) {
                    SystemUtil.makeShortToast(R.string.paint_save_error_reason);
                } else {
                    SystemUtil.makeShortToast(R.string.paint_save_error);
                    Log.d(TAG, "---savePaintImage----permissions--onDenied----" + permission);
                    //进行权限请求
                    /*ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            EXTERNAL_STORAGE_REQ_CODE);*/
                }
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.d(TAG, "--HandWritingActivity----onRequestPermissionsResult---");
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
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
        tvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //清除屏幕
                PaintFragment paintFragment = getPaintFragment();
                paintFragment.erase();
            }
        });
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
    public void onSaveImageError(String reason) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                SystemUtil.makeShortToast(R.string.paint_save_error);
                setResult(RESULT_CANCELED);
                Log.d(TAG, "---savePaintImage----onSaveImageError------");
                finish();
            }
        });
        
    }

    @Override
    public void onSaveImageSuccess(final AttachSpec attachSpec) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String filePath = attachSpec.filePath;
                Uri uri = Uri.fromFile(new File(filePath));
                //清除该图片的内存缓存
                ImageUtil.clearMemoryCache(filePath);
                Intent intent = new Intent();
                intent.setData(uri);
                if (attachSpec.isEditMode()) {  //编辑已有的
                    intent.putExtra(Constants.ARG_CORE_OBJ, attachSpec);
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public void onSaveImageCancel() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
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
