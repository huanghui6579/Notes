package com.yunxinlink.notes.cache;

import com.yunxinlink.notes.model.Folder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return getFolders().size() > 0;
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
     * 添加笔记本到缓存
     * @param sid
     * @param folder
     */
    public void addFolder(String sid, Folder folder) {
        mFolderMap.put(sid, folder);
    }
    
    /**
     * 获取排序后的文件夹列表
     * @author huanghui1
     * @update 2016/6/30 10:50
     * @version: 1.0.0
     */
    public List<Folder> getSortFolders() {
        List<Folder> list = getFolders();

        if (list != null && list.size() > 0) {
            Collections.sort(list, new Folder());
        }
        return list;
    }

    /**
     * 获取没有被删除的笔记本集合，是无序的
     * @return
     */
    private List<Folder> getFolders() {
        List<Folder> list = new ArrayList<>();
        Set<String> keys = mFolderMap.keySet();
        for (String key : keys) {
            Folder folder = mFolderMap.get(key);
            if (folder.isNormal()) {
                list.add(folder);
            }
        }
        return list;
    }

    /**
     * 是否没有数据
     * @return
     */
    public boolean isEmpty() {
        return mFolderMap.size() == 0;
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
