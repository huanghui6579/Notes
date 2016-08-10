package com.yunxinlink.notes.listener;

import com.nostra13.universalimageloader.core.assist.FailReason;

import com.yunxinlink.notes.model.Attach;

/**
 * 附件添加后的回调
 * @author huanghui1
 * @update 2016/7/6 14:50
 * @version: 1.0.0
 */
public interface AttachAddCompleteListener {
    /**
     * 附件添加完成的回调方法
     * @param attach 添加的附件
     * @param uri 文件的uri,如：content://、file://、http://等
     * @param data 添加成功后的文本内容
     */
    void onAddComplete(String uri, Object data, Attach attach);

    /**
     * 添加失败后的回调
     * @param uri 文件的uri,如：content://、file://、http://等
     * @param failReason 失败原因
     */
    void onAddFailed(String uri, FailReason failReason);
}