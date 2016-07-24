package net.ibaixin.notes.paint.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
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

    private Context mContext;
    
    //当前的画笔数据
    private PaintData mCurPaintData;
    
    //当前的画笔记录
    private PaintRecord mCurPaintRecord;

    //画笔默认的尺寸
    private float mStrokeSize = PaintRecord.DEFAULT_STROKE_SIZE;
    private float mEraserSize = PaintRecord.DEFAULT_ERASER_SIZE;
    //画笔实际的颜色，实际颜色有画笔颜色和画笔的alpha合成的
    private int mStrokeRealColor = Color.BLACK;//画笔实际颜色
    private int mStrokeColor = Color.BLACK;//画笔颜色
    private int mStrokeAlpha = 255;//画笔透明度

    private int mPaintType = PaintRecord.PAINT_TYPE_DRAW;
    
    //添加文字的回调
    private TextWindowCallback mTextWindowCallback;
    //绘制的回调
    private OnDrawChangedListener mOnDrawChangedListener;
    
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
     * 获取画笔的类型
     * @return
     */
    public int getPaintType() {
        return mPaintType;
    }

    /**
     * 设置画笔的类型
     * @param paintType
     */
    public void setPaintType(int paintType) {
        this.mPaintType = paintType;
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

    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.mTextWindowCallback = textWindowCallback;
    }

    public void setOnDrawChangedListener(OnDrawChangedListener onDrawChangedListener) {
        this.mOnDrawChangedListener = onDrawChangedListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mCurX = event.getX();
        mCurY = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp(event);
                break;
        }
        invalidate();
        mPreX = mCurX;
        mPreY = mCurY;
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawRecord(canvas);
        if (mOnDrawChangedListener != null) {
            mOnDrawChangedListener.onDrawChanged();
        }
    }

    /**
     * 画
     * @param canvas
     */
    private void drawRecord(Canvas canvas) {
        //设置背景
//        canvas.drawColor(Color.WHITE);
        if (mCurPaintData != null) {
            for (PaintRecord record : mCurPaintData.mUndoList) {

                int type = record.type;
                switch (type) {
                    case PaintRecord.PAINT_TYPE_ERASE:
                    case PaintRecord.PAINT_TYPE_DRAW:
                    case PaintRecord.PAINT_TYPE_LINE:
                        canvas.drawPath(record.path, record.paint);
                        break;
                    case PaintRecord.PAINT_TYPE_CIRCLE:
                        canvas.drawOval(record.rect, record.paint);
                        break;
                    case PaintRecord.PAINT_TYPE_RECTANGLE:
                        canvas.drawRect(record.rect, record.paint);
                        break;
                    case PaintRecord.PAINT_TYPE_TEXT:
                        if (record.text != null && record.textWidth > 0) {
                            StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
//                            canvas.save();
                            canvas.translate(record.textOffX, record.textOffY);
                            layout.draw(canvas);
//                            canvas.restore();
                            canvas.translate(-record.textOffX, -record.textOffY);
                        }
                        break;
                    case PaintRecord.PAINT_TYPE_CLEAR_ALL:  //清屏
                        Paint paint = record.paint;
                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                        canvas.drawPaint(paint);

                        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
                        canvas.drawColor(Color.WHITE);

                        break;
                }
            }
        }
    }

    /**
     * 初始按下的操作
     * @param event
     */
    private void onTouchDown(MotionEvent event) {
        mDownX = event.getX();
        mDownY = event.getY();
        
        mCurPaintRecord = new PaintRecord(mPaintType);
        switch (mPaintType) {
            case PaintRecord.PAINT_TYPE_ERASE:  //橡皮檫
                mPaint.setXfermode(null);
                mPath = new Path();
                mPath.moveTo(mDownX, mDownY);
//                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));//擦除模式
                mPaint.setColor(Color.WHITE);
                mPaint.setStrokeWidth(mEraserSize);
                
                //设置当前的画笔
                mCurPaintRecord.paint = new Paint(mPaint);
                mCurPaintRecord.path = mPath;
                break;
            case PaintRecord.PAINT_TYPE_DRAW:   //画笔
            case PaintRecord.PAINT_TYPE_LINE:   //画直线
                mPath = new Path();
                mPath.moveTo(mDownX, mDownY);
                
                mPaint.setXfermode(null);
                mPaint.setColor(mStrokeRealColor);
                mPaint.setStrokeWidth(mStrokeSize);

                //设置当前的画笔
                mCurPaintRecord.paint = new Paint(mPaint);
                mCurPaintRecord.path = mPath;
                break;
            case PaintRecord.PAINT_TYPE_CIRCLE: //画圆
            case PaintRecord.PAINT_TYPE_RECTANGLE:    //画矩形
                RectF rect = new RectF(mDownX, mDownY, mDownX, mDownY);

                mPaint.setXfermode(null);
                mPaint.setColor(mStrokeRealColor);
                mPaint.setStrokeWidth(mStrokeSize);
                
                mCurPaintRecord.rect = rect;
                mCurPaintRecord.paint = new Paint(mPaint);
                break;
            case PaintRecord.PAINT_TYPE_TEXT:   //文字
                mCurPaintRecord.textOffX = (int) mDownX;
                mCurPaintRecord.textOffY = (int) mDownY;

                TextPaint textPaint = new TextPaint();
                textPaint.setColor(mStrokeRealColor);
                mCurPaintRecord.textPaint = textPaint;

                if (mTextWindowCallback != null) {
                    mTextWindowCallback.onText(this, mCurPaintRecord);
                }
                return;
        }
        //将该操作步骤添加到撤销的栈中
        mCurPaintData.mUndoList.add(mCurPaintRecord);
    }

    /**
     * 处理移动操作
     * @param event
     */
    public void onTouchMove(MotionEvent event) {
        switch (mPaintType) {
            case PaintRecord.PAINT_TYPE_ERASE:  //橡皮檫
            case PaintRecord.PAINT_TYPE_DRAW:   //
                //贝塞尔曲线
                mPath.quadTo(mPreX, mPreY, (mPreX + mCurX) / 2, (mPreY + mCurY) / 2);
                break;
            case PaintRecord.PAINT_TYPE_LINE:   //画线
                mPath.reset();
                mPath.moveTo(mDownX, mDownY);
                mPath.lineTo(mCurX, mCurY);
                break;
            case PaintRecord.PAINT_TYPE_CIRCLE:
            case PaintRecord.PAINT_TYPE_RECTANGLE:
                mCurPaintRecord.rect.set(mDownX < mCurX ? mDownX : mCurX, mDownY < mCurY ? mDownY : mCurY, mDownX > mCurX ? mDownX : mCurX, mDownY > mCurY ? mDownY : mCurY);
                break;
        }
    }

    /**
     * 处理抬起状态
     * @param event
     */
    private void onTouchUp(MotionEvent event) {
        if (mCurPaintData != null && mCurPaintData.mRedoList.size() > 0) {
            mCurPaintData.mRedoList.clear();
        }
    }

    /**
     * 设置画笔的颜色alpha
     * @param alpha Alpha component [0..255] of the color
     */
    public void setPaintAlpha(int alpha) {
        mStrokeAlpha = alpha;
//        mStrokeRealColor = SystemUtil.calculColor(alpha, mStrokeColor);
//        mPaint.setColor(mStrokeRealColor);
    }

    /**
     * 设置画笔的颜色
     * @param color
     */
    public void setPaintColor(int color) {
        mStrokeColor = color;
//        mStrokeRealColor = SystemUtil.calculColor(mStrokeAlpha, color);
//        mPaint.setColor(mStrokeRealColor);
    }

    /**
     * 设置画笔的实际颜色
     * @param color
     */
    public void setPaintRealColor(int color) {
        mStrokeRealColor = color;
    }

    /**
     * 设置画笔的尺寸
     * @param size
     */
    public void setPaintSize(int size) {
        setPaintSize(size, PaintRecord.PAINT_TYPE_DRAW);
    }

    /**
     * 设置画笔或者橡皮檫的尺寸
     * @param size
     * @param paintType 画笔或者橡皮檫
     */
    public void setPaintSize(int size, int paintType) {
        switch (paintType) {
            case PaintRecord.PAINT_TYPE_ERASE:  //橡皮檫
                mEraserSize = size;
                break;
            case PaintRecord.PAINT_TYPE_DRAW:   //画笔
                mStrokeSize = size;
                break;
        }
    }

    /**
     * 添加一笔操作
     * @param record
     */
    public void addRecord(PaintRecord record) {
        if (mCurPaintData != null) {
            mCurPaintData.mUndoList.add(record);
            invalidate();
        }
    }

    /**
     * 回退
     */
    public void undo() {
        if (mCurPaintData != null && mCurPaintData.mUndoList.size() > 0) {
            mCurPaintData.mRedoList.add(mCurPaintData.mUndoList.get(mCurPaintData.mUndoList.size() - 1));
            mCurPaintData.mUndoList.remove(mCurPaintData.mUndoList.size() - 1);
            invalidate();
        }
    }

    /**
     * 前进
     */
    public void redo() {
        if (mCurPaintData != null && mCurPaintData.mRedoList.size() > 0) {
            mCurPaintData.mUndoList.add(mCurPaintData.mRedoList.get(mCurPaintData.mRedoList.size() - 1));
            mCurPaintData.mRedoList.remove(mCurPaintData.mRedoList.size() - 1);
            invalidate();
        }
    }

    /**
     * 获取前进的数量
     * @return
     */
    public int getRedoCount() {
        int count = 0;
        if (mCurPaintData != null && mCurPaintData.mRedoList.size() > 0) {
            count = mCurPaintData.mRedoList.size();
        }
        return count;
    }
    
    public int getUndoCount() {
        int count = 0;
        if (mCurPaintData != null && mCurPaintData.mUndoList.size() > 0) {
            count = mCurPaintData.mUndoList.size();
        }
        return count;
    }
    
    /**
     * 回收，清屏
     */
    public void erase() {
        PaintRecord record = new PaintRecord(PaintRecord.PAINT_TYPE_CLEAR_ALL);
        record.paint = new Paint();
        RectF rect = new RectF(0, 0, getWidth(), getHeight());
        addRecord(record);
    }

    /**
     * 添加文字的接口
     */
    public interface TextWindowCallback {
        /**
         * 添加文字之前的回调
         * @param view
         * @param paintRecord
         */
        void onText(View view, PaintRecord paintRecord);
    }

    /**
     * 绘制的回调
     */
    public interface OnDrawChangedListener {
        void onDrawChanged();
    }
}
