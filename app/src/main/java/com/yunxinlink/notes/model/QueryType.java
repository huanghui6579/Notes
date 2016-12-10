package com.yunxinlink.notes.model;

/**
 * 查询的条件，主要是几种删除状态
 * @author huanghui-iri
 * @update 2016/12/8 14:22
 * @version: 0.0.1
 */
public enum QueryType {
    /**
     * 普通的，没有删除的
     */
    NORMAL,

    /**
     * 删除到回收站的
     */
    TRASH,

    /**
     * 所有的，包含删除的和没有删除的
     */
    ALL;
    
    public static QueryType valueOf(int value) {
        switch (value) {
            case 0:
                return NORMAL;
            case 1:
                return TRASH;
            case 2:
                return ALL;
            default:
                return NORMAL;
        }
    }
}
