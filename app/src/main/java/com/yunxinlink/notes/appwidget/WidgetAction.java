package com.yunxinlink.notes.appwidget;

/**
 * widget item 的操作类型
 * @author huanghui1
 * @update 2016/9/14 9:55
 * @version: 0.0.1
 */
public enum WidgetAction {

    /**
     * 快速新建笔记 
     */
    NOTE_TEXT,

    /**
     * 新建相机笔记
     */
    NOTE_CAMERA,

    /**
     * 新建语音笔记
     */
    NOTE_VOICE,

    /**
     * 新建涂鸦笔记
     */
    NOTE_BRUSH,

    /**
     * 新建图片笔记
     */
    NOTE_PHOTO,

    /**
     * 新建附件笔记
     */
    NOTE_FILE,

    /**
     * 笔记搜索
     */
    NOTE_SEARCH;
    
    public static WidgetAction valueOf(int value) {
        switch (value) {
            case 0:
                return NOTE_TEXT;
            case 1:
                return NOTE_CAMERA;
            case 2:
                return NOTE_VOICE;
            case 3:
                return NOTE_BRUSH;
            case 4:
                return NOTE_PHOTO;
            case 5:
                return NOTE_FILE;
            case 6:
                return NOTE_SEARCH;
            default:
                return null;
        }
    }
}
