package com.yunxinlink.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.socks.library.KLog;
import com.yunxinlink.notes.listener.AttachAddCompleteListener;
import com.yunxinlink.notes.listener.OnAddSpanCompleteListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.richtext.AttachSpec;
import com.yunxinlink.notes.richtext.NoteRichSpan;
import com.yunxinlink.notes.richtext.SpanInfo;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/7/17 11:00
 */
public class NoteTextView extends TextView implements NoteRichSpan {
    private static final java.lang.String TAG = "NoteTextView";
    
    private Handler mHandler = new MyHandler();

    public NoteTextView(Context context) {
        super(context);
    }

    public NoteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NoteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        final boolean touchIsFinished = (action == MotionEvent.ACTION_UP) && isFocused();
        KLog.d(TAG, "---touchIsFinished--" + touchIsFinished);
        MovementMethod mMovement = getMovementMethod();
        CharSequence text = getText();
        if (touchIsFinished && text != null && (mMovement != null && mMovement instanceof LinkMovementMethod) && isEnabled()
                && text instanceof Spannable && getLayout() != null) {
            boolean handled = mMovement.onTouchEvent(this, (Spannable) text, event);
            if (handled) {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public CharSequence getTextContent() {
        return getText();
    }

    @Override
    public Context getNoteContext() {
        return getContext();
    }

    @Override
    public CharSequence addSpan(final CharSequence text, final AttachSpan clickSpan, final ReplacementSpan replaceSpan, final int selStart, final int selEnd, final OnAddSpanCompleteListener listener) {

        Message msg = mHandler.obtainMessage();
        msg.what = MSG_ADD_SPAN;

        msg.obj = new SpanInfo(text, clickSpan, replaceSpan, selStart, selEnd, listener);
        mHandler.sendMessage(msg);

        if (listener != null) {
            listener.onAddSpanComplete();
        }
        return text;
    }

    @Override
    public void showImage(AttachSpec attachSpec, AttachAddCompleteListener listener) {
        ImageSize imageSize = getImageSize(attachSpec.attachType);

        ImageUtil.generateThumbImageAsync(attachSpec.filePath, imageSize, new SimpleImageLoadingListenerImpl(attachSpec, listener));
    }

    /**
     * 根据附件的类型获取对应的显示尺寸大小
     * @param attachType 附件类型
     * @return
     */
    private ImageSize getImageSize(int attachType) {
        ImageSize imageSize = null;
        if (attachType == Attach.PAINT) {    //绘画
            int width = getWidth();
//            int height = getHeight();
            int height = width;
            imageSize = new ImageSize(width, height);
        }
        return imageSize;
    }

    @Override
    public int[] getSize() {
        int width = getWidth();
        if (width == 0) {
            width = SystemUtil.getScreenWidth(getContext());
        }
//        int height = getHeight();
        int height = width;
        int[] size = new int[2];
        size[0] = width;
        size[1] = height;
        return size;
    }

    @Override
    public void setTextContent(CharSequence text) {
        setText(text);
    }

    @Override
    public void setTextMovementMethod(MovementMethod movement) {
        KLog.d(TAG, "----setTextMovementMethod----");
        setMovementMethod(movement);
    }

    @Override
    public TextView getOriginalView() {
        return this;
    }

    /**
     * 图片加载完毕的回调
     */
    class SimpleImageLoadingListenerImpl extends SimpleImageLoadingListener {
        private AttachSpec attachSpec;
        private AttachAddCompleteListener listener;
        
        public SimpleImageLoadingListenerImpl(AttachSpec attachSpec, AttachAddCompleteListener listener) {
            this.attachSpec = attachSpec;
            this.listener = listener;
        }
        
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            final int selStart = attachSpec.start;
            final int selEnd = attachSpec.end;

            final String text = attachSpec.text.toString();

            final AttachSpan attachSpan = new AttachSpan();
            attachSpan.setAttachId(attachSpec.sid);
            attachSpan.setAttachType(attachSpec.attachType);
            attachSpan.setFilePath(attachSpec.filePath);
            attachSpan.setText(attachSpec.text);
            attachSpan.setSelStart(selStart);
            attachSpan.setSelEnd(selEnd);
            attachSpan.setNoteSid(attachSpec.noteSid);
            attachSpan.setMimeType(attachSpec.mimeType);

            ImageSpan imageSpan = new ImageSpan(getContext(), loadedImage);

            addSpan(text, attachSpan, imageSpan, selStart, selEnd, new OnAddSpanCompleteListener() {
                @Override
                public void onAddSpanComplete() {
                    if (listener != null) {

                        Attach attach = new Attach();
                        attach.setNoteId(attachSpec.noteSid);
                        attach.setSId(attachSpec.sid);
                        attach.setLocalPath(attachSpec.filePath);
                        attach.setType(attachSpec.attachType);

                        listener.onAddComplete(attachSpec.filePath, null, attach);
                    }
                }
            });
            
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD_SPAN:  //添加span
                    SpanInfo spanInfo = (SpanInfo) msg.obj;
                    if (spanInfo == null) {
                        return;
                    }
                    try {

                        int selStart = spanInfo.selStart;
                        int selEnd = spanInfo.selEnd;
                        ReplacementSpan replaceSpan = spanInfo.replaceSpan;
                        CharSequence text = getText();
                        ClickableSpan clickSpan = spanInfo.clickSpan;
                        
                        SpannableString spannableString = null;
                        if (text instanceof SpannableString) {
                            spannableString = (SpannableString) text;
                        } else {
                            spannableString = new SpannableString(text);
                        }
                        spannableString.setSpan(replaceSpan, selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannableString.setSpan(clickSpan, selStart, selEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        setText(spannableString);

                    } catch (Exception e) {
                        KLog.e(TAG, "--TextView---note---edit--addSpane---error--" + e.getMessage());
                    }
                    break;
            }
        }
    }
}