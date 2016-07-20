package net.ibaixin.notes.ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.PopupWindowCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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
    private int mPaintSize;
    //画笔的类型
    private int mPaintType;
    private int mPaintColor;
    
    //画笔数据记录的集合
    private List<PaintData> mPaintList = new ArrayList<>();
    
    //画笔的弹窗
    private PopupWindow mStrokePopupWindow;
    private PopupWindow mErasePopupWindow;
    
    @Override
    protected int getContentView() {
        return R.layout.activity_hand_writing;
    }

    @Override
    protected void initData() {
        initPaintParams();

        attachContainer();
    }

    /**
     * 填充内容
     * @return
     */
    private PaintFragment attachContainer() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        Painter painter = new Painter(mPaintSize, mPaintColor, mPaintType);

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

        mPaintSize = resources.getDimensionPixelSize(R.dimen.stroke_base_size);

        //默认画笔类型
        mPaintType = PaintRecord.PAINT_TYPE_DRAW;

        PaintData paintData = new PaintData();
        mPaintList.add(paintData);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.paint_edit, menu);
        
        MenuItem penItem = menu.findItem(R.id.action_pen);

        setMenuTint(penItem, Color.WHITE);

        int disableColor = SystemUtil.adjustAlpha(Color.WHITE, Constants.MENU_ITEM_COLOR_ALPHA);
        
        MenuItem undoItem = menu.findItem(R.id.action_undo);
        setMenuTint(undoItem, disableColor);

        MenuItem redoItem = menu.findItem(R.id.action_redo);
        setMenuTint(redoItem, disableColor);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        View view = getToolBarMenuView(item.getItemId());
        
        if (view != null) {
            
            switch (item.getItemId()) {
                case R.id.action_pen:
                    showStrokeWindow(view);
                    break;
                case R.id.action_eraser:
                    showEraseWindow(view);
                    break;
            }
            
        }
        
        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示画笔的弹窗
     * @param author
     */
    private void showStrokeWindow(View author) {

        if (mStrokePopupWindow == null) {
            mStrokePopupWindow = createPopuWindow(R.layout.popup_brush_panel);
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
     * 创建弹窗
     * @param layoutId
     * @return
     */
    private PopupWindow createPopuWindow(int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootPanel = inflater.inflate(layoutId, null);

        Resources resources = getResources();
        
        int checkedColor = Color.BLACK;
        int normalColor = SystemUtil.adjustAlpha(checkedColor, 0.3f);

        RadioGroup strokTypeGroup = (RadioGroup) rootPanel.findViewById(R.id.stroke_type_group);
        RadioButton strokeDraw = (RadioButton) rootPanel.findViewById(R.id.stroke_draw);
        RadioButton strokeLine = (RadioButton) rootPanel.findViewById(R.id.stroke_line);
        RadioButton strokeCircle = (RadioButton) rootPanel.findViewById(R.id.stroke_circle);
        RadioButton strokeRectangle = (RadioButton) rootPanel.findViewById(R.id.stroke_rectangle);
        RadioButton strokeText = (RadioButton) rootPanel.findViewById(R.id.stroke_text);
        
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
     * 创建橡皮檫的弹窗
     * @param layoutId
     * @return
     */
    private PopupWindow createEraseWindow(int layoutId) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View rootPanel = inflater.inflate(layoutId, null);

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
