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
    
    public static final String PATTERN_FILE_TIME = "yyyy-MM-dd HH:mm:ss";
    
    private TimeUtil() {}
    
    /**
     * 格式化笔记的日期，格式为yyyy-MM-dd HH:mm
     * @author huanghui1
     * @update 2016/6/22 20:48
     * @version: 1.0.0
     */
    public static String formatNoteTime(long time) {
        return formatTime(time, "yyyy-MM-dd HH:mm");
    }

    /**
     * 格式化时间
     * @param time
     * @param pattern
     * @return
     */
    public static String formatTime(long time, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        return dateFormat.format(new Date(time));
    }
}
