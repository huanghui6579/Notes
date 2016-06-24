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
            mCbLock.setChecked(mFolder.isLock());
//            mCbDefault.setChecked(mFolder.isDefault());
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
     * 保存文件夹
     * @author huanghui1
     * @update 2016/6/24 14:27
     * @version: 1.0.0
     */
    private void saveFolder() {
        if (!TextUtils.isEmpty(mEtName.getText())) {
            SystemUtil.getThreadPool().execute(new Runnable() {
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
                        mFolder.setUserId(getCurrentUserId());
                        mFolder = FolderManager.getInstance().addFolder(mFolder);
                        if (mFolder == null) {
                            Log.w(TAG, "---saveFolder----addFolder----error--");
                        }
                    } else {    //更新
                        mFolder.setName(name);
                        mFolder.setModifyTime(time);
                        mFolder.setIsLock(isLock);
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
