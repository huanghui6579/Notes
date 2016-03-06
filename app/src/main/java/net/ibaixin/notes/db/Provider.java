package net.ibaixin.notes.db;

import android.provider.BaseColumns;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/5 19:34
 */
public final class Provider {

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
        public static final String TABLE_NAME = "notes";

        /**
         * 用户的id,int类型
         */
        public static final String USER_ID = "user_id";

        /**
         * 文件夹的id
         */
        public static final String FOLDER_ID = "folderId";

        /**
         * 笔记内容
         */
        public static final String CONTENT = "content";

        /**
         * 笔记的种类，主要是文本和清单
         */
        public static final String KIND = "kind";

        /**
         * 笔记是否有附件
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
    }

    /**
     * 笔记的文件夹
     * @author tiger
     * @update 2016/3/6 8:53
     * @version 1.0.0
     */
    public static final class FolderColumns implements BaseColumns, SyncColumns {
        public static final String TABLE_NAME = "folder";

        /**
         * 文件夹的名称
         */
        public static final String NAME = "name";

        /**
         * 默认选中显示的文件夹
         */
        public static final String DEFAULT_FOLDER = "default_folder";

        /**
         * 是否被锁定
         */
        public static final String IS_LOCK = "is_lock";

        /**
         * 是否隐藏的
         */
        public static final String IS_HIDDEN = "is_hidden";

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

    }
    
    /**
     * 附件表
     * @author tiger
     * @update 2016/3/6 9:36
     * @version 1.0.0
     */
    public static final class AttachmentColumns implements BaseColumns, SyncColumns {
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
        public static final String DECRIPTION = "decription";

        /**
         * 服务器的路径
         */
        public static final String SERVER_PATH = "server_path";
    }

    /**
     * 清单表
     * @author tiger
     * @update 2016/3/6 9:56
     * @version 1.0.0
     */
    public static final class DetailedListColumns implements BaseColumns, SyncColumns {
        public static final String TABLE_NAME = "detailed_list_item";

        /**
         * 用户的id
         */
        public static final String user_id = "user_id";

        /**
         * 清单的标题
         */
        public static final String TITLE = "title";

        /**
         * 历史标题
         */
        public static final String TITLE_OLD = "title_old";

        /**
         * 排序
         */
        public static final String SORT = "sort";

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
    }

    /**
     * 手写
     * @author tiger
     * @update 2016/3/6 10:07
     * @version 1.0.0
     */
    public static final class HandWriteColumns implements BaseColumns {
        public static final String TABLE_NAME = "hand_write";

        /**
         * 附件id
         */
        public static final String ATTACH_ID = "attach_id";

        /**
         * 手写文件的本地存储路径
         */
        public static final String local_path = "local_path";

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
        public static final String TABLE_NAME = "user";

        /**
         * 用户名
         */
        public static final String USERNAME = "username";

        /**
         * 密码
         */
        public static final String PASSWORD = "password";

        /**
         * 访问服务器的token
         */
        public static final String ACCESS_TOKEN = "access_token";

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
    }

    /**
     * 用户的设置表
     * @author tiger
     * @update 2016/3/6 10:36
     * @version 1.0.0
     */
    public static final class UserSettingsColumns implements BaseColumns {
        public static final String TABLE_NAME = "user_settings";
    }
}
