package net.ibaixin.notes.widget;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.text.style.ClickableSpan;
import android.view.View;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.richtext.AttachSpec;
import net.ibaixin.notes.ui.HandWritingActivity;
import net.ibaixin.notes.ui.NoteEditActivity;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.TimeUtil;
import net.ibaixin.notes.util.log.Log;

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

    /**
     * Performs the click action associated with this span.
     */
    @Override
    public void onClick(View widget) {
        Context context = widget.getContext();
        doAction(context, attachSpec.filePath);
        Log.d(TAG, "--AttachSpan--onClick----");
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
            case Attach.PAINT:  //绘画
                menuItem.menuRes = R.array.paint_menu_items;
                handlePaint(context, menuItem, filePath);
                break;
            case Attach.VOICE:  //录音
                menuItem.menuRes = R.array.voice_menu_items;
                handleAttach(context, menuItem, filePath);
                break;
        }
    }

    /**
     * 显示菜单
     * @param context 上下文
     * @param menuItem 菜单数据
     * @param onClickListener 每一项的点击事件
     */
    private void showMenu(Context context, MenuItem menuItem, DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
                        SystemUtil.openFile(context, filePath, attachSpec.attachType);
                        break;
                    case 1: //分享
                        SystemUtil.shareFile(context, filePath, attachSpec.attachType);
                        break;
                    case 2: //详情
                        showInfo(context, filePath);
                        break;
                }
            }
        });
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
                        SystemUtil.openFile(context, filePath, attachSpec.attachType);
                        break;
                    case 1: //编辑
                        Intent intent = new Intent(context, HandWritingActivity.class);
                        intent.putExtra(Constants.ARG_CORE_OBJ, attachSpec);
                        
                        if (context instanceof Activity) {
                            Activity activity = (Activity) context;
                            activity.startActivityForResult(intent, NoteEditActivity.REQ_EDIT_PAINT);
                        } else {
                            context.startActivity(intent);
                            Log.d(TAG, "---handlePaint----context---is---not---activity---");
                        }
//                        SystemUtil.openFile(context, filePath, attachType);
                        break;
                    case 2: //分享
                        SystemUtil.shareFile(context, filePath, attachSpec.attachType);
                        break;
                    case 3: //详情
                        showInfo(context, filePath);
                        break;
                }
            }
        });
    }

    /**
     * 显示文件的详细新
     * @param filePath
     */
    private void showInfo(Context context, String filePath) {
        StringBuilder sb = new StringBuilder();
        File file = new File(filePath);
        String colon = context.getString(R.string.colon);
        sb.append(context.getString(R.string.path)).append(colon).append(Constants.TAG_INDENT).append(filePath).append(Constants.TAG_NEXT_LINE)
                .append(context.getString(R.string.size)).append(colon).append(Constants.TAG_INDENT).append(SystemUtil.formatFileSize(file.length())).append(Constants.TAG_NEXT_LINE)
                .append(context.getString(R.string.modify_time)).append(colon).append(Constants.TAG_INDENT).append(TimeUtil.formatTime(file.lastModified(), TimeUtil.PATTERN_FILE_TIME));
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.action_info)
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    class MenuItem {
        String title;
        int menuRes;
    }
}
