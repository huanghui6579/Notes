package net.ibaixin.notes.widget;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import net.ibaixin.notes.util.log.Log;

/**
 * @author huanghui1
 * @update 2016/7/4 16:13
 * @version: 0.0.1
 */
public class NoteLinkMovementMethod extends LinkMovementMethod {
    private static final String TAG = "NoteLinkMovementMethod";

    public static MovementMethod getInstance() {
        if (sInstance == null)
            sInstance = new NoteLinkMovementMethod();

        return sInstance;
    }

    private static NoteLinkMovementMethod sInstance;

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

            if (link != null && link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    link[0].onClick(widget);
                    Log.d(TAG, "---MotionEvent.ACTION_UP--onClick---");
                    return true;
                } else {
                    Log.d(TAG, "---MotionEvent.ACTION_DOWN--setSelection---");
                    Selection.setSelection(buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]));
                }
                return false;
            } else {
//                Selection.removeSelection(buffer);
            }
        }
        return false;
    }
}
