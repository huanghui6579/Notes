package com.yunxinlink.notes.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.listener.SimpleTextWatcher;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.FileUtil;
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
    
    private static final int REQ_TAKE_PIC = 1;
    private static final int REQ_PICK_IMG = 2;
    
    private ImageView mIvIcon;
    private TextView mTvTitle;
    private TextView mTvSummary;
    private EditText mEtNickname;
    //头像的文件
    private File mIconFile;

    @Override
    protected int getContentView() {
        return R.layout.activity_account_edit;
    }

    @Override
    protected void initData() {
        User user = getCurrentUser();
        showAccount(user);

        //添加监听器
        mEtNickname.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mTvTitle.setText(s);
            }
        });
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_icon:  //头像
                showIconMenu();
                break;
        }
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
                        saveAndShowImg(fileUri);
                    }
                    break;
                case REQ_PICK_IMG:  //图片选择
                    if (data != null) {
                        final Uri uri = data.getData();
                        saveAndShowImg(uri);
                    }
                    break;
            }
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 裁剪、保存并且显示图片
     * @param uri
     */
    private void saveAndShowImg(final Uri uri) {
        final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {

            @Override
            public void onGranted() {
                new LoadImgTask(OPT_SAVE_IMG).execute(uri);
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
     * @param uri
     */
    private File saveImg(Uri uri) {
        if (uri == null) {
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
            String filePath = SystemUtil.getFilePathFromContentUri(uri.toString(), mContext);
            KLog.d(TAG, "account edit load img task file:----" + filePath);
            KLog.d(TAG, "account edit save file:----" + mIconFile.getAbsolutePath());
            //压缩并保存文件
            String savePath = mIconFile.getAbsolutePath();
            String savePathTmp = savePath + ".tmp";
            boolean success = ImageUtil.generateThumbImage(filePath, new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_WIDTH), savePathTmp, false);

            if (success) {
                File file = new File(savePathTmp);
                File srcFile = new File(filePath);
                if (srcFile.length() < file.length()) { //压缩后的文件比原始文件还大，则取原始文件
                    KLog.d(TAG, "account edit thumb img is large than original img so copy original img to--->" + savePathTmp);
                    //复制文件
                    FileUtil.copyFile(srcFile, file);
                }
                File renameFile = new File(savePath);
                if (renameFile.exists()) {
                    renameFile.delete();
                }
                file.renameTo(renameFile);
                iconFile = renameFile;

                ImageUtil.clearMemoryCache(savePath);

                //添加到相册
                SystemUtil.galleryAddPic(mContext, file);
            }
        } catch (Exception e) {
            KLog.e(TAG, "account edit icon process img error:" + e.getMessage());
        }
        return iconFile;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        KLog.i(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }
    
    /**
     * 加载处理图片的后台任务
     * @author huanghui1
     * @update 2016/9/28 18:23
     * @version: 1.0.0
     */
    class LoadImgTask extends AsyncTask<Uri, Void, File> {
        
        private int optType;

        public LoadImgTask(int optType) {
            this.optType = optType;
        }

        @Override
        protected File doInBackground(Uri... params) {
            File iconFile = null;
            switch (optType) {
                case OPT_SAVE_IMG:  //保存图片
                    Uri uri = params[0];
                    iconFile = saveImg(uri);
                    KLog.d(TAG, "account edit load img iconFile:" + iconFile);
                    break;
            }
            return iconFile;
        }

        @Override
        protected void onPostExecute(File file) {
            switch (optType) {
                case OPT_SAVE_IMG:  //保存图片
                    if (file != null && file.exists()) {    //文件存在，则显示图片
                        ImageUtil.displayImage(file.getAbsolutePath(), mIvIcon, ImageUtil.getAvatarOptions(mContext), null);
                    }
                    break;
            }
        }
    }
    
}
