package net.ibaixin.notes.richtext;

import android.content.Context;
import android.text.method.MovementMethod;
import android.text.style.ReplacementSpan;

import net.ibaixin.notes.widget.AttachSpan;

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
    String addSpan(String text, AttachSpan clickSpan, ReplacementSpan replaceSpan, final int selStart, final int selEnd);

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
