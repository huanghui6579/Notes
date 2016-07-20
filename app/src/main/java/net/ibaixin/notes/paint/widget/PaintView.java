package net.ibaixin.notes.paint.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import net.ibaixin.notes.paint.PaintData;
import net.ibaixin.notes.paint.PaintRecord;

/**
 * @author huanghui1
 * @update 2016/7/20 20:53
 * @version: 0.0.1
 */
public class PaintView extends View implements View.OnTouchListener {
    private static final String TAG = "PaintView";

    public static final int DEFAULT_STROKE_SIZE = 7;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;

    private Context mContext;
    
    //当前的画笔数据
    private PaintData mCurPaintData;
    
    //当前的画笔记录
    private PaintRecord mCurPaintRecord;

    //画笔默认的尺寸
    private float mStrokeSize = DEFAULT_STROKE_SIZE;
    private float mEraserSize = DEFAULT_ERASER_SIZE;
    //画笔实际的颜色，实际颜色有画笔颜色和画笔的alpha合成的
    private int mStrokeRealColor = Color.BLACK;//画笔实际颜色
    private int mStrokeColor = Color.BLACK;//画笔颜色
    private int mStrokeAlpha = 255;//画笔透明度

    private int mPaintType = PaintRecord.PAINT_TYPE_DRAW;
    
    //画笔
    private Paint mPaint;
    private Path mPath;
    
    private int mWidth;
    private int mHeight;

    //触摸焦点的屏幕位置
    private float mDownX, mDownY, mPreX, mPreY, mCurX, mCurY;
    
    public PaintView(Context context) {
        this(context, null);
    }

    public PaintView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaintView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;

        //初始化参数
        initParams();
        
        if (isFocusable()) {
            setOnTouchListener(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    public void setPaintData(PaintData paintData) {
        this.mCurPaintData = paintData;
    }

    /**
     * 初始化参数
     */
    private void initParams() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);//是否使用抗锯齿功能,会消耗较大资源，绘制图形速度会变慢
        mPaint.setDither(true);// 设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
        mPaint.setColor(mStrokeRealColor);
        mPaint.setStyle(Paint.Style.STROKE);//设置画笔的样式
        mPaint.setStrokeJoin(Paint.Join.ROUND);//设置绘制时各图形的结合方式，如平滑效果等
        mPaint.setStrokeCap(Paint.Cap.ROUND);//当画笔样式为STROKE或FILL_OR_STROKE时，设置笔刷的图形样式，如圆形样式    Cap.ROUND,或方形样式Cap.SQUARE
        mPaint.setStrokeWidth(mStrokeSize);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mCurX = event.getX();
        mCurY = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
        }
        return false;
    }
    
    private void onTouchDown(MotionEvent event) {
        mDownX = event.getX();
        mDownY = event.getY();
        
        if (mCurPaintData != null) {
            mCurPaintData.mRedoStack.clear();
        }
        mCurPaintRecord = new PaintRecord(mPaintType);
        switch (mPaintType) {
            case PaintRecord.PAINT_TYPE_ERASE:  //橡皮檫
                mPath = new Path();
                mPath.moveTo(mDownX, mDownY);
                
                mPaint.setColor(Color.WHITE);
                mPaint.setStrokeWidth(mEraserSize);
                break;
        }
    }
}
