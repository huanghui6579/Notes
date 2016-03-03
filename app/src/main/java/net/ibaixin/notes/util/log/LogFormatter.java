package net.ibaixin.notes.util.log;

import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Decide the format of log will be written to the file.
 * 
 * Created by hui.yang on 2014/11/16.
 */
public abstract class LogFormatter {
    /**
     * format the log.
     *
     * @param level
     * @param tag
     * @param msg
     * @return
     */
    public abstract String format(Log.LEVEL level, String tag, String msg, Throwable tr);

    /**
     * Eclipse Style
     */
    public static class EclipseFormatter extends LogFormatter{
        private final DateFormat formatter;

        public EclipseFormatter(){
            DateFormat.getInstance();
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault());
        }

        public EclipseFormatter(String formatOfTime){
            if (TextUtils.isEmpty(formatOfTime)){
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault());
            }else{
                formatter = new SimpleDateFormat(formatOfTime, Locale.getDefault());
            }
        }

        @Override
        public String format(Log.LEVEL level, String tag, String msg, Throwable tr) {
            if (level == null || TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)){
                return "";
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append(level.getLevelString());
            buffer.append("\t");
            buffer.append(formatter.format(System.currentTimeMillis()));
            buffer.append("\t");
            buffer.append(android.os.Process.myPid());
            buffer.append("\t");
            buffer.append(android.os.Process.myTid());
            buffer.append("\t");
            buffer.append(tag);
            buffer.append("\t");
            buffer.append(msg);
            if (tr != null) {
                buffer.append(System.getProperty("line.separator"));
                buffer.append(android.util.Log.getStackTraceString(tr));
            }

            return buffer.toString();
        }
    }

    /**
     * IDEA Style
     */
    public static class IDEAFormatter extends LogFormatter{
        private final DateFormat formatter;

        public IDEAFormatter(){
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault());
        }

        public IDEAFormatter(String formatOfTime){
            if (TextUtils.isEmpty(formatOfTime)){
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault());
            }else{
                formatter = new SimpleDateFormat(formatOfTime, Locale.getDefault());
            }
        }

        @Override
        public String format(Log.LEVEL level, String tag, String msg, Throwable tr) {
            if (level == null || TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)){
                return "";
            }

            StringBuffer buffer = new StringBuffer();
            buffer.append(formatter.format(System.currentTimeMillis()));
            buffer.append("\t");
            buffer.append(android.os.Process.myPid());
            buffer.append("-");
            buffer.append(android.os.Process.myTid());
            buffer.append("/?");
            buffer.append("\t");
            buffer.append(level.getLevelString());
            buffer.append("/");
            buffer.append(tag);
            buffer.append(":");
            buffer.append("\t");
            buffer.append(msg);
            if (tr != null) {
                buffer.append(System.getProperty("line.separator"));
                buffer.append(android.util.Log.getStackTraceString(tr));
            }

            return buffer.toString();
        }
    }
}
