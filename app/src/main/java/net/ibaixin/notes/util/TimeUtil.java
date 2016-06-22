package net.ibaixin.notes.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author huanghui1
 * @update 2016/6/22 19:34
 * @version: 0.0.1
 */
public class TimeUtil {
    private TimeUtil() {}
    
    /**
     * 格式化笔记的日期，格式为yyyy-MM-dd HH:mm
     * @author huanghui1
     * @update 2016/6/22 20:48
     * @version: 1.0.0
     */
    public static String formatNoteTime(long time) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(time));
    }
}
