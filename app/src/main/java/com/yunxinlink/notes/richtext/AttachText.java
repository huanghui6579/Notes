package com.yunxinlink.notes.richtext;

import java.util.List;

/**
 * 笔记附件内容
 * @author huanghui1
 * @update 2016/8/15 19:12
 * @version: 0.0.1
 */
public class AttachText {
    /**
     * 文本中含有的附件sid集合
     */
    private List<String> attachSids;

    /**
     * 笔记的显示内容，除去了附件的sid
     */
    private String text;

    public List<String> getAttachSids() {
        return attachSids;
    }

    public void setAttachSids(List<String> attachSids) {
        this.attachSids = attachSids;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
