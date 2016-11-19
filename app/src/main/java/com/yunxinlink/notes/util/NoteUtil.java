package com.yunxinlink.notes.util;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.adapter.ShareListAdapter;
import com.yunxinlink.notes.api.model.NoteParam;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.appwidget.NoteListAppWidget;
import com.yunxinlink.notes.appwidget.ShortCreateAppWidget;
import com.yunxinlink.notes.model.AccountType;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.share.ShareInfo;
import com.yunxinlink.notes.share.ShareItem;
import com.yunxinlink.notes.share.SimplePlatformActionListener;
import com.yunxinlink.notes.sync.SyncCache;
import com.yunxinlink.notes.sync.SyncData;
import com.yunxinlink.notes.sync.SyncSettingState;
import com.yunxinlink.notes.sync.service.SyncService;
import com.yunxinlink.notes.ui.MainActivity;
import com.yunxinlink.notes.ui.NoteEditActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformDb;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.TitleLayout;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;

import static com.yunxinlink.notes.lockpattern.Alp.TAG;

/**
 * @author huanghui1
 * @update 2016/7/11 19:15
 * @version: 0.0.1
 */
public class NoteUtil {
    private NoteUtil() {}

    /**
     * 删除多条笔记,子线程中运行
     * @param noteList 要删除的笔记的集合
     */
    public static void handleDeleteNote(Context context, List<DetailNoteInfo> noteList, boolean hasDeleteOpt) {
        handleDeleteNote(context, noteList, hasDeleteOpt, false);
    }

    /**
     * 删除多条笔记，子线程中运行
     * @param noteList 要删除的笔记的集合
     * @param hasDeleteOpt 之前是否有删除操作，仅在删除到回收站是有效
     * @param realDelete 是否真正的删除，即在回收站中删除                    
     */
    public static void handleDeleteNote(final Context context, List<DetailNoteInfo> noteList, boolean hasDeleteOpt, final boolean realDelete) {
        if (noteList == null || noteList.size() == 0) {
            return;
        }
        if (!hasDeleteOpt || realDelete) {   //之前是否有删除操作，如果没有，则需弹窗
            final List<DetailNoteInfo> deleteList = new ArrayList<>(noteList);
            AlertDialog.Builder builder = buildDialog(context);
            int titleRes = 0;
            int msgRes = 0;
            if (realDelete) {   //彻底删除
                titleRes = R.string.real_delete;
                msgRes = R.string.tip_note_real_delete;
            } else {
                titleRes = R.string.action_delete;
                msgRes = R.string.confirm_to_trash;
            }
            builder.setTitle(titleRes)
                    .setMessage(msgRes)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doDeleteNote(deleteList, realDelete, context);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {    //直接删除
            doDeleteNote(noteList, false, context);
        }
    }

    /**
     * 删除多条笔记，子线程中运行
     * @param note 要删除的笔记
     */
    public static void handleDeleteNote(Context context, NoteInfo note, boolean hasDeleteOpt) {
        handleDeleteNote(context, note, hasDeleteOpt, false);
    }

    /**
     * 删除多条笔记，子线程中运行
     * @param note 要删除的笔记
     */
    public static void handleDeleteNote(Context context, NoteInfo note, boolean hasDeleteOpt, final boolean realDelete) {
        final List<DetailNoteInfo> list = new ArrayList<>();
        DetailNoteInfo detailNote = new DetailNoteInfo();
        detailNote.setNoteInfo(note);
        list.add(detailNote);
        handleDeleteNote(context, list, hasDeleteOpt, realDelete);
    }

    /**
     * 删除多条笔记，子线程中运行
     * @param noteList 要删除的笔记的集合
     * @param realDelete 是否彻底删除                
     */
    private static void doDeleteNote(final List<DetailNoteInfo> noteList, final boolean realDelete, final Context context) {
        DeleteState deleteState = null;
        if (realDelete) {    //彻底删除
            deleteState = DeleteState.DELETE_DONE;
        } else {
            deleteState = DeleteState.DELETE_TRASH;
        }
        doDeleteNote(noteList, deleteState, context);
    }

    /**
     * 删除多条笔记，子线程中运行
     * @param stateParam 笔记的删除状态
     * @param noteList 要删除的笔记的集合
     */
    public static void doDeleteNote(final List<DetailNoteInfo> noteList, DeleteState stateParam, final Context context) {
        final List<DetailNoteInfo> list = new ArrayList<>(noteList);
        SystemUtil.getThreadPool().execute(new NoteTask(list, stateParam, context) {
            @Override
            public void run() {
                DeleteState deleteState = (DeleteState) params[1];
                List<DetailNoteInfo> detailList = (List<DetailNoteInfo>) params[0];
                //需要同步笔记内容的列表
                List<DetailNoteInfo> syncContentList = new ArrayList<>();
                //只需要同步笔记状态的列表，不需要同步笔记的内容
                List<DetailNoteInfo> syncStateList = new ArrayList<>();
                for (DetailNoteInfo noteInfo : detailList) {
                    if (noteInfo.isSynced()) {
                        syncStateList.add(noteInfo);
                    } else {
                        syncContentList.add(noteInfo);
                    }
                }
                boolean success = NoteManager.getInstance().deleteNote(detailList, deleteState);
                if (success) {
                    notifyAppWidgetList(context);
                    //获取当前的用户
                    NoteApplication app = (NoteApplication) context.getApplicationContext();
                    User user = app.getCurrentUser();
                    if (user == null || !user.isAvailable()) {
                        KLog.d(TAG, "do delete note user is null or not available so don't sync");
                        return;
                    }
                    doSync(user, syncContentList, syncStateList, context);
                }
            }
        });
    }

    /**
     * 同步笔记，主要用户笔记删除或者还原
     * @param user 当前的用户
     * @param syncContentList 需要同步笔记内容的集合
     * @param syncStateList 只同步笔记状态的集合
     * @param context 上下文
     */
    private static void doSync(User user, List<DetailNoteInfo> syncContentList, List<DetailNoteInfo> syncStateList, Context context) {
        //开始同步笔记到服务器
        //如果有内容需要上传，则需分笔记本来同步
        if (!SystemUtil.isEmpty(syncContentList)) { //有需要上传内容的笔记
            KLog.d(TAG, "sync notes has some note need sync up content");
            //key 为folder sid ,value为DetailNoteInfo
            Map<String, List<DetailNoteInfo>> map = new HashMap<>();
            //默认的笔记本sid,主要是为没有归类的笔记准备的
            String defaultFolderSid = SystemUtil.generateFolderSid();
            for (DetailNoteInfo detailNoteInfo : syncContentList) {
                if (detailNoteInfo == null || detailNoteInfo.getNoteInfo() == null) {
                    continue;
                }
                NoteInfo noteInfo = detailNoteInfo.getNoteInfo();
                String folderSid = noteInfo.getSid();
                if (TextUtils.isEmpty(folderSid)) { //该笔记不属于任何笔记本，则使用默认的笔记
                    folderSid = defaultFolderSid;
                }
                List<DetailNoteInfo> list = map.get(folderSid);
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(detailNoteInfo);
            }
            //先上传没有笔记本的
            List<DetailNoteInfo> nofolderNotes = map.get(defaultFolderSid);
            if (!SystemUtil.isEmpty(nofolderNotes)) {    //有未归类的笔记
                NoteParam noteParam = new NoteParam();

                noteParam.setDetailNoteInfos(nofolderNotes);
                noteParam.setFolder(null);
                noteParam.setUserSid(user.getSid());

                //开始同步
                KLog.d(TAG, "start sync un folder notes with content:" + defaultFolderSid);
                startSyncUpNote(context, user, defaultFolderSid, noteParam);
                //从缓存中移除
                map.remove(defaultFolderSid);
            }
            Map<String, Folder> folderMap = FolderManager.getInstance().getFolders(user, null);
            if (!SystemUtil.isEmpty(map)) { //还有其他有归类的笔记需要同步内容
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    List<DetailNoteInfo> syncList = map.get(key);
                    if (SystemUtil.isEmpty(syncList)) {
                        continue;
                    }
                    Folder folder = folderMap.get(key);
                    if (folder == null) {
                        continue;
                    }

                    NoteParam noteParam = new NoteParam();

                    noteParam.setDetailNoteInfos(syncList);
                    noteParam.setFolder(folder);
                    noteParam.setUserSid(user.getSid());

                    //开始同步
                    KLog.d(TAG, "start sync folder notes with content:" + key);
                    startSyncUpNote(context, user, key, noteParam);
                }
                //清除缓存
                map.clear();
            }
        } else if (!SystemUtil.isEmpty(syncStateList)) {    //只同步笔记的状态

            String syncId = SystemUtil.generateNoteSid();

            NoteParam noteParam = new NoteParam();

            noteParam.setSyncScope(NoteParam.SYNC_STATE);
            noteParam.setDetailNoteInfos(syncStateList);
            noteParam.setFolder(null);
            noteParam.setUserSid(user.getSid());

            //开始同步
            KLog.d(TAG, "start sync folder notes with state:" + syncId);
            startSyncUpNote(context, user, syncId, noteParam);
        }
    }

    /**
     * 清空回收站
     */
    public static void handleClearTrash(Context context) {
        AlertDialog.Builder builder = buildDialog(context);
        builder.setTitle(R.string.title_note_clear_all)
                .setMessage(R.string.tip_note_clear_all)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SystemUtil.getThreadPool().execute(new NoteTask() {
                            @Override
                            public void run() {
                                NoteManager.getInstance().clearTrash();
                            }
                        });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }


    /**
     * 显示笔记详情
     * @param detailNoteInfo 笔记信息
     */
    public static void showInfo(Context context, final DetailNoteInfo detailNoteInfo) {
        NoteInfo note = detailNoteInfo.getNoteInfo();
        String info = detailNoteInfo.getNoteInfo(context);
        AlertDialog.Builder builder = buildDialog(context);
        builder.setTitle(note.getNoteTitle(false))
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * 格式化联系人的数据，格式为"name:number"
     * @param info 联系人信息的数据[0]:姓名，[1]:号码
     * @return 格式化后的文本内容
     */
    public static CharSequence formatContactInfo(String[] info) {
        String name = info[0];
        String number = info[1];
        return name + ":" + number;
    }

    /**
     * 在编辑框中插入文本
     * @param editText
     * @param text
     */
    public static void insertText(EditText editText, CharSequence text) {
        int selectionStart = editText.getSelectionStart();
        Editable editable = editText.getEditableText();
        editable.insert(selectionStart, text);
    }

    /**
     * 当请求权限失败后，给出对应的解释和提示
     * @param activity
     * @param permission 请求的权限
     * @param rationale 用户曾经拒绝过该权限，给予用户解释需要该权限的原因
     * @param failedMsg 用户拒绝该权限后的提示语
     */
    public static void onPermissionDenied(Activity activity, String permission, String rationale, String failedMsg) {
        //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                permission)) {
            if (!TextUtils.isEmpty(rationale)) {
                SystemUtil.makeLongToast(rationale);
            }
        } else {
            if (!TextUtils.isEmpty(failedMsg)) {
                SystemUtil.makeLongToast(failedMsg);
            }
            //进行权限请求
                    /*ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            EXTERNAL_STORAGE_REQ_CODE);*/
        }
    }

    /**
     * 当请求权限失败后，给出对应的解释和提示
     * @param activity
     * @param permission 请求的权限
     * @param rationaleRes 用户曾经拒绝过该权限，给予用户解释需要该权限的原因
     * @param failedRes 用户拒绝该权限后的提示语
     */
    public static void onPermissionDenied(Activity activity, String permission, int rationaleRes, int failedRes) {
        String rationale = null;
        if (rationaleRes != 0) {
            rationale = activity.getString(rationaleRes);
        }
        String failedMsg = null;
        if (failedRes != 0) {
            failedMsg = activity.getString(failedRes);
        }
        onPermissionDenied(activity, permission, rationale, failedMsg);
    }

    /**
     * 创建一个对话框
     * @param context
     * @return
     */
    public static AlertDialog.Builder buildDialog(Context context) {
        return new AlertDialog.Builder(context, R.style.NoteAlertDialogStyle);
    }

    /**
     * 处理普通文本的分享处理
     * @param  intent intent
     * @return 返回文本内容
     * @author tiger
     * @update 2015/11/14 10:44
     * @version 1.0.0
     */
    public static String handleSendText(Intent intent) {
        return intent.getStringExtra(Intent.EXTRA_TEXT);
    }

    /**
     * 处理分享过来的文件
     * @param intent intent
     * @return 文件的uri
     * @author tiger
     * @update 2015/11/15 10:41
     * @version 1.0.0
     */
    public static Uri handleSendFile(Intent intent) {
        return intent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    /**
     * 处理多文件分享
     * @param intent intent
     * @return 返回多文件的uri集合
     * @author tiger
     * @update 2015/11/15 10:46
     * @version 1.0.0
     */
    public static ArrayList<Uri> handleSendMultipleFiles(Intent intent) {
        return intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
    }

    /**
     * 显示分享对话框
     * @param context
     * @param shareInfo 分享的数据
     * @param isWebPage 分享的类型是否是链接、网址类型，网址类型就加上QQ和微信分享，否则，不加
     */
    public static void showShare(final Context context, final ShareInfo shareInfo, boolean isWebPage) {

        String[] titleArray = context.getResources().getStringArray(R.array.share_menu_items);
        int[] resArray = {R.drawable.ic_classic_sinaweibo, R.drawable.ic_classic_wechat, R.drawable.ic_classic_wechatmoments,
                R.drawable.ic_classic_qq, R.drawable.ic_classic_qzone, R.drawable.ic_more_horiz};
        String[] platforms = {SinaWeibo.NAME, Wechat.NAME, WechatMoments.NAME, QQ.NAME, QZone.NAME, ""}; 
        
        int size = titleArray.length;
        
        List<ShareItem> list = new ArrayList<>(size);
        
        for (int i = 0; i < size; i++) {
            ShareItem shareItem = new ShareItem(titleArray[i], resArray[i], platforms[i]);
            list.add(shareItem);
        }
        
        if (!isWebPage) {    //分享的不是是网址，则减去微信和QQ
            ShareItem shareItem = new ShareItem(Wechat.NAME);
            list.remove(shareItem);

            shareItem = new ShareItem(QQ.NAME);
            list.remove(shareItem);
        }

        final ShareListAdapter shareAdapter = new ShareListAdapter(list, context);

        final SimplePlatformActionListener platformActionListener = new SimplePlatformActionListener();

        AlertDialog.Builder builder = NoteUtil.buildDialog(context);
        builder.setTitle(R.string.action_share)
                .setAdapter(shareAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        
                        ShareItem item = (ShareItem) shareAdapter.getItem(which);
                        
                        String text = shareInfo.getText();
                        Platform.ShareParams sp = new Platform.ShareParams();
                        //2、设置分享内容
                        if (!TextUtils.isEmpty(shareInfo.getTitle())) { //分享标题
                            sp.setTitle(shareInfo.getTitle());
                        }
                        if (!TextUtils.isEmpty(text)) {  //分享文本
                            sp.setText(text);
                        }
                        if (!TextUtils.isEmpty(shareInfo.getImageUrl())) {  //网络图片rul
                            sp.setImageUrl(shareInfo.getImageUrl());
                        }
                        if (!TextUtils.isEmpty(shareInfo.getImagePath())) { //本地图片的路径，如果:/sdcard/xxx.png
                            sp.setImagePath(shareInfo.getImagePath());
                        }
                        boolean isMultiImage = false;
                        String[] imageArray = shareInfo.getImagePathArray();
                        if (imageArray != null && imageArray.length > 0) {
                            isMultiImage = true;
                            sp.setImageArray(imageArray);
                        }
                        if (!TextUtils.isEmpty(shareInfo.getTitleUrl())) {  //网友点进链接后，可以看到分享的详情,仅在人人网和QQ空间使用
                            sp.setTitleUrl(shareInfo.getTitleUrl());
                        }
                        if (!TextUtils.isEmpty(shareInfo.getUrl())) {   //网友点进链接后，可以看到分享的详情,仅在微信（包括好友和朋友圈）中使用
                            sp.setUrl(shareInfo.getUrl());
                        }
                        if (!TextUtils.isEmpty(shareInfo.getSiteUrl())) {   //siteUrl是分享此内容的网站地址，仅在QQ空间使用
                            sp.setSiteUrl(shareInfo.getSiteUrl());
                        }
                        if (!TextUtils.isEmpty(shareInfo.getFilePath())) {
                            sp.setFilePath(shareInfo.getFilePath());
                        }
                        if (!TextUtils.isEmpty(shareInfo.getSite())) {
                            sp.setSite(shareInfo.getSite());
                        }
                        String platformName = item.getPlatform();
                        Platform platform = null;
                        //3、非常重要：获取平台对象
                        if (!TextUtils.isEmpty(platformName)) {
                            platform = ShareSDK.getPlatform(platformName);
                            sp.setShareType(shareInfo.getShareType());//非常重要：一定要设置分享属性
                            sp.setSite(context.getString(R.string.app_name));   //site是分享此内容的网站名称，仅在QQ空间使用
                        }

//                        if (platformName.equals(SinaWeibo.NAME)) {  //新浪微博，不支持多图片
                            if (isMultiImage) { //多张图片，则取第一张
                                sp.setImagePath(imageArray[0]);
                            }
//                        }

                        if (platform != null) {
                            boolean hasClient = platform.isClientValid();
                            SystemUtil.makeShortToast(R.string.share_ing);
                            platform.setPlatformActionListener(platformActionListener); // 设置分享事件回调
                            // 执行分享
                            platform.share(sp);
                        } else {//更多，调用系统的分享
                            SystemUtil.shareText(context, text);
                        }
                    }
                })
                .setPositiveButton(R.string.share_cancel, null)
                .show();
    }

    /**
     * 分享笔记
     * @param detailNote
     */
    public static void shareNote(Context context, DetailNoteInfo detailNote) {
        NoteInfo note = detailNote.getNoteInfo();
        String text = note.getShowText();
        Attach lastAttach = detailNote.getLastAttach();
        ShareInfo shareInfo = new ShareInfo();
        String url = "http://www.yunxinlink.com/";
//                        shareInfo.setTitleUrl(url);
        shareInfo.setSite(SystemUtil.getAppName(context));
        shareInfo.setSiteUrl(url);
        int shareType = 0;
        if (!TextUtils.isEmpty(text)) {  //没有文本内容
            shareInfo.setText(text);
            shareType = Platform.SHARE_TEXT;
        }
        if (note.hasAttach() && note.getAttaches() != null && note.getAttaches().size() > 0) {
            Collection<Attach> attaches = note.getAttaches().values();
            List<String> images = new ArrayList<>();
            for (Attach att : attaches) {
                if (att.isImage()) {
                    images.add(att.getLocalPath());
                }
            }
            if (images.size() > 0) {    //有图片，则还添加图片
                if (images.size() == 1) {   //只有一张图片
                    shareInfo.setImagePath(images.get(0));
                } else {
                    String[] array = new String[images.size()];
                    shareInfo.setImagePathArray(images.toArray(array));
                }
                if (shareType == 0) {   //没有文本内容
                    shareType = Platform.SHARE_IMAGE;
                }
            } else if (lastAttach != null && !TextUtils.isEmpty(lastAttach.getLocalPath())) {
                if (shareType == 0) {   //没有文本内容
                    switch (lastAttach.getType()) {
                        case Attach.VOICE:
                            shareType = Platform.SHARE_MUSIC;
                            break;
                        case Attach.VIDEO:
                            shareType = Platform.SHARE_VIDEO;
                            break;
                        default:
                            shareType = Platform.SHARE_FILE;
                            break;
                    }
                }
                shareInfo.setFilePath(lastAttach.getLocalPath());
            }
        }
        shareInfo.setShareType(shareType);
        showShare(context, shareInfo, false);
    }

    /**
     * 初始化标题栏,自定义的web分享界面的标题
     */
    public static void initTitleView(final Activity activity, TitleLayout titleLayout) {

        activity.setTheme(R.style.AppTheme_NoActionBar);

//        titleLayout.removeAllViews();
        TextView textView = titleLayout.getTvTitle();

        CharSequence title = textView.getText();

        titleLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(activity);
        View view = inflater.inflate(R.layout.app_bar_layout, null);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        toolbar.setTitle(title);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        int homeRes = SystemUtil.getResourceId(activity, R.attr.homeAsUpIndicator);

        toolbar.setNavigationIcon(homeRes);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
            }
        });

        titleLayout.addView(view, params);
    }

    /**
     * 在通知栏上创建快捷方式
     */
    public static void createNotificationShortcut(Context context, int notifyId) {

        Intent addIntent = new Intent(context, NoteEditActivity.class);
        PendingIntent addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        addIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        addIntent.putExtra(NoteEditActivity.ARG_HAS_LOCK_CONTROLLER, false);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_nofitication_create);
        remoteViews.setOnClickPendingIntent(R.id.btn_plus, addPendingIntent);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Notification notification = builder.setAutoCancel(false)
                .setOngoing(true)
                .setContent(remoteViews)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        NotificationManagerCompat.from(context).notify(notifyId, notification);
    }

    /**
     * 取消状态栏的快捷方式
     */
    public static void cancelNotificationShortcut(Context context, int notifyId) {
        NotificationManagerCompat.from(context).cancel(notifyId);
    }

    /**
     * 获取快速创建小部件的id
     * @param context
     * @return
     */
    public static int[] getShortCreateAppWidgetId(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, ShortCreateAppWidget.class);
        return appWidgetManager.getAppWidgetIds(componentName);
    }

    /**
     * 获取桌面小部件笔记列表的id
     * @param context
     * @return
     */
    public static int[] getListAppWidgetId(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, NoteListAppWidget.class);
        return appWidgetManager.getAppWidgetIds(componentName);
    }

    /**
     * 通知appwidget来刷新列表
     * @param context
     */
    public static void notifyAppWidgetList(Context context) {
        int[] appWidgetIds = getListAppWidgetId(context);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            Intent intent = new Intent(NoteListAppWidget.ACTION_NOTIFY_CHANGE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds);
            context.sendBroadcast(intent);
            KLog.d("notifyAppWidgetList invoke appwidget id:" + appWidgetIds[0] + ", size:" + appWidgetIds.length);
        } else {
            KLog.d("notifyAppWidgetList invoke failed appwidget id");
        }
    }

    /**
     * 设备是否已经上传了设备信息
     * @param context
     * @return
     */
    public static boolean isDeviceActive(Context context) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        return preferences.getBoolean(Constants.PREF_IS_DEVICE_ACTIVE, false);
    }

    /**
     * 获取系统的版本号，如22、23
     * @param context
     * @return
     */
    public static int getSdkInt(Context context) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        return preferences.getInt(Constants.PREF_SDK_VERSION, 0);
    }

    /**
     * 系统是否升级了
     * @param context
     * @return
     */
    public static boolean isOsUpdated(Context context) {
        int sdkInt = getSdkInt(context);
        if (sdkInt != 0) {
            int curSdkInt = SystemUtil.getBuildSDKInt();
            return sdkInt == curSdkInt;
        }
        return true;
    }
    
    /**
     * 是否需要激活或者更新设备信息
     * @param context
     * @return
     */
    public static boolean shouldActive(Context context) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        boolean isActive = preferences.getBoolean(Constants.PREF_IS_DEVICE_ACTIVE, false);
        int sdkInt = preferences.getInt(Constants.PREF_SDK_VERSION, -1);
        //之前没有激活或者系统版本更了
        return !isActive || sdkInt != SystemUtil.getBuildSDKInt();
    }

    /**
     * 保存设备激活的信息
     * @param context
     * @param isActive
     */
    public static void saveDeviceActive(Context context, boolean isActive) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.PREF_IS_DEVICE_ACTIVE, isActive);
        editor.putInt(Constants.PREF_SDK_VERSION, SystemUtil.getBuildSDKInt());
        editor.apply();
    }

    /**
     * 获取用户登录的账号类型
     * @param context
     * @return
     */
    public static int getAccountType(Context context) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        return preferences.getInt(Constants.PREF_ACCOUNT_TYPE, -1);
    }

    /**
     * 保存用户登录的账号类型
     * @param context
     * @param type
     * @return
     */
    public static void saveAccountType(Context context, int type) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Constants.PREF_ACCOUNT_TYPE, type);
        editor.apply();
    }

    /**
     * 获取本地当前登录的用户id
     * @param context
     * @return
     */
    public static int getAccountId(Context context) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        return preferences.getInt(Constants.PREF_ACCOUNT_ID, 0);
    }

    /**
     * 保存用户登录的账号id
     * @param context
     * @param userId
     * @return
     */
    public static void saveAccountId(Context context, int userId) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Constants.PREF_ACCOUNT_ID, userId);
        editor.apply();
    }

    /**
     * 构建用户的登录的参数
     * @param context
     * @param user 用户参数，为空时，则可能是第三方账号登录或者本地登录
     * @param loginType 登录类型，是本地账号登录还是第三方账号登录
     * @see AccountType                 
     * @return
     */
    public static UserDto buildLoginParams(Context context, User user, Integer loginType) {
        UserDto userDto = new UserDto();

        int accountType = -1;
        if (user != null && TextUtils.isEmpty(user.getOpenUserId())) { //指定账号和密码登录，一般用于登录或者注册界面
            userDto.setType(AccountType.TYPE_LOCAL);
            userDto.setUser(user);
            KLog.d(TAG, "build login param login with local account");
            return userDto;
        }
        if (loginType == null) {
            accountType = NoteUtil.getAccountType(context);
        } else {
            accountType = loginType;
        }
        KLog.d(TAG, "build login param login with local open api account:" + accountType);
        userDto.setType(accountType);
        if (accountType > 0) { //用户有登录过，则校验信息
            //校验以及获取用户信息
            Platform platform = getPlatform(context, accountType);
            if (platform != null && platform.isAuthValid()) {
                PlatformDb platDB = platform.getDb();//获取数平台数据DB
                //通过DB获取各种数据
                String token = platDB.getToken();
                long expiresTime = platDB.getExpiresTime();
                String userId = platDB.getUserId();

                userDto.setExpiresTime(expiresTime);
                userDto.setOpenUserId(userId);
                userDto.setToken(token);

            } else {
                return null;
            }
        } else {    //则用默认的账号登录，如果账号存在的话
            KLog.d(TAG, "doAuthorityVerify user not use open api login will use local account login");
            int userId = NoteUtil.getAccountId(context);
            user = UserManager.getInstance().getAccountInfo(userId);
            if (user == null) { //本地没有账号
                KLog.d(TAG, "doAuthorityVerify user local account not exists ");
                return null;
            }
        }
        return userDto;
    }

    /**
     * 获取不同平台的数据
     * @param context
     * @param accountType
     * @return
     */
    public static Platform getPlatform(Context context, int accountType) {
        ShareSDK.initSDK(context);
        Platform platform = null;
        switch (accountType) {
            case AccountType.TYPE_QQ:   //QQ登录
                platform = ShareSDK.getPlatform(context, QQ.NAME);
                break;
            case AccountType.TYPE_WECHAT:   //微信登录
                platform = ShareSDK.getPlatform(context, Wechat.NAME);
                break;
            case AccountType.TYPE_WEIBO:   //微博登录
                platform = ShareSDK.getPlatform(context, SinaWeibo.NAME);
                break;
        }
        return platform;
    }

    /**
     * 移除分享平台的数据
     * @param context
     */
    private static void removeSharePlatform(Context context) {
        ShareSDK.initSDK(context);
        Platform[] platforms = ShareSDK.getPlatformList(context);
        if (platforms != null && platforms.length > 0) {
            for (Platform platform : platforms) {
                platform.removeAccount();
            }
        }
    }

    /**
     * 获取第三方账号的用户id
     * @param context
     * @param accountType
     * @return
     */
    public static String getOpenUserId(Context context, int accountType) {
        String userId = null;
        Platform platform = getPlatform(context, accountType);
        if (platform != null && platform.isAuthValid()) {
            PlatformDb platDB = platform.getDb();//获取数平台数据DB
            userId = platDB.getUserId();
        }
        return userId;
    }

    /**
     * 结束所有的界面
     * @param activity
     */
    public static void finishAll(Activity activity) {
        if (activity != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                activity.finishAffinity();
            } else {
                Intent home = new Intent(activity, MainActivity.class);
                home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.putExtra(MainActivity.ARG_EXIT, true);
                activity.startActivity(home);
            }
            activity.finish();
        }
    }

    /**
     * 清除本地的账号信息，一般用于账号退出登录
     * @param context
     */
    public static void clearAccountInfo(Context context) {
        KLog.d(TAG, "clear local pref account info");
        //移除分享平台的数据
        removeSharePlatform(context);
        //移除本地账户的数据
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Constants.PREF_ACCOUNT_TYPE);
        editor.remove(Constants.PREF_ACCOUNT_ID);
        editor.remove(Constants.SELECTED_FOLDER_ID);
        editor.remove(Constants.PREF_DEFAULT_FOLDER);
        editor.apply();
    }

    /**
     * 检查同步的状态
     * @return
     */
    public static SyncSettingState checkSyncSetting(Context context) {
        //检查网络是否可用
        int netType = SystemUtil.getConnectedType(context);
        if (netType == -1) { //网络不可用
//            SystemUtil.makeShortToast(R.string.tip_network_not_available);
            return SyncSettingState.NET_DISABLE;
        }
        // 如果不是wifi，则判断当前连接的是运营商的哪种网络2g、3g、4g等
        boolean isMobileNet = netType == ConnectivityManager.TYPE_MOBILE;
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        //是否自动同步
        boolean autoSync = preferences.getBoolean(context.getString(R.string.settings_key_sync_note_auto), context.getResources().getBoolean(R.bool.default_settings_key_sync_note_auto));
        if (autoSync) {
            //检测当前的网络环境
            //在数据流量环境下是否自动同步
            boolean syncDataMobile = preferences.getBoolean(context.getString(R.string.settings_key_sync_note_traffic), context.getResources().getBoolean(R.bool.default_settings_key_sync_note_traffic));
            if (!syncDataMobile && isMobileNet) {   //在移动数据下不同步，且当前使用的是移动网络数据
                KLog.d(TAG, "sync data but can not sync in mobile data and now net work type is mobile");
                return SyncSettingState.PROMPT;
            }
            return SyncSettingState.ENABLE;
        } else {
            return SyncSettingState.DISABLE;
        }
    }

    /**
     * 设置移动数据网络下是否同步笔记
     * @param enable
     */
    public static void setSyncMobile(Context context, boolean enable) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(context.getString(R.string.settings_key_sync_note_traffic), enable);
        editor.apply();
    }

    /**
     * 向上同步笔记
     * @param syncSid 同步的编号
     * @param noteParam 笔记的参数
     */
    public synchronized static void startSyncUpNote(Context context, User user, String syncSid, NoteParam noteParam) {
        if (user == null || !user.isAvailable() || noteParam == null) {  //用户不存在或者不可用
            KLog.d(TAG, "start sync up note but user is null or not available:" + user);
            return;
        }
        
        noteParam.setUserSid(user.getSid());
        
        SyncData syncData = new SyncData();
        syncData.setState(SyncData.SYNC_NONE);
        syncData.setSyncable(noteParam);
        SyncCache.getInstance().addOrUpdate(syncSid, syncData);
        
        Intent service = new Intent(context, SyncService.class);
        service.putExtra(Constants.ARG_CORE_OPT, Constants.SYNC_UP_NOTE);
        service.putExtra(Constants.ARG_CORE_OBJ, syncSid);
        context.startService(service);
    }

    /**
     * 开始向下同步笔记
     */
    public synchronized static void startSyncDownNote(Context context) {
        KLog.d(TAG, "start sync down note");
        startSyncNote(context, true);
    }

    /**
     * 开始同步笔记，先向下同步，后向上同步
     * @param context
     * @param onlyDown 是否只是向下同步
     */
    public synchronized static void startSyncNote(Context context, boolean onlyDown) {
        KLog.d(TAG, "start sync note");
        String syncSid = SystemUtil.generateSyncSid();

        if (SyncCache.getInstance().hasSyncData(syncSid)) {
            KLog.d(TAG, "start sync data but already hash sync data:" + syncSid);
            return;
        }
        Intent service = new Intent(context, SyncService.class);
        if (onlyDown) {
            service.putExtra(Constants.ARG_CORE_OPT, Constants.SYNC_DOWN_NOTE);
        } else {
            service.putExtra(Constants.ARG_CORE_OPT, Constants.SYNC_NOTE);
        }

        SyncData syncData = new SyncData();
        syncData.setState(SyncData.SYNC_NONE);
        SyncCache.getInstance().addOrUpdate(syncSid, syncData);

        service.putExtra(Constants.ARG_CORE_OBJ, syncSid);
        context.startService(service);
    }
}
