package com.yunxinlink.notes.sync.download;

import android.util.SparseArray;

import java.util.Map;

/**
 * 下载的任务
 * @author huanghui-iri
 * @update 2016/11/2 10:57
 * @version: 0.0.1
 */
public class DownloadTask {
    /**
     * 下载的任务ID，也就是附件的sid
     */
    private String id;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 保存的本地磁盘全路径，包含文件名
     */
    private String savePath;

    /**
     * 有特定key的tag
     */
    private SparseArray<Object> keyedTags;

    /**
     * 没有特定key的tag
     */
    private Object tag;

    /**
     * 设置任务是否只允许在Wifi网络环境下进行下载。 默认值 false
     */
    private boolean isWifiRequired = false;

    /**
     * 网络请求的参数
     */
    private Map<String, Object> params;
    
    private DownloadListener downloadListener;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public SparseArray<Object> getKeyedTags() {
        return keyedTags;
    }

    public void setKeyedTags(SparseArray<Object> keyedTags) {
        this.keyedTags = keyedTags;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public boolean isWifiRequired() {
        return isWifiRequired;
    }

    public void setWifiRequired(boolean wifiRequired) {
        isWifiRequired = wifiRequired;
    }

    public DownloadListener getDownloadListener() {
        return downloadListener;
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadTask that = (DownloadTask) o;

        return id != null ? id.equals(that.id) : that.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DownloadTask{" +
                "filename='" + filename + '\'' +
                ", savePath='" + savePath + '\'' +
                ", tag=" + tag +
                '}';
    }
}
