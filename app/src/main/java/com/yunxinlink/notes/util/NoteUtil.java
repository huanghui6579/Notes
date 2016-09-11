package com.yunxinlink.notes.util;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.adapter.ShareListAdapter;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.share.ShareInfo;
import com.yunxinlink.notes.share.ShareItem;
import com.yunxinlink.notes.share.SimplePlatformActionListener;
import com.yunxinlink.notes.ui.MainActivity;
import com.yunxinlink.notes.ui.NoteEditActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.TitleLayout;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.wechat.friends.Wechat;
import cn.sharesdk.wechat.moments.WechatMoments;

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
    public static void handleDeleteNote(Context context, List<DetailNoteInfo> noteList, boolean hasDeleteOpt, final boolean realDelete) {
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
                            doDeleteNote(deleteList, realDelete);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {    //直接删除
            doDeleteNote(noteList, false);
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
    private static void doDeleteNote(final List<DetailNoteInfo> noteList, final boolean realDelete) {
        final List<DetailNoteInfo> list = new ArrayList<>(noteList);
        SystemUtil.getThreadPool().execute(new NoteTask(list, realDelete) {
            @Override
            public void run() {
                DeleteState deleteState = null;
                boolean deleteOpt = (boolean) params[1];
                if (deleteOpt) {    //彻底删除
                    deleteState = DeleteState.DELETE_DONE;
                } else {
                    deleteState = DeleteState.DELETE_TRASH;  
                }
                NoteManager.getInstance().deleteNote((List<DetailNoteInfo>) params[0], deleteState);
            }
        });
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
     * @param note
     */
    public static void showInfo(Context context, final NoteInfo note) {
        String info = note.getNoteInfo(context);
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
}
