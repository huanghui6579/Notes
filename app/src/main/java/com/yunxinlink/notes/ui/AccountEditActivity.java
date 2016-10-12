package com.yunxinlink.notes.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.listener.SimpleTextWatcher;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.TaskParam;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;

/**
 * 用户信息编辑修改的界面
 */
public class AccountEditActivity extends BaseActivity implements View.OnClickListener {
    
    private static final int OPT_SAVE_IMG = 1;
    private static final int OPT_SHOW_IMG = 2;
    private static final int OPT_SAVE_INFO = 3;
    
    private static final int REQ_TAKE_PIC = 1;
    private static final int REQ_PICK_IMG = 2;
    private static final int REQ_CROP_IMG = 3;
    
    private ImageView mIvIcon;
    private TextView mTvTitle;
    private TextView mTvSummary;
    private EditText mEtNickname;
    //头像的文件
    private File mIconFile;
    
    protected ProgressDialog mProgressDialog;
    
    private UserObserver mUserObserver;

    @Override
    protected int getContentView() {
        return R.layout.activity_account_edit;
    }

    @Override
    protected void initData() {
        registerObserver();
        
        User user = getCurrentUser();
        KLog.d(TAG, "account edit init data user:" + user);
        
        showAccount(user);

        //添加监听器
        mEtNickname.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mTvTitle.setText(s);
            }
        });

        //请求sd卡的读写权限
        requestSdCardPermission();
        //网络加载用户信息，让信息与本地保持一致
        UserApi.syncUserInfo(user);
    }

    @Override
    protected void initView() {
        mIvIcon = (ImageView) findViewById(R.id.iv_icon);
        mTvTitle = (TextView) findViewById(R.id.tv_title);
        mTvSummary = (TextView) findViewById(R.id.tv_summary);
        mEtNickname = (EditText) findViewById(R.id.et_nickname);
        mIvIcon.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:  //保存用户信息
                saveAccountInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_icon:  //头像
                showIconMenu();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterObserver();
        super.onDestroy();
    }

    /**
     * 请求sd卡的权限
     */
    private void requestSdCardPermission() {
        final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {

            @Override
            public void onGranted() {
                KLog.d(TAG, "account edit activity sd card permission granted" + permissions[0]);
            }

            @Override
            public void onDenied(String permission) {
                KLog.d(TAG, "account edit activity on denied permission:" + permission);
                if (permissions[0].equals(permission)) {
                    NoteUtil.onPermissionDenied(AccountEditActivity.this, permission, R.string.tip_mkfile_error, R.string.tip_grant_write_storage_failed);
                }
            }
        });
    }

    /**
     * 显示用户
     * @param user
     */
    private void showAccount(User user) {
        if (user != null && !TextUtils.isEmpty(user.getAccount())) {
            String account = user.getAccount();
            mTvSummary.setText(account);
            String nickname = user.getNickname();
            if (TextUtils.isEmpty(nickname)) {
                if (SystemUtil.isEmail(account)) {  //邮箱
                    nickname = account.substring(0, account.indexOf("@"));
                }
            }
            mTvTitle.setText(nickname);
            mEtNickname.setText(nickname);
            String iconPath = user.getAvatar();
            if (!TextUtils.isEmpty(iconPath)) {
                ImageUtil.displayImage(iconPath, mIvIcon, ImageUtil.getAvatarOptions(mContext), null);
            }
        }
    }

    /**
     * 注册
     */
    private void registerObserver() {
        if (mUserObserver == null) {
            mUserObserver = new UserObserver(new Handler());
        }
        UserManager.getInstance().addObserver(mUserObserver);
    }

    /**
     * 注销用户的观察者
     */
    private void unregisterObserver() {
        if (mUserObserver != null) {
            UserManager.getInstance().removeObserver(mUserObserver);
        }
    }

    /**
     * 是否有保存提示，如果头像改动或者昵称改动，则用户返回时需要提示是否保存
     * @return
     */
    private boolean hasSaveTip() {
        User user = getCurrentUser();
        boolean hasTip = false;
        if (user != null) {
            String newNickname = mEtNickname.getText() == null ? null : mEtNickname.getText().toString();
            String oldNickname = user.getNickname();
            if (oldNickname != null && !oldNickname.equals(newNickname)) {  //需要保存
                hasTip = true;
            } else if (oldNickname == null && newNickname != null) {
                hasTip = true;
            }
            if (mIconFile != null) {
                hasTip = true;
            }
        }
        KLog.d(TAG, "account edit save account info has tip:" + hasTip);
        return hasTip;
    }

    /**
     * 显示保存信息的提示对话框
     */
    private void showSaveDialog() {
        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setTitle(R.string.account_save_tip_title)
                .setMessage(R.string.account_save_tip_content)
                .setPositiveButton(R.string.action_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveAccountInfo();
                    }
                })
                .setNegativeButton(R.string.account_save_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    @Override
    protected void onBack() {
        if (hasSaveTip()) { //有保存提示
            showSaveDialog();
        } else {
            super.onBack();
        }
    }

    @Override
    public void onBackPressed() {
        if (hasSaveTip()) { //有保存提示
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 显示菜单的对话框
     */
    private void showIconMenu() {
        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setItems(R.array.account_edit_icon_items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                switch (which) {
                    case 0: //拍照
                        takePic();
                        break;
                    case 1: //从相册中选择
                        pickImg();
                        break;
                }
            }
        }).show();
    }

    /**
     * 从相册中选择
     */
    private void pickImg() {
        SystemUtil.choseImage((Activity) mContext, REQ_PICK_IMG);
    }

    /**
     * 拍照
     */
    private void takePic() {
        final User user = getCurrentUser();
        if (user == null) {
            KLog.d(TAG, "account edit activity tak pic but user is null");
            return;
        }
        final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {
            @Override
            public void onGranted() {
                try {
                    mIconFile = SystemUtil.getAvatarFile(user.getSid());
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//create a intent to take picture 
                    //create a intent to take picture  
                    Uri uri = Uri.fromFile(mIconFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri); // set the image file name 
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent, REQ_TAKE_PIC);
                    } else {
                        SystemUtil.makeShortToast(R.string.tip_no_app_handle);
                    }
                } catch (Exception e) {
                    KLog.d(TAG, "account edit activity tak pic error:" + e.getMessage());
                }
            }

            @Override
            public void onDenied(String permission) {
                KLog.d(TAG, "account edit activity on denied permission:" + permission);
                if (permissions[0].equals(permission)) {
                    NoteUtil.onPermissionDenied(AccountEditActivity.this, permission, R.string.tip_mkfile_error, R.string.tip_grant_write_storage_failed);
                }
            }
        });
        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_TAKE_PIC:  //拍照
                    if (mIconFile != null) {
                        //file uri
                        Uri fileUri = Uri.fromFile(mIconFile);
                        cropImg(fileUri);
//                        saveAndShowImg(fileUri);
                    } else {
                        mIconFile = null;
                    }
                    break;
                case REQ_PICK_IMG:  //图片选择
                    if (data != null) {
                        final Uri uri = data.getData();
                        cropImg(uri);
//                        saveAndShowImg(uri);
                    } else {
                        mIconFile = null;
                    }
                    break;
                case REQ_CROP_IMG:  //裁剪的图片
                    if (data != null && data.getExtras() != null) {
                        Bundle extras = data.getExtras();
                        Bitmap photo = extras.getParcelable("data");
                        if (photo != null) {
                            saveAndShowImg(photo);
                        } else {
                            mIconFile = null;
                        }
                    } else {
                        mIconFile = null;
                    }
                    break;
            }
        } else {
            mIconFile = null;
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 条用系统相册来裁剪图片
     */
    private void cropImg(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        //可以选择图片类型，如果是*表明所有类型的图片
        intent.setDataAndType(uri, "image/*");
        // 下面这个crop = true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例，这里设置的是正方形（长宽比为1:1）
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        int size = Constants.AVATAR_THUMB_WIDTH;
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", size);
        intent.putExtra("outputY", size);
        //裁剪时是否保留图片的比例，这里的比例是1:1
        intent.putExtra("scale", true);
        //设置输出的格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
        //是否将数据保留在Bitmap中返回
        intent.putExtra("return-data", true);
        startActivityForResult(intent, REQ_CROP_IMG);
    }

    /**
     * 裁剪、保存并且显示图片
     * @param bitmap 裁剪后的图片
     */
    private void saveAndShowImg(final Bitmap bitmap) {
        final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {

            @Override
            public void onGranted() {
                TaskParam param = new TaskParam();
                param.optType = OPT_SAVE_IMG;
                param.data = bitmap;
                new LoadImgTask(OPT_SAVE_IMG).execute(param);
            }

            @Override
            public void onDenied(String permission) {
                KLog.d(TAG, "account edit activity on denied permission:" + permission);
                if (permissions[0].equals(permission)) {
                    NoteUtil.onPermissionDenied(AccountEditActivity.this, permission, R.string.tip_mkfile_error, R.string.tip_grant_write_storage_failed);
                }
            }
        });
        
    }

    /**
     * 保存图片
     * @param bitmap
     */
    private File saveImg(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        User user = getCurrentUser();
        if (user == null) {
            KLog.d(TAG, "account edit save img user is null");
            return null;
        }
        File iconFile = null;
        try {
            if (mIconFile == null) {
                mIconFile = SystemUtil.getAvatarFile(user.getSid());
            }
            if (mIconFile == null) {
                KLog.d(TAG, "account edit save img icon file is null");
                return null;
            }
            String savePath = mIconFile.getAbsolutePath();
            boolean success = ImageUtil.saveBitmap(bitmap, mIconFile, Bitmap.CompressFormat.PNG, 80);
            KLog.d(TAG, "account edt save img result:" + success + ", file path:" + savePath);
            if (success) {
                ImageUtil.clearMemoryCache(savePath);
                iconFile = mIconFile;
            }
        } catch (Exception e) {
            KLog.e(TAG, "account edit icon process img error:" + e.getMessage());
        }
        return iconFile;
    }

    /**
     * 构建保存用户信息的参数
     * @return
     */
    private void saveAccountInfo() {
        User localUser = getCurrentUser();
        if (localUser == null) {    //用户没有登录
            KLog.d(TAG, "save account info failed because current user is not login");
            return;
        }
        User user = (User) localUser.clone();
        if (user == null) {
            user = new User();
        }
        String nickname = mEtNickname.getText() == null ? null : mEtNickname.getText().toString();
        user.setNickname(nickname);
        user.setSid(localUser.getSid());
        user.setId(localUser.getId());
        if (mIconFile != null && mIconFile.exists()) {
            user.setAvatar(mIconFile.getAbsolutePath());
        } else {
            user.setAvatar(localUser.getAvatar());
            user.setAvatarHash(localUser.getAvatarHash());
        }
        TaskParam param = new TaskParam();
        param.optType = OPT_SAVE_INFO;
        param.data = user;
        mProgressDialog = showLoadingDialog(R.string.save_ing);
        new LoadImgTask(OPT_SAVE_INFO).execute(param);
    }

    /**
     * 保存信息到本地数据库
     * @param user
     */
    private boolean doSaveAccountInfo(User user) {
        if (user == null) {
            KLog.d(TAG, "do save account info failed because user is null");
            return false;
        }
        String iconPath = user.getAvatar();
        if (!TextUtils.isEmpty(iconPath)) { //存在头像，则生成头像文件的hash
            String iconHash = DigestUtil.md5FileHex(iconPath);
            if (!TextUtils.isEmpty(iconHash)) {
                user.setAvatarHash(iconHash);
            }
        }
        user.setSyncState(SyncState.SYNC_UP.ordinal());
        boolean success = UserManager.getInstance().update(user);
        if (success) {
            UserApi.syncUserInfo(user);
        }
        return success;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        KLog.i(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    /**
     * 用户信息的观察者
     */
    private class UserObserver extends ContentObserver {

        public UserObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.UserColumns.NOTIFY_FLAG:  //用户信息
                    switch (notifyType) {
                        case ADD:
                        case UPDATE:
                        case REFRESH:   //用户信息更新
                            User user = (User) data;
                            KLog.d(TAG, "account edit observer user refresh :" + user);
                            showAccount(user);
                            break;
                    }
                    break;
            }
        }
    }
    
    /**
     * 加载处理图片的后台任务
     * @author huanghui1
     * @update 2016/9/28 18:23
     * @version: 1.0.0
     */
    class LoadImgTask extends AsyncTask<TaskParam, Void, TaskParam> {
        
        private int optType;

        public LoadImgTask(int optType) {
            this.optType = optType;
        }

        @Override
        protected TaskParam doInBackground(TaskParam... params) {
            TaskParam param = params[0];
            File iconFile = null;
            TaskParam result = new TaskParam();
            result.optType = optType;
            switch (optType) {
                case OPT_SAVE_IMG:  //保存图片
                    Bitmap bitmap = (Bitmap) param.data;
                    iconFile = saveImg(bitmap);
                    result.data = iconFile;
                    KLog.d(TAG, "account edit load img iconFile:" + iconFile);
                    break;
                case OPT_SAVE_INFO: //保存用户信息
                    User user = (User) param.data;
                    result.data = doSaveAccountInfo(user);
                    break;
            }
            return result;
        }

        @Override
        protected void onPostExecute(TaskParam result) {
            if (result == null) {
                return;
            }
            switch (optType) {
                case OPT_SAVE_IMG:  //保存图片
                    File file = (File) result.data;
                    if (file != null && file.exists()) {    //文件存在，则显示图片
                        ImageUtil.displayImage(file.getAbsolutePath(), mIvIcon, ImageUtil.getAvatarOptions(mContext), null);
                    }
                    break;
                case OPT_SAVE_INFO: //保存用户信息
                    dismissDialog(mProgressDialog);
                    Boolean success = (Boolean) result.data;
                    int tipRes = 0;
                    if (success) {
                        tipRes = R.string.save_success;
                        finish();
                    } else {
                        tipRes = R.string.save_failed;
                    }
                    SystemUtil.makeShortToast(tipRes);
                    break;
            }
        }
    }
    
}
