package com.yunxinlink.notes.richtext;

import android.content.Context;
import android.text.method.MovementMethod;
import android.text.style.ReplacementSpan;

import com.yunxinlink.notes.listener.AttachAddCompleteListener;
import com.yunxinlink.notes.listener.OnAddSpanCompleteListener;
import com.yunxinlink.notes.widget.AttachSpan;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/7/17 13:46
 */
public interface NoteRichSpan {

    /**
     * 获取文本内容
     * @return
     */
    CharSequence getTextContent();

    /**
     * 获取context
     * @return
     */
    Context getNoteContext();

    /**
     *  添加Span
     */
    CharSequence addSpan(CharSequence text, AttachSpan clickSpan, ReplacementSpan replaceSpan, final int selStart, final int selEnd, OnAddSpanCompleteListener listener);

    /**
     * 显示图片、绘画
     * @param attachSpec 附件对象
     * @param listener 回调
     */
    void showImage(AttachSpec attachSpec, AttachAddCompleteListener listener);

    /**
     * 获取控件的尺寸
     * @return
     */
    int[] getSize();

    /**
     * 设置文本
     * @param text
     */
    void setTextContent(CharSequence text);

    /**
     * 设置点击的处理器
     * @param movement
     */
    void setTextMovementMethod(MovementMethod movement);
}
