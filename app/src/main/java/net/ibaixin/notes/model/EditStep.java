package net.ibaixin.notes.model;

import android.text.TextUtils;

/**
 * 文本编辑的步骤
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/5 8:31
 */
public class EditStep {
    private CharSequence content;

    private StepType stepType;

    /**
     * 字符串的开始位置
     */
    private int start;

    /**
     * 字符串的结束位置
     */
    private int end;

    /**
     * 该步骤的字符串长度
     */
    private int length;

    /**
     * 附件文件的路径
     */
    private String filePath;

    /**
     * 是否是添加内容
     */
    private boolean isAppend;

    public CharSequence getContent() {
        return content;
    }

    public void setContent(CharSequence content) {
        this.content = content;
    }

    public StepType getStepType() {
        return stepType;
    }

    public void setStepType(StepType stepType) {
        this.stepType = stepType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public boolean isAppend() {
        return isAppend;
    }

    public void setAppend(boolean isAppend) {
        this.isAppend = isAppend;
    }

    @Override
    public String toString() {
        return "EditStep{" +
                "content=" + content +
                ", stepType=" + stepType +
                ", start=" + start +
                ", end=" + end +
                ", length=" + length +
                ", filePath='" + filePath + '\'' +
                ", isAppend=" + isAppend +
                '}';
    }

    /**
     * 内容是否为空
     * @author tiger
     * @update 2016/3/5 9:43
     * @version 1.0.0
     */
    public boolean isEmpty() {
        return TextUtils.isEmpty(content) || length == 0;
    }

    /**
     * 步骤的类型
     * @author tiger
     * @update 2016/3/5 8:35
     * @version 1.0.0
     */
    enum StepType {
        TEXT,
        IMAGE,
        VOICE,
        FILE,
    }
}
