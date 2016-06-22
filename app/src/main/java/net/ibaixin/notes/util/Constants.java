package net.ibaixin.notes.util;

/**
 * @author huanghui1
 * @update 2016/2/24 19:07
 * @version: 0.0.1
 */
public class Constants {
    public static final String APP_ROOT_NAME = "IbaiXinNotes";
    public static final String LOG_SUBFFIX = ".log";
    public static final String LOG_DIR = "log";
    
    public static final String PREF_HAS_DELETE_OPT = "has_delete_opt";
    

    /**
     * 回车的标签
     */
    public static final String TAG_ENTER = "\n";
    /**
     * 列表的标签
     */
    public static final String TAG_FORMAT_LIST = "- ";

    /**
     * 列表的标签的长度
     */
    public static final int FORMAT_LIST_TAG_LENGTH = TAG_FORMAT_LIST.length();

    /**
     * 默认选中的文件夹id，为0时选中所有文件夹
     */
    public static final String SELECTED_FOLDER_ID = "selected_folder_id";
    
    public static final int OPT_ADD_NOTE = 1;
    public static final int OPT_UPDATE_NOTE = 2;
    
    public static final String ARG_CORE_OPT = "arg_core_opt";
    public static final String ARG_CORE_OBJ = "arg_core_obj";
    
    public static final int MSG_SUCCESS = 1;
    public static final int MSG_SUCCESS2 = 2;
    
}
