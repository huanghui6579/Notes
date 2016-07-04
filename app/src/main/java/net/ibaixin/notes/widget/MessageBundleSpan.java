package net.ibaixin.notes.widget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.text.style.URLSpan;
import android.view.View;

import net.ibaixin.notes.util.log.Log;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/13 10:54
 */
public class MessageBundleSpan extends URLSpan {
    private static final String TAG = "MessageBundleSpan";

    protected int urlType = 0;

    public MessageBundleSpan(String url) {
        super(url);
    }

    public MessageBundleSpan(Parcel src) {
        super(src);
    }

    public void setUrlType(int urlType) {
        this.urlType = urlType;
    }

    public int getUrlType() {
        return urlType;
    }

    @Override
    public void onClick(View widget) {
        Uri uri = Uri.parse(getURL());
        Context context = widget.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            android.util.Log.w("URLSpan", "Actvity was not found for intent, " + intent.toString());
        }
        Log.d(TAG, "---MessageBundleSpan---onClick---");
    }
    
    public void doAction() {
        
    }
}
