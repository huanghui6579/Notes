package net.ibaixin.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.*;
import android.widget.EditText;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.ibaixin.notes.listener.AttachAddCompleteListener;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.richtext.AttachSpec;
import net.ibaixin.notes.richtext.NoteRichSpan;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.ImageUtil;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

import java.io.File;

/**
 * 笔记的编辑控件
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/13 14:34
 */
public class NoteEditText extends EditText implements NoteRichSpan {

    private static final String TAG = "NoteEditText";

    protected SelectionChangedListener mSelectionChangedListener;

    public void setOnSelectionChangedListener(SelectionChangedListener selectionChangedListener) {
        this.mSelectionChangedListener = selectionChangedListener;
    }

    public NoteEditText(Context context) {
        super(context);
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
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
     * @param filePath 图片的本地全路径
     */
    public void addImage(final String filePath, final Attach attach, final AttachAddCompleteListener listener) {
        addImage(filePath, Attach.IMAGE, attach, listener);
    }
    
    /**
     * 添加图片
     * @param filePath 图片的本地全路径
     */
    public void addImage(final String filePath, final int attachType, final Attach attach, final AttachAddCompleteListener listener) {
        ImageSize imageSize = getImageSize(attachType);
        ImageUtil.generateThumbImageAsync(filePath, imageSize, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                ImageSpan imageSpan = new ImageSpan(getContext(), loadedImage);
                String fileId = null;
                if (attach == null) {
                    fileId = SystemUtil.generateAttachSid(attachType);
                } else {
                    fileId = attach.getSId();
                }
                AttachSpan attachSpan = new AttachSpan();
                attachSpan.setAttachId(fileId);
                attachSpan.setAttachType(attachType);
                attachSpan.setFilePath(filePath);
                String text = "[" + Constants.ATTACH_PREFIX + "=" + fileId + "]";

                addSpan(text, attachSpan, imageSpan);
                
                if (listener != null) {
                    Attach att = null;
                    if (attach == null) {
                        File file = new File(filePath);
                        att = new Attach();
                        att.setSId(fileId);
                        att.setType(attachType);
                        att.setLocalPath(filePath);
                        att.setFilename(file.getName());
                        att.setSize(file.length());
                    }
                    listener.onAddComplete(filePath, text, att);
                }
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
    public void addAttach(Attach attach, final AttachAddCompleteListener listener) {
        
        FileSpan fileSpan = new FileSpan(getContext(), attach, getWidth());
        
        String fileId = attach.getSId();
        String filePath = attach.getLocalPath();
        
        AttachSpan attachSpan = new AttachSpan();
        attachSpan.setAttachId(fileId);
        attachSpan.setAttachType(attach.getType());
        attachSpan.setFilePath(filePath);

        String text = "[" + Constants.ATTACH_PREFIX + "=" + fileId + "]";

        addSpan(text, attachSpan, fileSpan);

        if (listener != null) {
            listener.onAddComplete(filePath, text, attach);
        }
    }

    /**
     * 添加Span
     * @param text 原文本
     * @param clickSpan 可点击的Span              
     * @param replaceSpan 附件显示的span
     */
    private String addSpan(String text, AttachSpan clickSpan, ReplacementSpan replaceSpan) {
        final int selStart = getSelectionStart();
        final int selEnd = getSelectionEnd();
        return addSpan(text, clickSpan, replaceSpan, selStart, selEnd);
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
    public String addSpan(String text, AttachSpan clickSpan, ReplacementSpan replaceSpan, final int selStart, final int selEnd) {

        /*AttachSpan attachSpan = new AttachSpan();
        attachSpan.setAttachId(fileId);
        attachSpan.setAttachType(attach.getType());
        attachSpan.setFilePath(filePath);*/

        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(text);
        builder.setSpan(replaceSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        builder.setSpan(clickSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        post(new Runnable() {
            @Override
            public void run() {
                try {
                    Editable editable = getEditableText();
                    if (selStart < 0 || getText() == null || selStart >= getText().length()) {
                        editable.append(builder).append(Constants.TAG_ENTER);
                    } else {
                        editable.replace(selStart, selEnd, builder);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "---note---edit--addSpan---error--" + e.getMessage());
                }
            }
        });
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
        int height = getHeight();
        if (width == 0 || height == 0) {
            //计算尺寸
            int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            measure(w, h);
            width = getMeasuredWidth();
//            height = getMeasuredHeight();
            height = width;
        }
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
        setMovementMethod(movement);
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

            addSpan(text, attachSpan, imageSpan, selStart, selEnd);
            
            if (listener != null) {
                
                Attach attach = new Attach();
                attach.setNoteId(attachSpec.noteSid);
                attach.setSId(attachSpec.sid);
                attach.setLocalPath(attachSpec.filePath);
                attach.setType(attachSpec.attachType);
                
                listener.onAddComplete(null, null, attach);
            }
        }
    }

    /**
     * 光标位置变化的监听
     * @author tiger
     * @update 2016/3/13 14:36
     * @version 1.0.0
     */
    public interface SelectionChangedListener {
        public void onSelectionChanged(int selStart, int selEnd);
    }
    
}
