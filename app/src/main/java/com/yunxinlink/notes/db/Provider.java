package com.yunxinlink.notes.db;

import android.provider.BaseColumns;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/5 19:34
 */
public final class Provider {
    
    //其他公用的刷新界面的类型
    public static final int NOTIFY_FLAG = -1;

    private interface SyncColumns {
        /**
         * 主键，uuid策略
         */
        public static final String SID = "sId";

        /**
         * 该笔记的删除状态
         */
        public static final String DELETE_STATE = "delete_state";

        /**
         * 同步的的状态
         */
        public static final String SYNC_STATE = "sync_state";
    }
    
    /**
     * 笔记的数据量字段
     * @author tiger
     * @update 2016/3/5 19:37
     * @version 1.0.0
     */
    public static final class NoteColumns implements BaseColumns, SyncColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 1;

        /**
         * note表改变后更新文件夹的触发器
         */
        public static final String TRI_UPDATE_FOLDER = "tri_update_folder";
        /**
         * note表添加笔记后更新文件夹的触发器
         */
        public static final String TRI_INSERT_NOTE = "tri_insert_note";

        /**
         * note更新删除状态的文件夹中笔记的数量
         */
        public static final String TRI_NOTE_COUNT_ADD = "tri_note_count_add";

        /**
         * 文件夹中笔记减少的触发器
         */
        public static final String TRI_NOTE_COUNT_MINUS = "tri_note_count_minus";
        
        /**
         * sid的索引
         */
        public static final String NOTE_ID_IDX = "note_id_idx";
        
        public static final String TABLE_NAME = "notes";

        /**
         * 用户的id,int类型
         */
        public static final String USER_ID = "user_id";

        /**
         * 文件夹的id
         */
        public static final String FOLDER_ID = "folder_id";

        /**
         * 笔记标题
         */
        public static final String TITLE = "title";

        /**
         * 笔记内容
         */
        public static final String CONTENT = "content";

        /**
         * 实际显示的内容，仅用于本地数据库
         */
        public static final String SHOW_CONTENT = "show_content";

        /**
         * 笔记的种类，主要是文本和清单
         */
        public static final String KIND = "kind";

        /**
         * 笔记是否有附件,0表示没有，1表示有
         */
        public static final String HAS_ATTACH = "has_attach";

        /**
         * 笔记的提醒时间
         */
        public static final String REMIND_TIME = "remind_time";

        /**
         * 笔记的提醒id
         */
        public static final String REMIND_ID = "remind_id";

        /**
         * 笔记的创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 笔记的修改时间
         */
        public static final String MODIFY_TIME = "modify_time";

        /**
         * 之前的笔记内容
         */
        public static final String OLD_CONTENT = "old_content";

        /**
         * 文本的hash值
         */
        public static final String HASH = "hash";
        
        /**
         * 默认时间降序排列
         */
        public static final String DEFAULT_SORT = MODIFY_TIME + " desc";

    }

    /**
     * 笔记的文件夹
     * @author tiger
     * @update 2016/3/6 8:53
     * @version 1.0.0
     */
    public static final class FolderColumns implements BaseColumns, SyncColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 2;
        
        /**
         * sid的索引
         */
        public static final String FOLDER_ID_IDX = "folder_id_idx";

        /**
         * 插入数据后设置排序的触发器
         */
        public static final String TRI_SET_FOLDER_SORT = "tri_set_folder_sort";

        /**
         * 将文件夹移到回收站的触发器
         */
        public static final String TRI_TRASH_FOLDER = "tri_trash_folder";

        /**
         * 将文件夹移出回收站
         */
        public static final String TRI_UNTRASH_FOLDER = "tri_untrash_folder";

        public static final String TABLE_NAME = "folder";

        /**
         * 用户的id
         */
        public static final String USER_ID = "user_id";

        /**
         * 文件夹的名称
         */
        public static final String NAME = "name";

//        /**
//         * 默认选中显示的文件夹,1为默认选中的，0为没有选中的
//         */
//        public static final String DEFAULT_FOLDER = "default_folder";

        /**
         * 是否被锁定,1表示锁定，0表示没有锁定
         */
        public static final String IS_LOCK = "is_lock";

//        /**
//         * 是否隐藏的，1表示隐藏，0表示没有隐藏
//         */
//        public static final String IS_HIDDEN = "is_hidden";

        /**
         * 排序
         */
        public static final String SORT = "sort";

        /**
         * 创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 修改时间
         */
        public static final String MODIFY_TIME = "modify_time";

        /**
         * 默认的排序，升序
         */
        public static final String DEFAULT_SORT = SORT + " asc";

    }
    
    /**
     * 附件表
     * @author tiger
     * @update 2016/3/6 9:36
     * @version 1.0.0
     */
    public static final class AttachmentColumns implements BaseColumns, SyncColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 3;
        
        /**
         * sid的索引
         */
        public static final String ATTACH_ID_IDX = "attach_id_idx";
        
        public static final String TABLE_NAME = "attachment";

        /**
         * 笔记的id
         */
        public static final String NOTE_ID = "noteId";

        /**
         * 所属的用户id
         */
        public static final String USER_ID = "user_id";

        /**
         * 文件的类型
         */
        public static final String TYPE = "type";

        /**
         * 本地存储的路径
         */
        public static final String LOCAL_PATH = "local_path";

        /**
         * 文件名
         */
        public static final String FILE_NAME = "file_name";

        /**
         * 文件的大小
         */
        public static final String SIZE = "size";

        /**
         * 附件的描述
         */
        public static final String DESCRIPTION = "description";

        /**
         * 服务器的路径
         */
        public static final String SERVER_PATH = "server_path";

        /**
         * 创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 修改时间
         */
        public static final String MODIFY_TIME = "modify_time";

        /**
         * 文件的mime类型
         */
        public static final String MIME_TYPE = "mime_type";
    }

    /**
     * 清单表
     * @author tiger
     * @update 2016/3/6 9:56
     * @version 1.0.0
     */
    public static final class DetailedListColumns implements BaseColumns, SyncColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 4;
        
        /**
         * sid的索引
         */
        public static final String DETAILEDLIST_ID_IDX = "detailedList_id_idx";
        
        public static final String TABLE_NAME = "detailed_list_item";

        /**
         * 用户的id
         */
        public static final String USER_ID = "user_id";

        /**
         * 笔记的id
         */
        public static final String NOTE_ID = "note_id";

        /**
         * 清单的标题
         */
        public static final String TITLE = "title";

        /**
         * 历史标题
         */
        public static final String OLD_TITLE = "old_title";

        /**
         * 排序
         */
        public static final String SORT = "sort";

        /**
         * 上一次的排序
         */
        public static final String OLD_SORT = "old_sort";

        /**
         * 是否选中
         */
        public static final String CHECKED = "checked";

        /**
         * 创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 修改时间
         */
        public static final String MODIFY_TIME = "modify_time";

        /**
         * 清单的hash
         */
        public static final String HASH = "hash";

        /**
         * 默认的排序
         */
        public static final String DEFAULT_SORT = SORT + " ASC";
    }

    /**
     * 手写
     * @author tiger
     * @update 2016/3/6 10:07
     * @version 1.0.0
     */
    public static final class HandWriteColumns implements BaseColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 5;
        
        public static final String TABLE_NAME = "hand_write";

        /**
         * 附件id
         */
        public static final String ATTACH_ID = "attach_id";

        /**
         * 手写文件的本地存储路径
         */
        public static final String LOCAL_PATH = "local_path";

        /**
         * 涂鸦文件的大小，并非涂鸦后生成的图片
         */
        public static final String SIZE = "size";
        /**
         * 创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 修改时间
         */
        public static final String MODIFY_TIME = "modify_time";

    }

    /**
     * 时间提醒
     * @author ]
     * @update 2016/3/6 10:25
     * @version 1.0.0
     */
    public static final class RemindersColumns implements BaseColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 6;
        
        public static final String TABLE_NAME = "reminders";

        /**
         * 提醒时间
         */
        public static final String REMIND_TIME = "remind_time";

        /**
         * 是否已经提醒过了
         */
        public static final String IS_REMINDED = "is_reminded";

        /**
         * 创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 修改时间
         */
        public static final String MODIFY_TIME = "modify_time";
    }

    /**
     * 用户表
     * @author tiger
     * @update 2016/3/6 10:27
     * @version 1.0.0
     */
    public static final class UserColumns implements BaseColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 7;

        /**
         * 用户名索引
         */
        public static final String USERNAME_IDX = "username_idx";

        public static final String TABLE_NAME = "user";

        /**
         * 用户的唯一id，手动生成，由服务器端生成
         */
        public static final String SID = "sid";

        /**
         * 用户名
         */
        public static final String USERNAME = "username";

        /**
         * 密码
         */
        public static final String PASSWORD = "password";

        /**
         * 手机号
         */
        public static final String MOBILE = "mobile";

        /**
         * 邮箱
         */
        public static final String EMAIL = "email";

        /**
         * 访问服务器的token
         */
        public static final String ACCESS_TOKEN = "access_token";

        /**
         * 用户头像
         */
        public static final String AVATAR = "avatar";

        /**
         * 用户性别
         */
        public static final String GENDER = "gender";

        /**
         * 用户的状态，0：可用，1：停用
         */
        public static final String STATE = "state";

        /**
         * 账户创建时间
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * 账户修改时间
         */
        public static final String MODIFY_TIME = "modify_time";

        /**
         * 最后同步的时间
         */
        public static final String LAST_SYNC_TIME = "last_sync_time";

        /**
         * 第三方账号的用户id
         */
        public static final String OPEN_USER_ID = "open_user_id";

        /**
         * 用户头像的hash值
         */
        public static final String AVATAR_HASH = "avatar_hash";

        /**
         * 用户昵称
         */
        public static final String NICKNAME = "nickname";
    }

    /**
     * 桌面小部件的表
     */
    public static final class WidgetColumns implements BaseColumns {

        public static final String TABLE_NAME = "widget";
        /**
         * 标题，名称
         */
        public static final String TITLE = "title";

        /**
         * 类型
         */
        public static final String TYPE = "type";

        /**
         * 排序
         */
        public static final String SORT = "sort";

        /**
         * 排序方式2，主要用于列表的标题栏的图标排序
         */
        public static final String SORT2 = "sort2";
        
        public static final String DEFAULT_SORT = SORT + " ASC";
        
        public static final String DEFAULT_SORT2 = SORT2 + " ASC";
    }

    /**
     * 用户的设置表
     * @author tiger
     * @update 2016/3/6 10:36
     * @version 1.0.0
     */
    public static final class UserSettingsColumns implements BaseColumns {

        /**
         * 通知标识，主要是区分别的通知内容
         */
        public static final int NOTIFY_FLAG = 8;
        
        public static final String TABLE_NAME = "user_settings";
    }
}
