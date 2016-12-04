package com.yunxinlink.notes.widget;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.richtext.AttachSpec;
import com.yunxinlink.notes.ui.HandWritingActivity;
import com.yunxinlink.notes.ui.NoteEditActivity;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;
import com.yunxinlink.notes.util.log.Log;

import java.io.File;

/**
 * 附件的span
 * @author huanghui1
 * @update 2016/7/7 21:34
 * @version: 0.0.1
 */
public class AttachSpan extends ClickableSpan {

    private static final java.lang.String TAG = "AttachSpan";
    
    private AttachSpec attachSpec = new AttachSpec();

    public AttachSpec getAttachSpec() {
        return attachSpec;
    }

    public void setAttachSpec(AttachSpec attachSpec) {
        this.attachSpec = attachSpec;
    }

    public int getAttachType() {
        return attachSpec.attachType;
    }

    public void setAttachType(int attachType) {
        this.attachSpec.attachType = attachType;
    }

    public String getFilePath() {
        return attachSpec.filePath;
    }

    public void setFilePath(String filePath) {
        this.attachSpec.filePath = filePath;
    }

    public String getAttachId() {
        return attachSpec.sid;
    }

    public void setAttachId(String attachId) {
        this.attachSpec.sid = attachId;
    }

    public CharSequence getText() {
        return attachSpec.text;
    }

    public void setText(CharSequence text) {
        this.attachSpec.text = text;
    }
    
    public void setNoteSid(String sid) {
        this.attachSpec.noteSid = sid;
    }

    public int getSelStart() {
        return attachSpec.start;
    }

    public void setSelStart(int selStart) {
        this.attachSpec.start = selStart;
    }

    public int getSelEnd() {
        return attachSpec.end;
    }

    public void setSelEnd(int selEnd) {
        this.attachSpec.end = selEnd;
    }


    public String getMimeType() {
        return attachSpec.mimeType;
    }

    public void setMimeType(String mimeType) {
        this.attachSpec.mimeType = mimeType;
    }

    /**
     * Performs the click action associated with this span.
     */
    @Override
    public void onClick(View widget) {
        Context context = widget.getContext();
        onClick(context);
    }

    /**
     * 处理点击事件
     * @param context
     */
    public void onClick(Context context) {
        Log.d(TAG, "--AttachSpan--onClick----");
        if (TextUtils.isEmpty(attachSpec.filePath)) {
            SystemUtil.makeShortToast(R.string.tip_file_not_download);
            KLog.d(TAG, "attach span file path is empty");
        } else {
            if (!FileUtil.isFileExists(attachSpec.filePath)) {
                SystemUtil.makeShortToast(R.string.tip_file_not_exists);
                KLog.d(TAG, "attach span file path is not exists:" + attachSpec.filePath);
            } else {
                doAction(context, attachSpec.filePath);
            }
        }
    }

    /**
     * 响应点击事件
     * @param context 上下文
     * @param filePath 文件的本地全路径
     */
    public void doAction(Context context, String filePath) {
        MenuItem menuItem = new MenuItem();
        switch (attachSpec.attachType) {
            case Attach.IMAGE:  //图片
                menuItem.menuRes = R.array.image_menu_items;
                handleAttach(context, menuItem, filePath);
                break;
            case Attach.PAINT:  //绘画
                menuItem.menuRes = R.array.paint_menu_items;
                handlePaint(context, menuItem, filePath);
                break;
            case Attach.VOICE:  //录音
                menuItem.menuRes = R.array.voice_menu_items;
                handleAttach(context, menuItem, filePath);
                break;
            default:    //文件
                menuItem.menuRes = R.array.file_menu_items;
                handleAttach(context, menuItem, filePath);
                break;
        }
    }

    /**
     * 根据链接的类型来获取对应的图标
     * @return 返回图标的资源id
     */
    public int getActionIconRes() {
        int res = 0;
        switch (attachSpec.attachType) {
            case Attach.IMAGE:  //图片
                res = R.drawable.ic_action_photo;
                break;
            case Attach.PAINT:  //绘画
                res = R.drawable.ic_action_brush;
                break;
            case Attach.VOICE:  //录音
                res = R.drawable.ic_audiotrack;
                break;
            case Attach.ARCHIVE:  //压缩文件
                res = R.drawable.ic_archive;
                break;
            case Attach.VIDEO:  //视频
                res = R.drawable.ic_play_arrow;
                break;
            default:    //文件
                res = R.drawable.ic_action_insert_file;
                break;

        }
        return res;
    }

    /**
     * 显示菜单
     * @param context 上下文
     * @param menuItem 菜单数据
     * @param onClickListener 每一项的点击事件
     */
    private void showMenu(Context context, MenuItem menuItem, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = NoteUtil.buildDialog(context);
        builder.setTitle(menuItem.title)
                .setItems(menuItem.menuRes, onClickListener)
                .show();
    }

    /**
     * 处理附件的菜单事件
     * @param context
     * @param menuItem
     * @param filePath
     */
    private void handleAttach(final Context context, MenuItem menuItem, final String filePath) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //打开
                        SystemUtil.openFile(context, filePath, attachSpec.attachType, attachSpec.mimeType);
                        break;
                    case 1: //分享
                        SystemUtil.shareFile(context, filePath, attachSpec.attachType, attachSpec.mimeType);
                        break;
                    case 2: //详情
                        showInfo(context, filePath, attachSpec.mimeType);
                        break;
                }
            }
        });
    }

    /**
     * 显示附件
     * @param context
     */
    public void showAttach(Context context) {
        switch (attachSpec.attachType) {
            case Attach.PAINT:  //绘画
                editPaint(context);
                break;
            default:    //文件
                openFile(context);
                break;
        }
    }

    /**
     * 打开文件
     * @param context
     */
    private void openFile(Context context) {
        SystemUtil.openFile(context, attachSpec.filePath, attachSpec.attachType, attachSpec.mimeType);
    }

    /**
     * 处理绘画的菜单
     * @param context
     * @param menuItem
     * @param filePath
     */
    private void handlePaint(final Context context, MenuItem menuItem, final String filePath) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //打开
                        SystemUtil.openFile(context, filePath, attachSpec.attachType, attachSpec.mimeType);
                        break;
                    case 1: //编辑
                        editPaint(context);
                        break;
                    case 2: //分享
                        SystemUtil.shareFile(context, filePath, attachSpec.attachType, attachSpec.mimeType);
                        break;
                    case 3: //详情
                        showInfo(context, filePath, attachSpec.mimeType);
                        break;
                }
            }
        });
    }

    /**
     * 编辑涂鸦
     * @param context
     */
    private void editPaint(Context context) {
        Intent intent = new Intent(context, HandWritingActivity.class);
        intent.putExtra(Constants.ARG_CORE_OBJ, attachSpec);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.startActivityForResult(intent, NoteEditActivity.REQ_EDIT_PAINT);
        } else {
            context.startActivity(intent);
            Log.d(TAG, "---handlePaint----context---is---not---activity---");
        }
    }

    /**
     * 显示文件的详细新
     * @param filePath
     */
    private void showInfo(Context context, String filePath, String mimeType) {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        String colon = context.getString(R.string.colon);
        sb.append(context.getString(R.string.path)).append(colon).append(Constants.TAG_INDENT).append(filePath).append(Constants.TAG_NEXT_LINE);
        if (!TextUtils.isEmpty(mimeType)) {
            sb.append(context.getString(R.string.note_type)).append(colon).append(Constants.TAG_INDENT).append(mimeType).append(Constants.TAG_NEXT_LINE);
        }
        sb.append(context.getString(R.string.size)).append(colon).append(Constants.TAG_INDENT).append(SystemUtil.formatFileSize(file.length())).append(Constants.TAG_NEXT_LINE)
                .append(context.getString(R.string.modify_time)).append(colon).append(Constants.TAG_INDENT).append(TimeUtil.formatTime(file.lastModified(), TimeUtil.PATTERN_FILE_TIME));
        AlertDialog.Builder builder = NoteUtil.buildDialog(context);
        builder.setTitle(R.string.action_info)
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    class MenuItem {
        String title;
        int menuRes;
    }

    @Override
    public String toString() {
        return "AttachSpan{" +
                "attachSpec=" + attachSpec +
                "} " + super.toString();
    }
}
