package net.ibaixin.notes.paint;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;

/**
 * 画的记录实体
 * @author huanghui1
 * @update 2016/7/20 19:33
 * @version: 0.0.1
 */
public class PaintRecord {
    /**
     * 橡皮檫类型
     */
    public static final int PAINT_TYPE_ERASE = 1;

    /**
     * 画笔类型
     */
    public static final int PAINT_TYPE_DRAW = 2;

    /**
     * 直线类型
     */
    public static final int PAINT_TYPE_LINE = 3;

    /**
     * 圆形类型
     */
    public static final int PAINT_TYPE_CIRCLE = 4;

    /**
     * 方形
     */
    public static final int PAINT_TYPE_RECTANGLE = 5;

    /**
     * 文字类型
     */
    public static final int PAINT_TYPE_TEXT = 6;
    
    public int type;    //记录的类型
    
    public int paint;   //笔
    
    public Path path;   //画笔的路径
    
    public PointF[] linePoints; //线
    
    public RectF rect;  //圆、矩形
    
    public String text; //文字
    
    public TextPaint textPaint; //文字的笔
    
    public int textOffX;
    public int textOffY;    //文字的位置
    public int textWidth;   //文字的宽度
    
    public PaintRecord(int type) {
        this.type = type;
    }
}
