package com.yunxinlink.notes.listener;

import com.nostra13.universalimageloader.core.assist.FailReason;

import com.yunxinlink.notes.model.Attach;

/**
 * 附件添加后的回调
 * @author huanghui1
 * @update 2016/7/6 15:43
 * @version: 1.0.0
 */
public class SimpleAttachAddCompleteListener implements AttachAddCompleteListener {

    @Override
    public void onAddComplete(String uri, Object data, Attach attach) {

    }

    @Override
    public void onAddFailed(String uri, FailReason failReason) {

    }
}