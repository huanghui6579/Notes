package com.yunxinlink.notes.api.model;

import com.yunxinlink.notes.util.Constants;

/**
 * 分页相关的类
 * @author huanghui-iri
 * @update 2016/10/20 11:50
 * @version: 0.0.1
 */
public class PageInfo<T> {
    /**
     * 第几页，从1开始
     */
    private Integer pageNumber;

    private Integer pageSize = Constants.PAGE_SIZE_DEFAULT;

    private long count;

    private T data;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "PageInfo{" +
                "pageNumber=" + pageNumber +
                ", pageSize=" + pageSize +
                ", count=" + count +
                ", data=" + data +
                '}';
    }
}
