package com.yunxinlink.notes.richtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import android.util.SparseArray;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.listener.RichTextClickListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.widget.AttachSpan;
import com.yunxinlink.notes.widget.FileSpan;

import java.util.List;
import java.util.Map;

import static com.yunxinlink.notes.R.drawable.ic_broken_image;

/**
 * 附件解析器
 * @author huanghui1
 * @update 2016/7/8 11:52
 * @version: 0.0.1
 */
public class AttachResolver implements Resolver {
    private static final String TAG = "AttachResolver";
    private Handler mHandler;

    @Override
    public void resolve(NoteRichSpan richSpan, CharSequence text, SparseArray<Object> extra, RichTextClickListener listener) {
        Map<String, Attach> map = (Map<String, Attach>) extra.get(0);
        if (map != null && map.size() > 0) {
//                mHandler.post(new AnalysisTextTask(editText, text, map));
            postTask(new AnalysisTextTask(richSpan, text, map));
        }
    }

    @Override
    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    private void postTask(Runnable runnable) {
        SystemUtil.getThreadPool().execute(runnable);
    }

    /**
     * 从文本解析出附件的sid
     */
    class AnalysisTextTask implements Runnable {
        Map<String, Attach> map;
        CharSequence text;
        NoteRichSpan richSpan;

        public AnalysisTextTask(NoteRichSpan richSpan, CharSequence text, Map<String, Attach> map) {
            this.map = map;
            this.text = text;
            this.richSpan = richSpan;
        }

        @Override
        public void run() {
            List<AttachSpec> list = SystemUtil.getAttachText(text);
            if (list != null && list.size() > 0) {
                for (AttachSpec spec : list) {
                    postTask(new ResolverAttachTask(richSpan, spec, map));
                }
            }
        }
    }

    /**
     * 显示附件的任务
     */
    class ResolverAttachTask implements Runnable {
        
        Map<String, Attach> map;
        
        AttachSpec spec;
        
        private Context context;
        
        private NoteRichSpan richSpan;

        public ResolverAttachTask(NoteRichSpan richSpan, AttachSpec spec, Map<String, Attach> map) {
            this.richSpan = richSpan;
            this.map = map;
            this.spec = spec;
            context = richSpan.getNoteContext();
        }

        @Override
        public void run() {
            String sid = spec.sid;
            Attach attach = map.get(sid);
            if (attach != null && richSpan != null) {
                final String text = spec.text.toString();
                final int selStart = spec.start;
                final int selEnd = spec.end;
                
                String filePath = attach.getLocalPath();
                final AttachSpan attachSpan = new AttachSpan();
                attachSpan.setAttachId(sid);
                attachSpan.setAttachType(attach.getType());
                attachSpan.setFilePath(filePath);
                attachSpan.setText(text);
                attachSpan.setSelStart(selStart);
                attachSpan.setSelEnd(selEnd);
                attachSpan.setNoteSid(attach.getNoteId());
                attachSpan.setMimeType(attach.getMimeType());
                ReplacementSpan replacementSpan = null;
                int[] size = null;
                switch (attach.getType()) {
                    case Attach.IMAGE:  //显示图片
                        replacementSpan = getImageSpan(context, filePath, null);
                        break;
                    case Attach.PAINT:  //显示绘画
                        size = richSpan.getSize();
                        ImageSize imageSize = new ImageSize(size[0], size[1]);
                        replacementSpan = getImageSpan(context, filePath, imageSize);
                        break;
                    default:  //录音文件
                        size = richSpan.getSize();
                        replacementSpan = new FileSpan(context, attach, size[0]);
                        break;
                }
                KLog.d(TAG, "---ResolverAttachTask----addSpan-----replacementSpan--attachSpan:" + attachSpan);
                if (replacementSpan != null) {
                    richSpan.addSpan(text, attachSpan, replacementSpan, selStart, selEnd, null);
                }
            }
        }
    }

    /**
     * 获取图片的span
     * @param context
     * @param filePath 图片的路径
     * @return
     */
    public ReplacementSpan getImageSpan(Context context, String filePath, ImageSize imageSize) {
        ReplacementSpan replacementSpan = null;
        Bitmap bitmap = null;
        if (!TextUtils.isEmpty(filePath)) {
            bitmap = ImageUtil.loadImageThumbnailsSync(filePath, imageSize);
        }
        if (bitmap == null) {
            Drawable drawable = getFailedImage(context);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
            replacementSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
        } else {
            replacementSpan = new ImageSpan(context, bitmap);
        }
        return replacementSpan;
    }

    /**
     * 初始化图片加载时候的图片
     * @return
     */
    private Drawable getFailedImage(Context context) {
        int color = ContextCompat.getColor(context, R.color.text_time_color);
        Drawable drawable = ContextCompat.getDrawable(context, ic_broken_image);
        drawable = SystemUtil.getTintDrawable(context, drawable, color);
        return drawable;
    }
}
