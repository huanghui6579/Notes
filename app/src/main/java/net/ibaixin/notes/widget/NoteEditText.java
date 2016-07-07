package net.ibaixin.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import net.ibaixin.notes.listener.AttachAddCompleteListener;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.util.ImageUtil;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 笔记的编辑控件
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/13 14:34
 */
public class NoteEditText extends EditText {

    private static final String TAG = "NoteEditText";
    
    public static String ATTACH_PREFIX = "attach";

    /**
     * 附件的正则表达式
     */
    protected String mAttachRegEx = "\\[" + ATTACH_PREFIX + "=([a-zA-Z0-9_]+)\\]";
    
    private Pattern mPattern;

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
    public void addImage(String filePath, final Attach attach, final AttachAddCompleteListener listener) {
        ImageUtil.generateThumbImageAsync(filePath, ImageUtil.getNoteImageSize(), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                AttchSpan imageSpan = new AttchSpan(getContext(), loadedImage);
                String fileId = null;
                if (attach != null) {
                    fileId = attach.getSId();
                } else {
                    fileId = SystemUtil.generateAttachSid();
                }
                String text = "[" + ATTACH_PREFIX + "=" + fileId + "]";
                final SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(text);
                builder.setSpan(imageSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                final int selStart = getSelectionStart();
                final int selEnd = getSelectionEnd();
                post(new Runnable() {
                    @Override
                    public void run() {
                        Editable editable = getEditableText();
                        if (selStart < 0) {
                            editable.append(builder);
                        } else {
                            editable.replace(selStart, selEnd, builder);
                        }
                    }
                });
                
                if (listener != null) {
                    Attach att = null;
                    if (attach == null) {
                        att = new Attach();
                        att.setSId(fileId);
                        att.setType(Attach.IMAGE);
                    }
                    
                    listener.onAddComplete(imageUri, text, att);
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
     * 获取指定内容里的附件信息
     * @param text 文本内容
     * @return 附件的sid
     */
    public String getAttachSid(CharSequence text) {
        if (mPattern == null) {
            mPattern = Pattern.compile(mAttachRegEx);
        }
        Matcher matcher = mPattern.matcher(text);
        String sid = null;
        if (matcher.find()) {
            try {
                sid = matcher.group(1);
            } catch (Exception e) {
                Log.e(TAG, "--getAttachSid--error--" + e.getMessage());
            }
        }
        return sid;
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
