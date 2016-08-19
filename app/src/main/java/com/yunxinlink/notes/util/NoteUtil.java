package com.yunxinlink.notes.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.adapter.ShareListAdapter;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.share.ShareInfo;
import com.yunxinlink.notes.share.ShareItem;
import com.yunxinlink.notes.share.SimplePlatformActionListener;

import java.util.ArrayList;
import java.util.List;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
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
     * 删除多条笔记
     * @param noteList 要删除的笔记的集合
     */
    public static void handleDeleteNote(Context context, final List<DetailNoteInfo> noteList, boolean hasDeleteOpt) {
        if (!hasDeleteOpt) {   //之前是否有删除操作，如果没有，则需弹窗           
            AlertDialog.Builder builder = buildDialog(context);
            builder.setTitle(R.string.prompt)
                    .setMessage(R.string.confirm_to_trash)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doDeleteNote(noteList);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {    //直接删除
            doDeleteNote(noteList);
        }
    }

    /**
     * 删除多条笔记
     * @param note 要删除的笔记
     */
    public static void handleDeleteNote(Context context, NoteInfo note, boolean hasDeleteOpt) {
        final List<DetailNoteInfo> list = new ArrayList<>();
        DetailNoteInfo detailNote = new DetailNoteInfo();
        detailNote.setNoteInfo(note);
        list.add(detailNote);
        handleDeleteNote(context, list, hasDeleteOpt);
    }

    /**
     * 删除多条笔记
     * @param noteList 要删除的笔记的集合
     */
    private static void doDeleteNote(final List<DetailNoteInfo> noteList) {
        final List<DetailNoteInfo> list = new ArrayList<>(noteList);
        SystemUtil.getThreadPool().execute(new NoteTask(list) {
            @Override
            public void run() {
                NoteManager.getInstance().deleteNote((List<DetailNoteInfo>) params[0]);
            }
        });
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
                        String platformName = item.getPlatform();
                        Platform platform = null;
                        //3、非常重要：获取平台对象
                        if (!TextUtils.isEmpty(platformName)) {
                            platform = ShareSDK.getPlatform(platformName);
                            sp.setShareType(shareInfo.getShareType());//非常重要：一定要设置分享属性
                            sp.setSite(context.getString(R.string.app_name));   //site是分享此内容的网站名称，仅在QQ空间使用
                        }
                        
                        if (platformName.equals(SinaWeibo.NAME)) {  //新浪微博，不支持多图片
                            if (isMultiImage) { //多张图片，则取第一张
                                sp.setImagePath(imageArray[0]);
                            }
                        }
                        
                        if (platform != null) {
                            SystemUtil.makeShortToast(R.string.share_ing);
                            platform.setPlatformActionListener(platformActionListener); // 设置分享事件回调
                            // 执行分享
                            platform.share(sp);
                        } else {//更多，调用系统的分享
                            SystemUtil.shareText(context, text);
                        }
                    }
                })
                .setNegativeButton(R.string.share_cancel, null)
                .show();
    }
}
