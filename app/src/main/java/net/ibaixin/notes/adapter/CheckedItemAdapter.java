package net.ibaixin.notes.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * 选择的列表适配器
 * @author huanghui1
 * @update 2016/6/30 11:11
 * @version: 1.0.0
 */
public class CheckedItemAdapter extends ArrayAdapter<CharSequence> {
    public CheckedItemAdapter(Context context, int resource, int textViewResourceId,
                              CharSequence[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}