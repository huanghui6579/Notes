package net.ibaixin.notes.cache;

import net.ibaixin.notes.model.Folder;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件夹的缓存
 * @author huanghui1
 * @update 2016/6/22 20:42
 * @version: 0.0.1
 */
public class FolderCache {
    
    private static FolderCache mInstance;

    //文件夹的缓存
    private Map<String, Folder> mFolderMap = new HashMap<>();
    
    private FolderCache() {}
    
    public static FolderCache getInstance() {
        if (mInstance == null) {
            synchronized (FolderCache.class) {
                if (mInstance == null) {
                    mInstance = new FolderCache();
                }
            }
        }
        return mInstance;
    }

    public Map<String, Folder> getFolderMap() {
        return mFolderMap;
    }

    public void setFolderMap(Map<String, Folder> folderMap) {
        this.mFolderMap = folderMap;
    }
    
    /**
     * 获取缓存中的文件夹
     * @param sid 文件夹的sid
     * @author huanghui1
     * @update 2016/6/29 19:56
     * @version: 1.0.0
     */
    public Folder getCacheFolder(String sid) {
        return mFolderMap.get(sid);
    }
}
