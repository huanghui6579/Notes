package com.yunxinlink.notes.appwidget;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * widget item的实体
 * @author huanghui1
 * @update 2016/9/14 10:12
 * @version: 1.0.0
 */
public class WidgetItem implements Parcelable {
    /**
     * 名称
     */
    private String name;
    /**
     * 图标资源
     */
    private int resId;
    /**
     * 排序
     */
    private int sort;

    /**
     * 是否选中
     */
    private boolean isChecked;
    //笔记类型
    private int type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "WidgetItem{" +
                "name='" + name + '\'' +
                ", resId=" + resId +
                ", sort=" + sort +
                ", type=" + type +
                '}';
    }

    public WidgetItem() {}
    
    public WidgetItem(Parcel in) {
        name = in.readString();
        resId = in.readInt();
        sort = in.readInt();
        type = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(resId);
        dest.writeInt(sort);
        dest.writeInt(type);
    }

    public static final Creator<WidgetItem> CREATOR = new Creator<WidgetItem>() {
        @Override
        public WidgetItem createFromParcel(Parcel source) {
            return new WidgetItem(source);
        }

        @Override
        public WidgetItem[] newArray(int size) {
            return new WidgetItem[size];
        }
    };
}