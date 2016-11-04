package com.yunxinlink.notes.db.observer;

/**
 * 观察者
 * @author huanghui1
 * @update 2016/3/9 16:44
 * @version: 0.0.1
 */
public interface Observer {
    enum NotifyType {
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
         * 移动操作
         */
        MOVE,
        /**
         * 批量更新
         */
        BATCH_UPDATE,

        /**
         * 移出，彻底删除
         */
        REMOVE,

        /**
         * 合并，一般用于服务器下载到本地
         */
        MERGE,

        /**
         * 刷新，数据加载完毕了
         */
        REFRESH,

        /**
         * 正在加载数据、或者正在同步数据，主要是显示进度条
         */
        LOADING,

        /**
         * 数据加载完毕
         */
        DONE,
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
     *            the data passed to {@link Observable#notifyObservers(int, NotifyType, Object)}.
     * @param notifyType one of {@link NotifyType#ADD}, {@link NotifyType#DELETE}, {@link NotifyType#UPDATE}
     */
    void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);

    /**
     * 处理更新
     * @update 2015年3月10日 下午2:01:17
     * @param observable
     * @param notifyFlag 通知标识
     * @param notifyType
     * @param data
     */
    void dispatchUpdate(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data);
}
