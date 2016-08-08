package net.ibaixin.notes.util;

/**
 * @author huanghui1
 * @update 2016/8/8 17:32
 * @version: 0.0.1
 */
public abstract class NoteTask implements Runnable {
    protected Object[] params;
    
    public NoteTask(Object... params) {
        this.params = params; 
    }
}
