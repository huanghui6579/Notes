package net.ibaixin.notes.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;
import android.view.View;

import net.ibaixin.notes.util.log.Log;

/**
 * 附件的span
 * @author huanghui1
 * @update 2016/7/7 21:34
 * @version: 0.0.1
 */
public class AttchSpan extends ImageSpan {

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

    public AttchSpan(Drawable d) {
        super(d);
    }

    public AttchSpan(Context context, Bitmap b) {
        super(context, b);
    }

    public AttchSpan(Context context, Bitmap b, int verticalAlignment) {
        super(context, b, verticalAlignment);
    }

    public AttchSpan(Drawable d, int verticalAlignment) {
        super(d, verticalAlignment);
    }

    public AttchSpan(Drawable d, String source) {
        super(d, source);
    }

    public AttchSpan(Drawable d, String source, int verticalAlignment) {
        super(d, source, verticalAlignment);
    }

    public AttchSpan(Context context, Uri uri) {
        super(context, uri);
    }

    public AttchSpan(Context context, Uri uri, int verticalAlignment) {
        super(context, uri, verticalAlignment);
    }

    public AttchSpan(Context context, int resourceId) {
        super(context, resourceId);
    }

    public AttchSpan(Context context, int resourceId, int verticalAlignment) {
        super(context, resourceId, verticalAlignment);
    }

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

    /**
     * Performs the click action associated with this span.
     */
    public void onClick(View widget) {
        Log.d(TAG, "--AttchSpan--onClick----");
    }
}
