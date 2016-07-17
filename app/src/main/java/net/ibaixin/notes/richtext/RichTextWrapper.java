package net.ibaixin.notes.richtext;

import android.content.Context;
import android.os.Handler;
import android.support.v4.util.ArrayMap;
import android.text.method.MovementMethod;
import android.util.SparseArray;
import android.widget.TextView;

import net.ibaixin.notes.listener.RichTextClickListener;
import net.ibaixin.notes.model.Attach;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huanghui1
 * @update 2016/7/8 10:46
 * @version: 0.0.1
 */
public class RichTextWrapper {
    private SparseArray<Object> mExtra = new SparseArray<>();
    private HashMap<Class<? extends Resolver>, Resolver> mResolvers = new HashMap<>();
    private ArrayMap<String, RichTextClickListener> mListenerMap = new ArrayMap<>();
    private NoteRichSpan mRichSpan;
    
    private Handler mHandler;

    public RichTextWrapper(NoteRichSpan richSpan, Handler handler) {
        if (richSpan == null) {
            return;
        }
        this.mRichSpan = richSpan;
        this.mHandler = handler;
    }

    public void putExtra(int key, Object value) {
        mExtra.put(key, value);
    }

    public void addResolver(Class<? extends Resolver>... clazzs) {
        for(Class<? extends Resolver> clazz:clazzs){
            if(!mResolvers.containsKey(clazz)) {
                mResolvers.put(clazz, null);
            }
        }
    }

    public void setOnRichTextListener(Class<? extends Resolver> clazz, RichTextClickListener listener) {
        if(!mResolvers.containsKey(clazz)) {
            mResolvers.put(clazz, null);
        }
        mListenerMap.put(clazz.getSimpleName(), listener);
    }

    public void setRichSpan(NoteRichSpan richSpan) {
        this.mRichSpan = richSpan;
    }

    public Context getContext() {
        return mRichSpan.getNoteContext();
    }
    
    public CharSequence getText() {
        return mRichSpan.getTextContent();
    }

    public NoteRichSpan getRichSpan() {
        return mRichSpan;
    }

    /**
     * Sets the movement method (arrow key handler) to be used for
     * this TextView.  This can be null to disallow using the arrow keys
     * to move the cursor or scroll the view.
     * <p>
     * Be warned that if you want a TextView with a key listener or movement
     * method not to be focusable, or if you want a TextView without a
     * key listener or movement method to be focusable, you must call
     * {@link TextView#setFocusable} again after calling this to get the focusability
     * back the way you want it.
     */
    public void setMovementMethod(MovementMethod movement) {
        mRichSpan.setTextMovementMethod(movement);
    }

    private void resolveText(Map<String, Attach> map) {
        for (Class<? extends Resolver> clazz : mResolvers.keySet()) {
            Resolver resolver = mResolvers.get(clazz);
            if (resolver == null) {
                try {
                    resolver = clazz.newInstance();
                    mResolvers.put(clazz, resolver);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                    continue;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            RichTextClickListener listener = mListenerMap.get(resolver.getClass().getSimpleName());
            mExtra.put(0, map);
            resolver.setHandler(mHandler);
            resolver.resolve(mRichSpan, mRichSpan.getTextContent(), mExtra, listener);
        }
    }

    /**
     * set text 
     * @param text content text
     * @param map 附件的缓存            
     */
    public void setText(CharSequence text, Map<String, Attach> map) {
        mRichSpan.setTextContent(text);
        resolveText(map);
    }
}
