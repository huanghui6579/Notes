package net.ibaixin.notes.db.observer;

/**
 * 观察者
 * @author huanghui1
 * @update 2016/3/9 16:44
 * @version: 0.0.1
 */
public interface Observer {
    public enum NotifyType {
        /**
         * 单个添加
         */
        ADD,
        /**
         * 单个删除
         */
        DELETE,
        /**
         * 单个更新
         */
        UPDATE,
        /**
         * 批量更新
         */
        BATCH_UPDATE,
    }

    /**
     * This method is called if the specified {@code Observable} object's
     * {@code notifyObservers} method is called (because the {@code Observable}
     * object has been updated.
     *
     * @param observable
     *            the {@link Observable} object.
     * @param notifyFlag 通知标识
     * @param data
     *            the data passed to {@link Observable#notifyObservers(Object)}.
     * @param notifyType one of {@link NotifyType#ADD}, {@link NotifyType#DELETE}, {@link NotifyType#UPDATE}
     */
    public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);

    /**
     * 处理更新
     * @update 2015年3月10日 下午2:01:17
     * @param observable
     * @param notifyFlag 通知标识
     * @param notifyType
     * @param data
     */
    public void dispatchUpdate(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);
}
