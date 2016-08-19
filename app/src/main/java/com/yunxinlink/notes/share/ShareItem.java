package com.yunxinlink.notes.share;

import android.text.TextUtils;

/**
 * @author huanghui1
 * @update 2016/8/18 17:57
 * @version: 0.0.1
 */
public class ShareItem {
    /**
     * 列表显示标题
     */
    private String title;

    /**
     * 列表显示的图标资源
     */
    private int resId;

    /**
     * 列表项的类型
     */
    private String platform;

    public ShareItem(String title, int resId, String platform) {
        this.title = title;
        this.resId = resId;
        this.platform = platform;
    }

    public ShareItem(String platform) {
        this.platform = platform;
    }

    public ShareItem() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * 该项是否是空的
     * @return
     */
    public boolean isEmpty() {
        return TextUtils.isEmpty(platform);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShareItem shareItem = (ShareItem) o;

        return platform != null ? platform.equals(shareItem.platform) : shareItem.platform == null;

    }

    @Override
    public int hashCode() {
        return platform != null ? platform.hashCode() : 0;
    }
}
