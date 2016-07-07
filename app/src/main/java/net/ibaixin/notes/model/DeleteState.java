package net.ibaixin.notes.model;

/**
 * 删除的状态，主要是没删除0，到回收站1，真删除2
 * @author huanghui1
 * @update 2016/3/8 9:55
 * @version: 0.0.1
 */
public enum DeleteState {
    /**
     * 没有删除
     */
    DELETE_NONE,

    /**
     * 删除到垃圾桶
     */
    DELETE_TRASH,

    /**
     * 隐藏
     */
    DELETE_HIDE,

    /**
     * 完全删除
     */
    DELETE_DONE;

    /**
     * 将原始的数字转换成枚举
     * @param original 原始的数字
     * @return
     */
    public static DeleteState valueOf(int original) {
        switch (original) {
            case 0:
                return DELETE_NONE;
            case 1:
                return DELETE_TRASH;
            case 2:
                return DELETE_HIDE;
            case 3:
                return DELETE_DONE;
            default:
                return DELETE_NONE;
        }
    }
}
