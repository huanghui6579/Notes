package com.yunxinlink.notes.richtext;

import android.text.style.ReplacementSpan;

import com.yunxinlink.notes.listener.OnAddSpanCompleteListener;
import com.yunxinlink.notes.widget.AttachSpan;

/**
 * 富文本的span信息
 * @author huanghui1
 * @update 2016/8/17 9:32
 * @version: 1.0.0
 */
public class SpanInfo {
    public CharSequence text;
    public AttachSpan clickSpan;
    public ReplacementSpan replaceSpan;
    public int selStart;
    public int selEnd;
    public OnAddSpanCompleteListener listener;

    public SpanInfo(CharSequence text, AttachSpan clickSpan, ReplacementSpan replaceSpan, int selStart, int selEnd, OnAddSpanCompleteListener listener) {
        this.text = text;
        this.clickSpan = clickSpan;
        this.replaceSpan = replaceSpan;
        this.selStart = selStart;
        this.selEnd = selEnd;
        this.listener = listener;
    }
}