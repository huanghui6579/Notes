package net.ibaixin.notes.model;

/**
 * 同步的状态
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/7 21:49
 */
public enum SyncState {
    /**
     * 不需要同步
     */
    SYNC_NONE,

    /**
     * 向上同步
     */
    SYNC_UP,

    /**
     * 向下同步
     */
    SYNC_DOWN;

    /**
     * 将int转换成枚举
     * @param original 原始的int值
     * @return
     */
    public static SyncState valueOf(int original) {
        switch (original) {
            case 0:
                return SYNC_NONE;
            case 1:
                return SYNC_UP;
            case 2:
                return SYNC_DOWN;
            default:
                return SYNC_NONE;
        }
    }
}
