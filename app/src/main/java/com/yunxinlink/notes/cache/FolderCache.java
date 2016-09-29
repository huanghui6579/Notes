package com.yunxinlink.notes.cache;

import com.yunxinlink.notes.model.Folder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    
    /**
     * 是否有更多的文件夹
     * @author huanghui1
     * @update 2016/6/30 11:17
     * @version: 1.0.0
     */
    public boolean hasMoreFolder() {
        return mFolderMap.size() > 0;
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
    
    /**
     * 获取排序后的文件夹列表
     * @author huanghui1
     * @update 2016/6/30 10:50
     * @version: 1.0.0
     */
    public List<Folder> getSortFolders() {
        List<Folder> list = new ArrayList<>();
        list.addAll(mFolderMap.values());
        
        Collections.sort(list, new Folder());
        return list;
    }

    /**
     * 清楚缓存
     */
    public void clear() {
        if (mFolderMap != null) {
            mFolderMap.clear();
        }
    }

}
