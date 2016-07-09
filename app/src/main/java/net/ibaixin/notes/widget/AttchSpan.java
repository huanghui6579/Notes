package net.ibaixin.notes.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.style.ClickableSpan;
import android.view.View;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

import java.io.File;

/**
 * 附件的span
 * @author huanghui1
 * @update 2016/7/7 21:34
 * @version: 0.0.1
 */
public class AttchSpan extends ClickableSpan {

    private static final java.lang.String TAG = "AttchSpan";
    /**
     * 附件的类型
     */
    protected int attachType = 0;

    /**
     * 附件的本地全路径
     */
    protected String filePath;

    /**
     * 附件的id
     */
    private String attachId;

    /**
     * 内容
     */
    private CharSequence text;

    public int getAttachType() {
        return attachType;
    }

    public void setAttachType(int attachType) {
        this.attachType = attachType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getAttachId() {
        return attachId;
    }

    public void setAttachId(String attachId) {
        this.attachId = attachId;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    /**
     * Performs the click action associated with this span.
     */
    @Override
    public void onClick(View widget) {
        Context context = widget.getContext();
        doAction(context, filePath);
        Log.d(TAG, "--AttchSpan--onClick----");
    }

    /**
     * 响应点击事件
     * @param context 上下文
     * @param filePath 文件的本地全路径
     */
    public void doAction(Context context, String filePath) {
        MenuItem menuItem = new MenuItem();
        switch (attachType) {
            case Attach.IMAGE:  //图片
                menuItem.menuRes = R.array.image_menu_items;
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
    
    private void handleAttach(final Context context, MenuItem menuItem, final String filePath) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //打开
                        switch (attachType) {
                            case Attach.IMAGE:  //图片类型
                                SystemUtil.openImage(context, filePath);
                                break;
                        }
                        break;
                    case 1: //分享
                        shareImage(context, filePath);
                        break;
                    case 2: //详情
                        
                        break;
                }
            }
        });
    }

    /**
     * 分享图片
     * @param context
     * @param filePath
     */
    private void shareImage(Context context, String filePath) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        sendIntent.setType("image/*");
        if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(sendIntent);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    class MenuItem {
        String title;
        int menuRes;
    }
}
