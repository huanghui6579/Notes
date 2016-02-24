package ibaixin.net.notes;

import android.app.Application;

/**
 * @author huanghui1
 * @update 2016/2/24 19:28
 * @version: 0.0.1
 */
public class NoteApplication extends Application {
    private static NoteApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    /**
     * 获得全局的application
     * @return 全局的application
     */
    public static NoteApplication getInstance() {
        return mInstance;
    }
}
