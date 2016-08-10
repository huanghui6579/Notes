package com.yunxinlink.notes.listener;

import android.widget.TextView;

/**
 *    callback to be invoked when rich text is clicked
 */
public interface RichTextClickListener {
    void onRichTextClick(TextView v, CharSequence content);
}