package net.ibaixin.notes.widget;

import android.text.style.ClickableSpan;
import android.view.View;

import net.ibaixin.notes.util.log.Log;

/**
 * 附件的span
 * @author huanghui1
 * @update 2016/7/7 21:34
 * @version: 0.0.1
 */
public class AttchSpan extends ClickableSpan {

    private static final java.lang.String TAG = "AttchSpan";
    /**
     * 附件的类型
     */
    protected int attachType = 0;

    /**
     * 附件的本地全路径
     */
    protected String filePath;

    /**
     * 附件的id
     */
    private String attachId;

    /**
     * 内容
     */
    private CharSequence text;

    public int getAttachType() {
        return attachType;
    }

    public void setAttachType(int attachType) {
        this.attachType = attachType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAttachId() {
        return attachId;
    }

    public void setAttachId(String attachId) {
        this.attachId = attachId;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    /**
     * Performs the click action associated with this span.
     */
    @Override
    public void onClick(View widget) {
        Log.d(TAG, "--AttchSpan--onClick----");
    }
}
