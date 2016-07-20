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

    public Painter(int size, int color, int type) {
        this.size = size;
        this.color = color;
        this.type = type;
    }

    public Painter() {
    }

    protected Painter(Parcel in) {
        size = in.readInt();
        color = in.readInt();
        type = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size);
        dest.writeInt(color);
        dest.writeInt(type);
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
