package com.yunxinlink.notes.ui.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.helper.AdapterRefreshHelper;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.widget.LayoutManagerFactory;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.yunxinlink.notes.util.Constants.MAX_FEEDBACK_IMG_LENGTH;

/**
 * 问题反馈或者建议页面
 * @author huanghui-iri
 * @update 2016/11/15 14:42
 * @version: 1.0.0
 */
public class FeedbackActivity extends BaseActivity {
    
    private static final int REQ_PICK_IMAGE = 1;
    
    private static final int MSG_FILE_NOT_EXISTS = 100;
    private static final int MSG_FILE_ADD_SUCCESS = 101;
    private static final int MSG_FILE_TOO_LARGE = 102;
    
    private final int MAX_IMG_SIZE = 3;
    
    private EditText mEtContent;
    
    private RecyclerView mRecyclerView;
    
    private EditText mEtContactWay;
    
    //图片的列表，最多3张图片，每张大小不超过2M
    private List<String> mImgList;
    
    private ImgAdapter mAdapter;
    
    private Handler mHandler = new MyHandler(this);

    @Override
    protected int getContentView() {
        return R.layout.activity_feedback;
    }

    @Override
    protected void initData() {
        mImgList = new ArrayList<>(MAX_IMG_SIZE);
        mImgList.add("");

        mAdapter = new ImgAdapter(mContext, mImgList);
        LayoutManagerFactory layoutManagerFactory = new LayoutManagerFactory();
        layoutManagerFactory.setGridSpanCount(4);
        RecyclerView.LayoutManager layoutManager = layoutManagerFactory.getLayoutManager(mContext, true);
        
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void initView() {
        mEtContent = (EditText) findViewById(R.id.et_content);
        mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
        mEtContactWay = (EditText) findViewById(R.id.et_contact_way);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_PICK_IMAGE:    //选择图片
                    if (data != null) {
                        final Uri uri = data.getData();
                        handleAddImage(uri);
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 添加图片
     * @param filePath 图片的地址
     */
    private void addImage(String filePath, int position) {
        if (position >= MAX_IMG_SIZE) {
            return;
        }
        boolean add = true;
        int imgSize = mImgList.size();
        int index = position;
        if (imgSize == MAX_IMG_SIZE) {  //图片已填充满了,则全部是修改图片
            mImgList.set(position, filePath);
            add = false;
        } else {    //添加图片或者修改图片
            if (position < imgSize - 1) {   //点击的不是最后一个“+”图片的按钮,则视为修改图片
                mImgList.set(position, filePath);
                add = false;
            } else {    //添加图片
                index = imgSize - 1;
                mImgList.add(index, filePath);
            }
        }
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.position = index;
        if (add) {
            refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        } else {
            refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
        }
        refreshHelper.refresh(mAdapter);
    }

    /**
     * 移除图片
     * @param position 要移除的索引位置
     */
    private void removeImage(int position) {
        int imgSize = mImgList.size();
        if (position < 0 || position >= imgSize) {
            return;
        }
        int refreshType = 0;
        int fromPosition = 0;
        int toPosition = 0;
        if (position == imgSize - 1) { //目前只有一张图片了或者点击的是最后一张，删除后，则需添加默认的“+”
            mImgList.set(position, "");
            refreshType = AdapterRefreshHelper.TYPE_UPDATE;
        } else {
            if (imgSize == MAX_IMG_SIZE && !TextUtils.isEmpty(mImgList.get(MAX_IMG_SIZE - 1))) {  //删除之前已经有3张图片了，且最后一张不是"+"
                refreshType = AdapterRefreshHelper.TYPE_SWAP;
                fromPosition = position;
                toPosition = MAX_IMG_SIZE - 1;
            } else {
                mImgList.remove(position);
                refreshType = AdapterRefreshHelper.TYPE_DELETE;
            }
        }
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.position = position;
        refreshHelper.fromPosition = fromPosition;
        refreshHelper.toPosition = toPosition;
        refreshHelper.type = refreshType;
        refreshHelper.refresh(mAdapter);
    }

    /**
     * 处理图片的添加
     * @param uri
     */
    private void handleAddImage(Uri uri) {
        doInbackground(new NoteTask(uri) {
            @Override
            public void run() {
                Uri imgUri = (Uri) params[0];
                String filePath = SystemUtil.getFilePathFromContentUri(imgUri.toString(), mContext);
                if (!FileUtil.isFileExists(filePath)) { //文件不存在
                    mHandler.sendEmptyMessage(MSG_FILE_NOT_EXISTS);
                    return;
                }
                File file = new File(filePath);
                if (file.length() > MAX_FEEDBACK_IMG_LENGTH) {  //文件超过了2M
                    mHandler.sendEmptyMessage(MSG_FILE_TOO_LARGE);
                    return;
                }
                Message msg = mHandler.obtainMessage();
                msg.obj = filePath;
                msg.arg1 = mAdapter.getSelectedPosition();
                msg.what = MSG_FILE_ADD_SUCCESS;
                mHandler.sendMessage(msg);
            }
        });
    }

    private class ImgViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvImg;
        ImageButton mBtnRemove;

        ImgViewHolder(View itemView) {
            super(itemView);
            
            mIvImg = (ImageView) itemView.findViewById(R.id.iv_icon);
            mBtnRemove = (ImageButton) itemView.findViewById(R.id.btn_remove);
        }
    }

    /**
     * 图片的适配器
     */
    private class ImgAdapter extends RecyclerView.Adapter<ImgViewHolder> {
        private Context context;
        private List<String> list;

        /**
         * 当前点击的索引，从0开始
         */
        private int selectedPosition;

        ImgAdapter(Context context, List<String> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public ImgViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feedback_photo, parent, false);
            ImgViewHolder holder = new ImgViewHolder(view);
            
            OnClickListener clickListener = new OnClickListener();
            
            holder.mIvImg.setOnClickListener(clickListener);
            holder.mBtnRemove.setOnClickListener(clickListener);
            return holder;
        }

        @Override
        public void onBindViewHolder(ImgViewHolder holder, int position) {
            String imgPath = list.get(position);
            holder.mIvImg.setTag(position);
            holder.mBtnRemove.setTag(position);
            if (TextUtils.isEmpty(imgPath)) {
                holder.mIvImg.setBackgroundResource(R.drawable.bg_img_border);
                holder.mIvImg.setImageResource(R.drawable.ic_plus);
                SystemUtil.setViewVisibility(holder.mBtnRemove, View.GONE);
            } else {
                holder.mIvImg.setBackgroundResource(0);
                holder.mIvImg.setImageResource(R.mipmap.ic_launcher);
                SystemUtil.setViewVisibility(holder.mBtnRemove, View.VISIBLE);
                ImageUtil.displayImage(imgPath, holder.mIvImg, ImageUtil.getNoteImageSize());
            }
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        public int getSelectedPosition() {
            return selectedPosition;
        }

        public void setSelectedPosition(int selectedPosition) {
            this.selectedPosition = selectedPosition;
        }

        /**
         * 控件点击的监听器
         */
        class OnClickListener implements View.OnClickListener {

            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                if (position == null) {
                    return;
                }
                selectedPosition = position;
                switch (v.getId()) {
                    case R.id.iv_icon:  //选择图片
                        final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult((Activity) mContext, permissions, new PermissionsResultAction() {
                            @Override
                            public void onGranted() {
                                SystemUtil.choseImage((Activity) mContext, REQ_PICK_IMAGE);
                            }

                            @Override
                            public void onDenied(String permission) {
                                KLog.d(TAG, "-----onDenied---permission----" + permission);
                                if (permissions[0].equals(permission)) {
                                    //如果App的权限申请曾经被用户拒绝过，就需要在这里跟用户做出解释
                                    NoteUtil.onPermissionDenied((Activity) mContext, permission, R.string.tip_read_sdcard_permission_need, R.string.tip_read_sdcard_permission_failed);
                                }
                            }
                        });
                        break;
                    case R.id.btn_remove:   //移除图片
                        removeImage(position);
                        break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
        KLog.d(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
    }

    private static class MyHandler extends Handler {
        private final WeakReference<FeedbackActivity> mTarget;

        public MyHandler(FeedbackActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            FeedbackActivity target = mTarget.get();
            if (target != null) {
                switch (msg.what) {
                    case MSG_FILE_NOT_EXISTS:   //文件不存在
                        SystemUtil.makeShortToast(R.string.tip_file_not_exists);
                        break;
                    case MSG_FILE_ADD_SUCCESS: //文件添加成功
                        String filePath = (String) msg.obj;
                        if (!TextUtils.isEmpty(filePath)) {
                            target.addImage(filePath, msg.arg1);
                        }
                        break;
                    case MSG_FILE_TOO_LARGE:    //文件超过了2M
                        SystemUtil.makeShortToast(target.getString(R.string.tip_file_too_large, Constants.MAX_FEEDBACK_IMG_UNIT));
                        break;
                }
            }
        }
    }
}
