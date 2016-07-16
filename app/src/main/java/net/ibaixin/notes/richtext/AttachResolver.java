package net.ibaixin.notes.richtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.style.ImageSpan;
import android.text.style.ReplacementSpan;
import android.util.SparseArray;
import android.widget.TextView;

import net.ibaixin.notes.listener.RichTextClickListener;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.util.ImageUtil;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.widget.AttachSpan;
import net.ibaixin.notes.widget.NoteEditText;
import net.ibaixin.notes.widget.VoiceSpan;

import java.util.List;
import java.util.Map;

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
    public void resolve(TextView textView, CharSequence text, SparseArray<Object> extra, RichTextClickListener listener) {
        if (textView instanceof NoteEditText) {
            NoteEditText editText = (NoteEditText) textView;

            Map<String, Attach> map = (Map<String, Attach>) extra.get(0);
            if (map != null && map.size() > 0) {
//                mHandler.post(new AnalysisTextTask(editText, text, map));
                postTask(new AnalysisTextTask(editText, text, map));
            }
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
        NoteEditText editText;

        public AnalysisTextTask(NoteEditText editText, CharSequence text, Map<String, Attach> map) {
            this.map = map;
            this.text = text;
            this.editText = editText;
        }

        @Override
        public void run() {
            List<AttachSpec> list = SystemUtil.getAttachText(text);
            if (list != null && list.size() > 0) {
                for (AttachSpec spec : list) {
                    postTask(new ResolverAttachTask(editText, spec, map));
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
        
        private NoteEditText editText;

        public ResolverAttachTask(NoteEditText editText, AttachSpec spec, Map<String, Attach> map) {
            this.editText = editText;
            this.map = map;
            this.spec = spec;
            context = editText.getContext();
        }

        @Override
        public void run() {
            String sid = spec.sid.toString();
            Attach attach = map.get(sid);
            if (attach != null) {
                String filePath = attach.getLocalPath();
                AttachSpan attchSpan = new AttachSpan();
                attchSpan.setAttachId(sid);
                attchSpan.setAttachType(attach.getType());
                attchSpan.setFilePath(filePath);
                String text = spec.text.toString();

                final int selStart = spec.start;
                final int selEnd = spec.end;
                ReplacementSpan replacementSpan = null;
                switch (attach.getType()) {
                    case Attach.IMAGE:  //显示图片
                        Bitmap bitmap = ImageUtil.loadImageThumbnailsSync(filePath);
                        if (bitmap == null) {
                            return;
                        }
                        replacementSpan = new ImageSpan(context, bitmap);
                        break;
                    case Attach.VOICE:  //录音文件
                        replacementSpan = new VoiceSpan(context, attach);
                        break;
                }
                if (replacementSpan != null) {
                    editText.addSpane(text, attchSpan, replacementSpan, selStart, selEnd);
                } else {
                    Log.d(TAG, "---ResolverAttachTask--replacementSpan--is---null--");
                }
            }
        }
    }
    
    
}
