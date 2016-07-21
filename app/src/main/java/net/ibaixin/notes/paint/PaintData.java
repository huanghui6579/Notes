package net.ibaixin.notes.paint;

import java.util.LinkedList;
import java.util.List;

/**
 * 画笔的数据
 * @author huanghui1
 * @update 2016/7/20 19:58
 * @version: 0.0.1
 */
public class PaintData {
    //回撤的集合
    public List<PaintRecord> mUndoList;
    //前进的集合
    public List<PaintRecord> mRedoList;
    
    public PaintData() {
        mUndoList = new LinkedList<>();
        mRedoList = new LinkedList<>();
    }

    /**
     * 清除
     */
    public void clear() {
        mUndoList.clear();
        mRedoList.clear();
    }
}
