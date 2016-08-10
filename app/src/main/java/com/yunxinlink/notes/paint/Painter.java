package com.yunxinlink.notes.paint;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.yunxinlink.notes.richtext.AttachSpec;

/**
 * @author huanghui1
 * @update 2016/7/20 20:41
 * @version: 0.0.1
 */
public class Painter implements Parcelable {
    //画笔大小
    public int size;
    
    //画笔颜色
    public int color;
    
    //画笔类型
    public int type;
    
    //画笔颜色的alpha
    public int alpha;
    
    //橡皮檫的尺寸大小
    public int eraseSize;
    
    //是否是新创建
    public boolean isNew = true;

    //如果是编辑模式，则该值为附件的信息
    public AttachSpec attachSpec;

    /**
     * 判断是否是编辑模式
     * @return
     */
    public boolean isEditMode() {
        return !isNew && attachSpec != null && !TextUtils.isEmpty(attachSpec.sid);
    }

    public Painter(int size, int color, int type, int alpha, int eraseSize) {
        this.size = size;
        this.color = color;
        this.type = type;
        this.alpha = alpha;
        this.eraseSize = eraseSize;
    }

    public Painter() {
    }

    protected Painter(Parcel in) {
        size = in.readInt();
        color = in.readInt();
        type = in.readInt();
        alpha = in.readInt();
        eraseSize = in.readInt();
        isNew = in.readByte() != 0;
        attachSpec = in.readParcelable(AttachSpec.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size);
        dest.writeInt(color);
        dest.writeInt(type);
        dest.writeInt(alpha);
        dest.writeInt(eraseSize);
        dest.writeByte((byte) (isNew ? 1 : 0));
        if (attachSpec != null) {
            dest.writeParcelable(attachSpec, flags);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Painter> CREATOR = new Creator<Painter>() {
        @Override
        public Painter createFromParcel(Parcel in) {
            return new Painter(in);
        }

        @Override
        public Painter[] newArray(int size) {
            return new Painter[size];
        }
    };
}
