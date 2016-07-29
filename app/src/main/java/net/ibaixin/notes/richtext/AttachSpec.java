package net.ibaixin.notes.richtext;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * 解析附件的实体
 */
public class AttachSpec implements Parcelable {
    public CharSequence text;
    public String sid;
    public int start;
    public int end;
    
    //附件类型
    public int attachType;
    
    //附件的本地路径
    public String filePath;
    
    //笔记的sid
    public String noteSid;
    
    //文件的mimeType
    public String mimeType;
    
    public AttachSpec() {}

    /**
     * 是否是编辑模式
     * @return
     */
    public boolean isEditMode() {
        return !TextUtils.isEmpty(sid);
    }

    protected AttachSpec(Parcel in) {
        sid = in.readString();
        start = in.readInt();
        end = in.readInt();
        attachType = in.readInt();
        filePath = in.readString();
        text = in.readString();
        noteSid = in.readString();
        mimeType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sid);
        dest.writeInt(start);
        dest.writeInt(end);
        dest.writeInt(attachType);
        dest.writeString(filePath);
        dest.writeString(text == null ? "" : text.toString());
        dest.writeString(noteSid);
        dest.writeString(mimeType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<AttachSpec> CREATOR = new Creator<AttachSpec>() {
        @Override
        public AttachSpec createFromParcel(Parcel in) {
            return new AttachSpec(in);
        }

        @Override
        public AttachSpec[] newArray(int size) {
            return new AttachSpec[size];
        }
    };
}