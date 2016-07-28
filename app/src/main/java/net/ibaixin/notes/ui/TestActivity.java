package net.ibaixin.notes.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.widget.ImageButton;
import android.widget.ImageView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

import java.lang.reflect.Method;

public class TestActivity extends BaseActivity {

    @Override
    protected int getContentView() {
        return R.layout.activity_test;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {
        ImageView imageView = (ImageView) findViewById(R.id.icon);

        Drawable drawable = getResources().getDrawable(R.drawable.ic_action_trash);
        
        int color = getResources().getColor(R.color.colorPrimary);
        
        getTintDrawable(drawable, color);

        imageView.setImageDrawable(drawable);



        int disableColor = SystemUtil.adjustAlpha(Color.WHITE, Constants.MENU_ITEM_COLOR_ALPHA);
        ImageButton btnDo = (ImageButton) findViewById(R.id.btn_do);
        
        int resId = R.drawable.ic_action_undo;
        
        tintDoMenuIcon(btnDo, resId, disableColor);
    }

    private void tintDoMenuIcon(ImageButton imageButton, int resId, int disableColor) {

        Drawable normalDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme());

        Drawable spinnerInkDrawable = getResources().getDrawable(R.drawable.ic_action_spinner_ink);

        int[] colors = new int[] {Color.RED, Color.BLACK};

        Drawable srcDrawable = getStateListDrawable(normalDrawable, colors);

        int[] bgColors = new int[] {Color.WHITE, Color.TRANSPARENT};
        Drawable bgDrawable = getStateListDrawable(spinnerInkDrawable, bgColors);
        

        //合成的选中后的图标
        Drawable[] drawables = new Drawable[2];
        drawables[0] = srcDrawable;
        drawables[1] = bgDrawable;
        LayerDrawable layerDrawable = new LayerDrawable(drawables);

//        Drawable normalDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme());
//        getTintDrawable(normalDrawable, Color.WHITE);
//
//        Drawable disableDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_brush, getTheme());
//        getTintDrawable(disableDrawable, Color.RED);
//
//        listDrawable.addState(new int[] {android.R.attr.state_pressed}, disableDrawable);
//        listDrawable.addState(new int[] {}, normalDrawable);

        /*Drawable currentDrawable = getStateDrawable(listDrawable, StateSet.NOTHING);

        getTintDrawable(currentDrawable, Color.WHITE);

        Drawable pressedDrawable = getStateDrawable(listDrawable, new int[] {android.R.attr.state_pressed});
        if (pressedDrawable != null) {
            getTintDrawable(pressedDrawable, Color.RED);
        }*/

        imageButton.setImageDrawable(layerDrawable);
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
    
    protected Drawable getStateListDrawable(Drawable drawable, int[] colors) {
        int[][] states = new int[2][];


        states[0] = new int[] { android.R.attr.state_pressed};

        states[1] = new int[] {};


        ColorStateList colorList = new ColorStateList(states, colors);

        StateListDrawable stateListDrawable = new StateListDrawable();



        stateListDrawable.addState(states[0], drawable);//注意顺序

        stateListDrawable.addState(states[1], drawable);

        Drawable.ConstantState state = stateListDrawable.getConstantState();

//        normalDrawable = DrawableCompat.wrap(state == null ? stateListDrawable : drawable).mutate();
        Drawable srcDrawable = DrawableCompat.wrap(state == null ? stateListDrawable : state.newDrawable()).mutate();

        DrawableCompat.setTintList(srcDrawable, colorList);
        return srcDrawable;
    }
}
