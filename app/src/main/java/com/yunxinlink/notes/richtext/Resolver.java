package com.yunxinlink.notes.richtext;

import android.os.Handler;
import android.util.SparseArray;

import com.yunxinlink.notes.listener.RichTextClickListener;

/**
 * Interface definition for a resolver that resolve provided data to your rich text.
 *
 * @author Bingding.
 *
 */
public interface Resolver {

    /**
     * Resolve your rich text here.
     *
     * @param richSpan the textView display rich text;
     * @param text the content of TextView;
     * @param extra extra data if exist；
     * @param listener Callback if need;
     */
    void resolve(NoteRichSpan richSpan, CharSequence text, SparseArray<Object> extra, RichTextClickListener listener);

    /**
     * set handler
     * @param handler
     */
    void setHandler(Handler handler);
}
