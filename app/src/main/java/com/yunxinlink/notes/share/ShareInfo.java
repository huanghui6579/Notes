package com.yunxinlink.notes.share;

import cn.sharesdk.framework.Platform;

/**
 * 分享的数据
 * @author huanghui1
 * @update 2016/8/18 16:56
 * @version: 0.0.1
 */
public class ShareInfo {
    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String text;

    /**
     * 图片的网络地址
     */
    private String imageUrl;

    /**
     * 本地图片的数组
     */
    private String[] imagePathArray;

    /**
     * 本地的图片
     */
    private String imagePath;

    /**
     * 标题的url，网友点进链接后，可以看到分享的详情
     * titleUrl是标题的网络链接，仅在人人网和QQ空间使用
     */
    private String titleUrl;

    /**
     * siteUrl是分享此内容的网站地址，仅在QQ空间使用
     */
    private String siteUrl;

    /**
     * 网友点进链接后，可以看到分享的详情
     * 仅在微信（包括好友和朋友圈）中使用
     */
    private String url;

    /**
     * 分享类型，仅在微信好友和微信朋友圈中可用，默认是 {@link Platform#SHARE_TEXT}
     */
    private int shareType = Platform.SHARE_TEXT;

    /**
     * 文件的路径
     */
    private String filePath;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String[] getImagePathArray() {
        return imagePathArray;
    }

    public void setImagePathArray(String[] imagePathArray) {
        this.imagePathArray = imagePathArray;
    }

    public String getTitleUrl() {
        return titleUrl;
    }

    public void setTitleUrl(String titleUrl) {
        this.titleUrl = titleUrl;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getShareType() {
        return shareType;
    }

    public void setShareType(int shareType) {
        this.shareType = shareType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
