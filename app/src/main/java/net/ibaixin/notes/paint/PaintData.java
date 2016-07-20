package net.ibaixin.notes.paint;

import java.util.Stack;

/**
 * 画笔的数据
 * @author huanghui1
 * @update 2016/7/20 19:58
 * @version: 0.0.1
 */
public class PaintData {
    //回撤的集合
    public Stack<PaintRecord> mUndoStack;
    //前进的集合
    public Stack<PaintRecord> mRedoStack;
    
    public PaintData() {
        mUndoStack = new Stack<>();
        mRedoStack = new Stack<>();
    }    
}
