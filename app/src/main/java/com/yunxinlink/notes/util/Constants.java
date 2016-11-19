package com.yunxinlink.notes.util;

/**
 * @author huanghui1
 * @update 2016/2/24 19:07
 * @version: 0.0.1
 */
public class Constants {
    public static final String APP_ROOT_NAME = "YunXinNotes";
    public static final String LOG_SUBFFIX = ".log";
    public static final String APP_LOG_DIR = "log";

    public static final String APP_DOWNLOAD_FOLDER_NAME = "Download";
    public static final String APP_CAMERA_FOLDER_NAME = "Camera";
    public static final String APP_VOICE_FOLDER_NAME = "Voice";
    public static final String APP_PAINT_FOLDER_NAME = "Paint";
    public static final String DATA_MSG_ATT_FOLDER_NAME = "attach";
    public static final String APP_NOTES_FOLDER_NAME = "notes";
    public static final String APP_AVATAR_FOLDER_NAME = "icon";

    //通知栏快捷方式的通知id，1000
    public static final int ID_NOTIFY_CREATE_SHORTCUT = 1000;

    /**
     * 最多只处理5个分享过来的附件
     */
    public static final int MAX_SHARE_ATTACH_SIZE = 5;
    /**
     * 桌面显示的widget项的数量
     */
    public static final int MAX_WIDGET_ITEM_SIZE = 5;
    
    //菜单各项的不可用的alpha颜色值
    public static final float MENU_ITEM_COLOR_ALPHA = 0.25F;

    /**
     * 系统默认手动生成的缩略图缩放参造宽度400
     */
    public static final int IMAGE_THUMB_WIDTH = 400;

    /**
     * 头像的尺寸，100
     */
    public static final int AVATAR_THUMB_WIDTH = 100;

    /**
     * 系统默认手动生成的缩略图缩放参造高度100
     */
    public static final int IMAGE_THUMB_HEIGHT = 400;

    /**
     * 默认分页大小，20条数据
     */
    public static final int PAGE_SIZE_DEFAULT = 20;

    public static final String CLIP_TEXT_LABEL = "text";
    
    public static final String PREF_HAS_DELETE_OPT = "has_delete_opt";
    /**
     * 桌面小工具默认的保存的文件夹
     */
    public static final String PREF_DEFAULT_FOLDER = "default_folder";
    /**
     * 是否显示“所有文件夹”这一项
     */
    public static final String PREF_SHOW_FOLDER_ALL = "show_folder_all";
    /**
     * 默认选中的文件夹id，为null时选中所有文件夹
     */
    public static final String SELECTED_FOLDER_ID = "selected_folder_id";

    /**
     * 主界面是否显示网格，默认true
     */
    public static final String PREF_IS_GRID_STYLE = "is_grid_style";
    /**
     * 笔记的排序方式
     */
    public static final String PREF_NOTE_SORT = "note_sort";

    /**
     * 设备是否已经上传了设备信息
     */
    public static final String PREF_IS_DEVICE_ACTIVE = "is_device_active";

    /**
     * 系统版本,如22、23
     */
    public static final String PREF_SDK_VERSION = "sdk_version";

    /**
     * 当前登录的账号类型，0：本账号系统登录，1：微信，2：QQ，3：微博
     */
    public static final String PREF_ACCOUNT_TYPE = "account_type";

    /**
     * 当前登录的账号在本地数据库中的id，0则没有使用本地登录
     */
    public static final String PREF_ACCOUNT_ID = "account_id";

    /**
     * 快速创建笔记的桌面小部件id
     */
    public static final String PREF_APPWIDGETID_SHORT_CREATE = "appwidgetid_short_create";
    public static final String PREF_APPWIDGETID_LIST = "appwidgetid_list";

    /**
     * 回车的标签
     */
    public static final String TAG_ENTER = "\r";
    
    /**
     * 回车的标签
     */
    public static final String TAG_NEXT_LINE = "\n";
    /**
     * 列表的标签
     */
    public static final String TAG_FORMAT_LIST = "- ";

    /**
     * 英文的逗号“,”
     */
    public static final String TAG_COMMA = ",";

    /**
     * 英文的分号";"
     */
    public static final String TAG_SEMICOLON = ";";

    /**
     * 缩进
     */
    public static final String TAG_INDENT = "\t";

    /**
     * 列表的标签的长度
     */
    public static final int FORMAT_LIST_TAG_LENGTH = TAG_FORMAT_LIST.length();

    public static final int OPT_ADD_NOTE = 1;
    public static final int OPT_UPDATE_NOTE = 2;
    public static final int OPT_REMOVE_NOTE_ATTACH = 3;
    public static final int OPT_LOAD_WIDGET_ITEMS = 4;
    public static final int OPT_INSTALL_APP = 5;

    public static final int SYNC_UP_NOTE = 1;
    public static final int SYNC_DOWN_NOTE = 2;
    public static final int SYNC_NOTE = 3;

    
    public static final String ARG_CORE_OPT = "arg_core_opt";
    public static final String ARG_CORE_OBJ = "arg_core_obj";
    public static final String ARG_CORE_LIST = "arg_core_list";
    public static final String ARG_SUB_OBJ = "arg_sub_obj";
    
    //是否包含回收站的内容
    public static final String ARG_ISRECYCLE = "isRecycle";
    public static final String ARG_ISFOLDER = "folderId";
    public static final String ARG_SORT = "sort";

    public static final String ATTACH_PREFIX = "attach";
    
    public static final String OS = "Android";
    
    public static final int MSG_SUCCESS = 1;
    public static final int MSG_SUCCESS2 = 2;
    public static final int MSG_FAILED = -1;
    
    //意见反馈的最大图片大小，2MB=1024 * 1024
    public static final long MAX_FEEDBACK_IMG_LENGTH = 1048576L;
    public static final int MAX_FEEDBACK_IMG_UNIT = 2;
    
}
