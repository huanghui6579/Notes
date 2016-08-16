package com.yunxinlink.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.socks.library.KLog;
import com.yunxinlink.notes.listener.AttachAddCompleteListener;
import com.yunxinlink.notes.listener.OnAddSpanCompleteListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.richtext.AttachSpec;
import com.yunxinlink.notes.richtext.NoteRichSpan;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.log.Log;

/**
 * 笔记的编辑控件
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/13 14:34
 */
public class NoteEditText extends EditText implements NoteRichSpan {

    private static final String TAG = "NoteEditText";
    
    private static final int MSG_ADD_SPAN = 1;

    protected SelectionChangedListener mSelectionChangedListener;
    
    private Handler mHandler = new MyHandler();
    
    public void setOnSelectionChangedListener(SelectionChangedListener selectionChangedListener) {
        this.mSelectionChangedListener = selectionChangedListener;
    }

    public NoteEditText(Context context) {
        this(context, null);
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        if (mSelectionChangedListener != null) {
            mSelectionChangedListener.onSelectionChanged(selStart, selEnd);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        final boolean touchIsFinished = (action == MotionEvent.ACTION_UP) && isFocused();
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

    /**
     * 添加图片
     * @param attach 图片
     */
    private void addImage(final Attach attach, final AttachAddCompleteListener listener) {
        final AttachSpan attachSpan = getAttachSpan(attach);
        ImageSize imageSize = getImageSize(attachSpan.getAttachType());
        final String filePath = attach.getLocalPath();
        ImageUtil.generateThumbImageAsync(filePath, imageSize, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                ImageSpan imageSpan = new ImageSpan(getContext(), loadedImage);
                final CharSequence text = attachSpan.getText();
                addSpan(text, attachSpan, imageSpan, new OnAddSpanCompleteListener() {
                    @Override
                    public void onAddSpanComplete() {
                        if (listener != null) {
                            listener.onAddComplete(filePath, text, attach);
                        }
                    }
                });
                
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                if (listener != null) {
                    listener.onAddFailed(imageUri, failReason);
                }
            }
        });
    }

    /**
     * 添加附件
     * @param attach
     * @param listener
     */
    public void addAttach(final Attach attach, final AttachAddCompleteListener listener) {
        int attachType = attach.getType();
        if (attachType == Attach.IMAGE || attachType == Attach.PAINT) { //图片
            addImage(attach, listener);
            return;
        }

        final String filePath = attach.getLocalPath();
        FileSpan fileSpan = new FileSpan(getContext(), attach, getSize()[0]);
        
        AttachSpan attachSpan = getAttachSpan(attach);
        final CharSequence text = attachSpan.getText();
        KLog.d(TAG, "---addAttach---attach-----" + attach + "---text--" + text);
        addSpan(text, attachSpan, fileSpan, new OnAddSpanCompleteListener() {
            @Override
            public void onAddSpanComplete() {
                if (listener != null) {
                    listener.onAddComplete(filePath, text, attach);
                }
            }
        });
        
    }

    /**
     * 获取AttachSpan
     * @param attach 附件
     * @return
     */
    public AttachSpan getAttachSpan(Attach attach) {
        int attachType = attach.getType();
        String fileId = attach.getSId();
        String filePath = attach.getLocalPath();
        
        String text = "[" + Constants.ATTACH_PREFIX + "=" + fileId + "]";
        AttachSpan attachSpan = new AttachSpan();
        attachSpan.setAttachId(fileId);
        attachSpan.setAttachType(attachType);
        attachSpan.setFilePath(filePath);
        attachSpan.setMimeType(attach.getMimeType());
        attachSpan.setText(text);
        attachSpan.setNoteSid(attach.getNoteId());
        return attachSpan;
    }

    /**
     * 添加Span
     * @param text 原文本
     * @param clickSpan 可点击的Span              
     * @param replaceSpan 附件显示的span
     */
    private CharSequence addSpan(CharSequence text, AttachSpan clickSpan, ReplacementSpan replaceSpan, final OnAddSpanCompleteListener listener) {
        final int selStart = getSelectionStart();
        final int selEnd = getSelectionEnd();
        return addSpan(text, clickSpan, replaceSpan, selStart, selEnd, listener);
    }

    @Override
    public CharSequence getTextContent() {
        return getText();
    }

    @Override
    public Context getNoteContext() {
        return getContext();
    }

    /**
     * 添加Span
     * @param text 原文本
     * @param clickSpan 可点击的Span
     * @param replaceSpan 附件显示的span
     */
    @Override
    public CharSequence addSpan(CharSequence text, AttachSpan clickSpan, ReplacementSpan replaceSpan, final int selStart, final int selEnd, final OnAddSpanCompleteListener listener) {

        /*AttachSpan attachSpan = new AttachSpan();
        attachSpan.setAttachId(fileId);
        attachSpan.setAttachType(attach.getType());
        attachSpan.setFilePath(filePath);*/

//        builder.setSpan(clickSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        KLog.d(TAG, "-------addSpan---post--start---");
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_ADD_SPAN;
        
        SpanInfo spanInfo = new SpanInfo(text, clickSpan, replaceSpan, selStart, selEnd, listener);
        msg.obj = spanInfo;
        mHandler.sendMessage(msg);

        if (listener != null) {
            listener.onAddSpanComplete();
        }
        /*
        post(new Runnable() {
            @Override
            public void run() {
                try {
                    KLog.d(TAG, "-------addSpan---post--ing---");
                    Editable editable = getEditableText();
                    if (selStart < 0 || getText() == null || selStart >= getText().length()) {
                        builder.append(Constants.TAG_NEXT_LINE);
                        editable.append(builder);
                        KLog.d(TAG, "-----addSpan----append--");
                    } else {
                        editable.replace(selStart, selEnd, builder);
                        KLog.d(TAG, "-----addSpan----replace--");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "---note---edit--addSpan---error--" + e.getMessage());
                }
            }
        });*/
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
    public ImageSize getImageSize(int attachType) {
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
        
//        KLog.d(TAG, "---getSize--width-----" + mWidth);
        /*int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            //计算尺寸
            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            measure(w, h);
            width = getMeasuredWidth();
//            height = getMeasuredHeight();
            height = width;
            KLog.d(TAG, "---getSize--width-----" + width);
        }*/
        int[] size = new int[2];
        int width = getWidth();
        if (width == 0) {
            width = SystemUtil.getScreenWidth(getContext());
        }
        size[0] = width;
        size[1] = width;
        return size;
    }

    @Override
    public void setTextContent(CharSequence text) {
        setText(text);
    }

    @Override
    public void setTextMovementMethod(MovementMethod movement) {
        setMovementMethod(movement);
    }

    @Override
    public TextView getOriginalView() {
        return this;
    }

    /**
     * 显示附件的span
     * @param text
     * @param selStart
     * @param attach
     */
    public void showSpanAttach(CharSequence text, final int selStart, Attach attach) {
        showSpanAttach(text, selStart, attach, false);
    }

    /**
     * 显示附件的span
     * @param text
     * @param selStart
     * @param attach
     * @param inHander 是否需要在handler中执行
     */
    public void showSpanAttach(CharSequence text, final int selStart, Attach attach, boolean inHander) {
        FileSpan fileSpan = new FileSpan(getContext(), attach, getWidth());
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(text);
        builder.setSpan(fileSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Editable editable = getEditableText();
        try {
            if (inHander) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (selStart < 0 || getText() == null || selStart >= getText().length()) {
                            editable.append(builder).append(Constants.TAG_ENTER);
                        } else {
                            editable.insert(selStart, builder);
                        }
                    }
                });
            } else {
                if (selStart < 0 || getText() == null || selStart >= getText().length()) {
                    editable.append(builder);
                } else {
                    editable.insert(selStart, builder);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "-----resetAttach--error----" + e.getMessage());
        }
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

                        listener.onAddComplete(null, null, attach);
                    }
                }
            });
        }
    }

    /**
     * 光标位置变化的监听
     * @author tiger
     * @update 2016/3/13 14:36
     * @version 1.0.0
     */
    public interface SelectionChangedListener {
        void onSelectionChanged(int selStart, int selEnd);
    }
    
    class SpanInfo {
        CharSequence text;
        AttachSpan clickSpan;
        ReplacementSpan replaceSpan;
        int selStart;
        int selEnd;
        OnAddSpanCompleteListener listener;

        public SpanInfo(CharSequence text, AttachSpan clickSpan, ReplacementSpan replaceSpan, int selStart, int selEnd, OnAddSpanCompleteListener listener) {
            this.text = text;
            this.clickSpan = clickSpan;
            this.replaceSpan = replaceSpan;
            this.selStart = selStart;
            this.selEnd = selEnd;
            this.listener = listener;
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
                        CharSequence text = spanInfo.text;
                        ReplacementSpan replaceSpan = spanInfo.replaceSpan;
                        
                        SpannableStringBuilder builder = new SpannableStringBuilder();
                        builder.append(text);
                        builder.setSpan(replaceSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        
                        KLog.d(TAG, "-------addSpan---post--ing---");
                        Editable editable = getEditableText();
                        if (selStart < 0 || getText() == null || selStart >= getText().length()) {
                            builder.append(Constants.TAG_NEXT_LINE);
                            editable.append(builder);
                            KLog.d(TAG, "-----addSpan----append--");
                        } else {
                            editable.replace(selStart, selEnd, builder);
                            KLog.d(TAG, "-----addSpan----replace--");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "---note---edit--addSpan---error--" + e.getMessage());
                    }
                    break;
            }
        }
    }
}
