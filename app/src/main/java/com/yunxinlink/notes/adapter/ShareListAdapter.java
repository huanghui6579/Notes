package com.yunxinlink.notes.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.share.ShareItem;

import java.util.List;

/**
 * 分享的列表适配器
 *
 * @author huanghui1
 * @update 2016/8/18 15:31
 * @version: 1.0.0
 */
public class ShareListAdapter extends BaseAdapter {

    private List<ShareItem> mList;
    private LayoutInflater mInflater;

    private int[] mResArray = {R.drawable.ic_classic_sinaweibo, R.drawable.ic_classic_wechat, R.drawable.ic_classic_wechatmoments,
            R.drawable.ic_classic_qq, R.drawable.ic_classic_qzone, R.drawable.ic_more_horiz};

    public ShareListAdapter(List<ShareItem> list, Context context) {
        this.mList = list;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ShareViewHolder holder = null;
        if (convertView == null) {
            View view = mInflater.inflate(R.layout.item_share_layout, parent, false);

            holder = new ShareViewHolder();
            holder.ivIcon = (ImageView) view.findViewById(R.id.iv_icon);
            holder.tvTitle = (TextView) view.findViewById(android.R.id.text1);

            convertView = view;

            convertView.setTag(holder);
        } else {
            holder = (ShareViewHolder) convertView.getTag();
        }
        
        ShareItem shareItem = mList.get(position);
        
        String title = shareItem.getTitle();
        int resId = shareItem.getResId();

        holder.tvTitle.setText(title);
        holder.ivIcon.setImageResource(resId);
        return convertView;
    }

    class ShareViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
    }
}