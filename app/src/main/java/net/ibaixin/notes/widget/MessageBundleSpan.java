package net.ibaixin.notes.widget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.text.style.URLSpan;
import android.view.View;

import net.ibaixin.notes.R;
import net.ibaixin.notes.util.NoteLinkify;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/3/13 10:54
 */
public class MessageBundleSpan extends URLSpan {
    private static final String TAG = "MessageBundleSpan";

    protected int urlType = 0;
    
    private CharSequence text;

    public MessageBundleSpan(String url) {
        super(url);
    }

    public MessageBundleSpan(Parcel src) {
        super(src);
    }

    public void setUrlType(int urlType) {
        this.urlType = urlType;
    }

    public int getUrlType() {
        return urlType;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    @Override
    public void onClick(View widget) {
        Context context = widget.getContext();
        try {
            doAction(context, getURL());
        } catch (ActivityNotFoundException e) {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
            Log.w("URLSpan", "Actvity was not found for intent");
        }
    }

    /**
     * 执行各种链接的点击操作
     */
    public void doAction(Context context, String url) {
        MenuItem menuItem = new MenuItem();
        switch (urlType) {
            case NoteLinkify.WEB_URLS:  //网页链接
                menuItem.menuRes = R.array.url_menu_items;
                handleUrl(context, menuItem, url);
                break;
            case NoteLinkify.MAP_ADDRESSES: //地图显示
                menuItem.menuRes = R.array.map_menu_items;
                handleUrl(context, menuItem, url);
                break;
            case NoteLinkify.PHONE_NUMBERS: //电话号码
                menuItem.menuRes = R.array.tel_menu_items;
                handleTel(context, menuItem, url);
                break;
            case NoteLinkify.EMAIL_ADDRESSES:   //发送邮件
                menuItem.menuRes = R.array.mail_menu_items;
                handleEmail(context, menuItem, url);
                break;
            default:
                viewUrl(context, url);
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
     * 处理链接的点击事件
     * @param context 上下文
     * @param menuItem 菜单
     * @param url 链接
     */
    private void handleUrl(final Context context, MenuItem menuItem, final String url) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //复制
                        SystemUtil.copyText(context, getText(), true);
                        break;
                    case 1: //浏览器打开
                        viewUrl(context, url);
                        break;
                    case 2: //转发
                        shareUrl(context, getText());
                        break;
                }
            }
        });
    }

    /**
     * 处理电话号码的点击事件
     * @param context 上下文
     * @param menuItem 菜单
     * @param url 链接
     */
    private void handleTel(final Context context, final MenuItem menuItem, final String url) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //复制
                        SystemUtil.copyText(context, getText(), true);
                        break;
                    case 1: //呼叫
                        call(context, url);
                        break;
                    case 2: //添加到联系人
                        menuItem.title = context.getString(R.string.action_contact_add);
                        menuItem.menuRes = R.array.tel_menu_items_add;
                        showContactMenu(context, menuItem, getText() == null ? "" : getText().toString());
                        break;
                    case 3: //转发
                        shareUrl(context, getText());
                        break;
                }
            }
        });
    }

    /**
     * 处理邮箱地址的点击事件
     * @param context 上下文
     * @param menuItem 菜单
     * @param url 链接
     */
    private void handleEmail(final Context context, final MenuItem menuItem, final String url) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //复制
                        SystemUtil.copyText(context, getText(), true);
                        break;
                    case 1: //发送邮件
                        sendEmail(context, url);
                        break;
                    case 2: //转发
                        shareUrl(context, getText());
                        break;
                }
            }
        });
    }

    /**
     * 显示添加到联系人的子菜单
     * @param context 上下文
     * @param menuItem 菜单
     * @param phoneNumber 链接
     */
    private void showContactMenu(final Context context, MenuItem menuItem, final String phoneNumber) {
        showMenu(context, menuItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: //创建新联系人
                        addNewContact(context, phoneNumber);
                        break;
                    case 1: //添加到现有联系人
                        addOrEditContact(context, phoneNumber);
                        break;
                }
            }
        });
    }
    
    /**
     * 添加号码到新的联系人
     * @param context 上下文
     * @param phoneNumber 电话号码
     */
    private void addNewContact(Context context, String phoneNumber) {
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
        context.startActivity(intent);
    }

    /**
     * 将号码添加到现有联系人中
     * @param context 上下文
     * @param phoneNumber 电话号码
     */
    private void addOrEditContact(Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
        context.startActivity(intent);
    }

    /**
     * 处理对应的url
     * @param context 上下文
     * @param url 链接
     */
    private void viewUrl(Context context, String url) {
        Uri uri = Uri.parse(url);
//        Uri uri = Uri.parse("http://www.google.com"); //浏览器 
//        Uri uri =Uri.parse("tel:1232333"); //拨号程序 
//        Uri uri=Uri.parse("geo:39.899533,116.036476"); //打开地图定位 
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {    //没有对应的程序处理
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 拨打电话
     * @param context 上下文
     * @param url 链接
     */
    private void call(Context context, String url) {
        Uri uri = Uri.parse(url);
//        Uri uri = Uri.parse("http://www.google.com"); //浏览器 
//        Uri uri =Uri.parse("tel:1232333"); //拨号程序 
//        Uri uri=Uri.parse("geo:39.899533,116.036476"); //打开地图定位 
        Intent intent = new Intent(Intent.ACTION_DIAL, uri);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 分享url
     * @param context 上下文
     * @param url 链接
     */
    private void shareUrl(Context context, CharSequence url) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, url);
        sendIntent.setType("text/plain");
        if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(sendIntent);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 发送邮件
     * @param context 上下文
     * @param url 链接
     */
    private void sendEmail(Context context, String url) {
        Intent sendIntent = new Intent();
        String[] to = {getText() == null ? "" : getText().toString()};
        sendIntent.setData(Uri.parse(url));
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, to);
        sendIntent.setType("message/rfc822");
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
