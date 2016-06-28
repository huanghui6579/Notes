package net.ibaixin.notes.ui;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.persistent.FolderManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.Log;

/**
 * 文件夹编辑、添加界面
 * @author huanghui1
 * @update 2016/6/24 9:54
 * @version: 1.0.0
 */
public class FolderEditActivity extends BaseActivity implements View.OnClickListener {
    
    private EditText mEtName;
    private ViewGroup mLockLayout;
    private ViewGroup mDefaultLayout;
    
    private CheckBox mCbLock;
    private CheckBox mCbDefault;
    
    private Folder mFolder;
    
    //是否是添加操作
    private boolean mIsAdd;

    @Override
    protected int getContentView() {
        return R.layout.activity_folder_edit;
    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        mFolder = intent.getParcelableExtra(Constants.ARG_CORE_OBJ);
        if (mFolder == null || mFolder.getSId() == null) {  //新建
            mIsAdd = true;
        } else {    //编辑，则显示已有的数据
            mEtName.setText(mFolder.getName());
            mEtName.requestFocus();
            mCbLock.setChecked(mFolder.isLock());
            mCbDefault.setChecked(isDefaultFolder(mFolder));
        }
    }

    private boolean isDefaultFolder(Folder folder) {
        String defaultSid = getDefaultFolderSid();
        if (TextUtils.isEmpty(defaultSid)) {
            return false;
        } else {
            String sid = folder.getSId();
            return defaultSid.equals(sid);
        }
    }

    @Override
    protected void initView() {
        mEtName = (EditText) findViewById(R.id.et_name);
        mLockLayout = (ViewGroup) findViewById(R.id.lock_layout);
        mDefaultLayout = (ViewGroup) findViewById(R.id.default_layout);

        mCbLock = (CheckBox) mLockLayout.findViewById(R.id.check);
        mCbDefault = (CheckBox) mDefaultLayout.findViewById(R.id.check);

        TextView lockTitleView = (TextView) mLockLayout.findViewById(R.id.tv_title);
        TextView lockSummaryView = (TextView) mLockLayout.findViewById(R.id.tv_summary);

        lockTitleView.setText(R.string.folder_title_lock);
        lockSummaryView.setText(R.string.folder_summary_lock);

        TextView defaultTitleView = (TextView) mDefaultLayout.findViewById(R.id.tv_title);
        TextView defaultSummaryView = (TextView) mDefaultLayout.findViewById(R.id.tv_summary);

        defaultTitleView.setText(R.string.folder_title_hide);
        defaultSummaryView.setText(R.string.folder_summary_hide);

        mLockLayout.setOnClickListener(this);
        mDefaultLayout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lock_layout:  //加锁的项
                mCbLock.toggle();
                break;
            case R.id.default_layout:  //默认文件夹的项
                mCbDefault.toggle();
                break;
        }
    }

    /**
     * 是否需要保存，如果没有修改则
     * @return
     */
    private boolean isNeedSave() {
        if (!mIsAdd) {  //更新的
            String oldName = mFolder.getName();
            boolean isOldLock = mFolder.isLock();
            String oldDefaultSid = getDefaultFolderSid();
            String name = mEtName.getText().toString();
            boolean modifyName = !oldName.equals(name);
            boolean modifyLock;
            if (isOldLock) {
                modifyLock = !mCbLock.isChecked();
            } else {
                modifyLock = mCbLock.isChecked();
            }
            boolean modifyDefault;
            if (TextUtils.isEmpty(oldDefaultSid)) {
                modifyDefault = mCbDefault.isChecked();
            } else {
                String sid = mFolder.getSId();
                if (mCbDefault.isChecked()) {
                    modifyDefault = !oldDefaultSid.equals(sid);
                } else {
                    modifyDefault = oldDefaultSid.equals(sid);
                }
            }
            return modifyName || modifyLock || modifyDefault;
        } else {
            return true;
        }
    }
    
    /**
     * 保存文件夹
     * @author huanghui1
     * @update 2016/6/24 14:27
     * @version: 1.0.0
     */
    private void saveFolder() {
        if (!TextUtils.isEmpty(mEtName.getText()) && isNeedSave()) {
            
            doInbackground(new Runnable() {
                @Override
                public void run() {
                    String name = mEtName.getText().toString();
                    boolean isLock = mCbLock.isChecked();
                    boolean isDefault = mCbDefault.isChecked();
                    long time = System.currentTimeMillis();
                    if (mIsAdd) {   //添加的
                        mFolder = new Folder();
                        mFolder.setName(name);
                        mFolder.setSId(SystemUtil.generateFolderSid());
                        mFolder.setCreateTime(time);
                        mFolder.setModifyTime(time);
                        mFolder.setIsLock(isLock);
                        mFolder.setDefault(isDefault);
                        mFolder.setUserId(getCurrentUserId());
                        mFolder = FolderManager.getInstance().addFolder(mFolder);
                        if (mFolder == null) {
                            Log.w(TAG, "---saveFolder----addFolder----error--");
                        }
                    } else {    //更新
                        mFolder.setName(name);
                        mFolder.setModifyTime(time);
                        mFolder.setIsLock(isLock);
                        mFolder.setDefault(isDefault);
                        boolean result = FolderManager.getInstance().updateFolder(mFolder);
                        if (result) {
                            Log.w(TAG, "---saveFolder----updateFolder----error--");
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        saveFolder();
        super.onDestroy();
    }
}
