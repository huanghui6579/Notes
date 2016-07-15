package net.ibaixin.notes.edit.recorder;

import android.media.MediaRecorder;
import android.os.Handler;

import net.ibaixin.notes.util.FileUtil;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 录音器
 * @author huanghui1
 * @update 2016/7/13 17:14
 * @version: 0.0.1
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    
    //录音时长最长60分钟
    private static final long MAX_TIME = 3600000;
    
    private MediaRecorder mRecorder = null;

    private String mFilePath = null;

    /**
     * 是否正在录音
     */
    private boolean mIsRecording;

    /**
     * 录音的开始时间
     */
    private long mStartTime;

    /**
     * 录音的结束时间
     */
    private long mEndTime;
    
    //计时器
    private Timer mTimer;
    
    //录音状态的监听器
    private OnRecordListener mRecordListener;
    
    private Handler mHandler;
    
    public void setRecordListener(OnRecordListener recordListener) {
        this.mRecordListener = recordListener;
    }
    
    public AudioRecorder(Handler handler) {
        this.mHandler = handler;
    }

    private void onRecord(boolean start) {
        try {
            if (start) {
                startRecording();
            } else {
                stopRecording();
            }
        } catch (Exception e) {
            Log.e(TAG, "---onRecord---error--" + e.getMessage());
        }
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * 获取录音的时长,最长60分钟，一到60分钟，自动录音结束
     * @return
     */
    public long getRecordTime() {
        long time = mEndTime - mStartTime;
        if (time < 0) {
            time = 0;
        } else if (time >= MAX_TIME) {
            time = MAX_TIME;
        }
        return time;
    }

    /**
     * 初始化录音器
     */
    public void initRecorder() {
        Log.d(TAG, "---initRecorder---begin---");
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mStartTime = 0L;
        mEndTime = 0L;
        Log.d(TAG, "---initRecorder---end---");
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        this.mFilePath = filePath;
    }

    /**
     * 开始录音
     */
    public void startRecording() {
        
        try {
            if (mRecordListener != null) {
                mRecordListener.onBeforeRecord(mFilePath);
            }
            initRecorder();
            mRecorder.setOutputFile(mFilePath);
            mRecorder.prepare();
            doRecord();
        } catch (final IOException e) {
            recordError(e);
            e.printStackTrace();
        }
    }
    
    private void recordError(final Exception e) {
        Log.e(TAG, "---startRecording--error--" + e.getMessage());
        FileUtil.deleteFile(mFilePath);
        if (mRecordListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordListener.onRecordError(mFilePath, e.getMessage());
                }
            });
        }
        resetRecorder();
    }
    
    private void doRecord() {
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "---startRecording--begin--");
                    mRecorder.start();
                    mStartTime = System.currentTimeMillis();
                    mIsRecording = true;
                    //每秒执行一次
                    mTimer = new Timer("TimeRecordTimer");
                    mTimer.schedule(new TimeRecordTask(), 0, 1000);
                    Log.d(TAG, "---startRecording--end--");
                } catch (Exception e) {
                    recordError(e);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 重置录音器
     */
    private void resetRecorder() {
        mStartTime = 0L;
        mEndTime = 0L;
        mFilePath = null;
        mIsRecording = false;
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    /**
     * 停止录音
     */
    public void stopRecording() {
        mRecorder.stop();
        mIsRecording = false;
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mRecordListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mRecordListener.onEndRecord(mFilePath, getRecordTime());
                }
            });
           
        }
    }

    /**
     * 释放录音器
     */
    public void releaseRecorder() {
        if (mRecorder != null) {
            stopRecording();
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * 录音时间的计时器
     */
    class TimeRecordTask extends TimerTask {

        @Override
        public void run() {
            if (mRecordListener != null && mIsRecording) {
                mEndTime = System.currentTimeMillis();
                final long time = getRecordTime();
                if ((time + 1000) >= MAX_TIME) { //超时，停止录音
                    stopRecording();
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRecordListener.onRecording(mFilePath, time);
                        }
                    });
                    
                }
            }
        }
    }

    /**
     * 录音状态的监听器
     */
    public interface OnRecordListener {
        /**
         * 刚开始准备录音，还没开始
         * @param filePath 录音文件的存储路径
         */
        void onBeforeRecord(String filePath);

        /**
         * 正在录音，子线程里执行
         * @param time 录音的时长
         * @param filePath filePath
         */
        void onRecording(String filePath, long time);

        /**
         * 结束录音
         * @param time 录音的时长
         * @param filePath filePath
         */
        void onEndRecord(String filePath, long time);

        /**
         * 录音失败的回调
         * @param filePath 录音文件的存储路径
         * @param errorMsg 失败原因
         */
        void onRecordError(String filePath, String errorMsg);
    }
}
