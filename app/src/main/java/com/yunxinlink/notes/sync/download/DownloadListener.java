package com.yunxinlink.notes.sync.download;

/**
 * 下载的任务监听器
 * @author huanghui-iri
 * @update 2016/11/2 11:35
 * @version: 0.0.1
 */
public interface DownloadListener {

    /**
     * 开始下载
     * @param downloadTask
     */
    void onStart(DownloadTask downloadTask);
    
    /**
     * 下载结束
     * @param downloadTask
     */
    void onCompleted(DownloadTask downloadTask);

    /**
     * 下载失败
     * @param downloadTask
     */
    void onError(DownloadTask downloadTask);

    /**
     * 下载的进度
     * @param bytesRead
     * @param contentLength
     * @param done
     */
    void onProgress(long bytesRead, long contentLength, boolean done);

    /**
     * 下载任务取消了
     * @param downloadTask
     */
    void onCanceled(DownloadTask downloadTask);
}
