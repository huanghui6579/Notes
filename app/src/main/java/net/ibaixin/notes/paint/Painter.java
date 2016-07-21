package net.ibaixin.notes.paint;

import android.os.Parcel;
import android.os.Parcelable;

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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size);
        dest.writeInt(color);
        dest.writeInt(type);
        dest.writeInt(alpha);
        dest.writeInt(eraseSize);
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
