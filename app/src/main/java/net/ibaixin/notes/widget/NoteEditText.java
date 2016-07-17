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
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.ibaixin.notes.listener.AttachAddCompleteListener;
import net.ibaixin.notes.model.Attach;
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
        Log.d(TAG, "---touchIsFinished--" + touchIsFinished);
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
        ImageUtil.generateThumbImageAsync(filePath, null, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                ImageSpan imageSpan = new ImageSpan(getContext(), loadedImage);
                String fileId = null;
                if (attach == null) {
                    fileId = SystemUtil.generateAttachSid();
                } else {
                    fileId = attach.getSId();
                }
                AttachSpan attachSpan = new AttachSpan();
                attachSpan.setAttachId(fileId);
                attachSpan.setAttachType(Attach.IMAGE);
                attachSpan.setFilePath(filePath);
                String text = "[" + Constants.ATTACH_PREFIX + "=" + fileId + "]";

                addSpan(text, attachSpan, imageSpan);
                
                if (listener != null) {
                    Attach att = null;
                    if (attach == null) {
                        File file = new File(filePath);
                        att = new Attach();
                        att.setSId(fileId);
                        att.setType(Attach.IMAGE);
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
        VoiceSpan voiceSpan = new VoiceSpan(getContext(), attach);
        
        String fileId = attach.getSId();
        String filePath = attach.getLocalPath();
        
        AttachSpan attachSpan = new AttachSpan();
        attachSpan.setAttachId(fileId);
        attachSpan.setAttachType(attach.getType());
        attachSpan.setFilePath(filePath);

        String text = "[" + Constants.ATTACH_PREFIX + "=" + fileId + "]";

        addSpan(text, attachSpan, voiceSpan);

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
                        editable.append(builder);
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
        VoiceSpan voiceSpan = new VoiceSpan(getContext(), attach);
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(text);
        builder.setSpan(voiceSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Editable editable = getEditableText();
        try {
            if (inHander) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (selStart < 0 || getText() == null || selStart >= getText().length()) {
                            editable.append(builder);
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
     * 光标位置变化的监听
     * @author tiger
     * @update 2016/3/13 14:36
     * @version 1.0.0
     */
    public interface SelectionChangedListener {
        public void onSelectionChanged(int selStart, int selEnd);
    }
}
