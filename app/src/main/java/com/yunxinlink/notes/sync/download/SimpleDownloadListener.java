package com.yunxinlink.notes.sync.download;

/**
 * 重写下载监听的方法
 * @author huanghui-iri
 * @update 2016/11/3 11:08
 * @version: 0.0.1
 */
public class SimpleDownloadListener implements DownloadListener {
    @Override
    public void onStart(DownloadTask downloadTask) {
        
    }

    @Override
    public void onCompleted(DownloadTask downloadTask) {

    }

    @Override
    public void onError(DownloadTask downloadTask) {

    }

    @Override
    public void onProgress(long bytesRead, long contentLength, boolean done) {
        
    }

    @Override
    public void onCanceled(DownloadTask downloadTask) {
        
    }
}
