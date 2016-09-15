package com.yunxinlink.notes.appwidget;

import android.os.Parcel;
import android.os.Parcelable;

import com.yunxinlink.notes.R;

/**
 * widget item的实体
 * @author huanghui1
 * @update 2016/9/14 10:12
 * @version: 1.0.0
 */
public class WidgetItem implements Parcelable {
    private int id;
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
     * 排序方式2，主要用于列表的标题栏的图标排序
     */
    private int sort2;

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
        if (resId == 0) {
            WidgetAction action = WidgetAction.valueOf(type);
            if (action != null) {
                switch (action) {
                    case NOTE_TEXT:
                        resId = R.drawable.ic_note_add;
                        break;
                    case NOTE_CAMERA:
                        resId = R.drawable.ic_action_camera;
                        break;
                    case NOTE_VOICE:
                        resId = R.drawable.ic_action_voice;
                        break;
                    case NOTE_BRUSH:
                        resId = R.drawable.ic_action_brush;
                        break;
                    case NOTE_PHOTO:
                        resId = R.drawable.ic_action_photo;
                        break;
                    case NOTE_FILE:
                        resId = R.drawable.ic_action_insert_file;
                        break;
                    case NOTE_SEARCH:
                        resId = R.drawable.ic_action_search;
                        break;
                    default:
                        resId = R.drawable.ic_note_add;
                        break;

                }
            } else {
                resId = R.drawable.ic_note_add;
            }
        }
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSort2() {
        return sort2;
    }

    public void setSort2(int sort2) {
        this.sort2 = sort2;
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
        id = in.readInt();
        name = in.readString();
        resId = in.readInt();
        sort = in.readInt();
        sort2 = in.readInt();
        type = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(resId);
        dest.writeInt(sort);
        dest.writeInt(sort2);
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