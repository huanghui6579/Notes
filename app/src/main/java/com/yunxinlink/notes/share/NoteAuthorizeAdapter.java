package com.yunxinlink.notes.share;

import android.content.res.Resources;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.util.NoteUtil;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.authorize.AuthorizeAdapter;
import cn.sharesdk.sina.weibo.SinaWeibo;

/**
 * 自定义的授权界面
 * @author huanghui1
 * @update 2016/8/19 20:04
 * @version: 0.0.1
 */
public class NoteAuthorizeAdapter extends AuthorizeAdapter implements View.OnClickListener, PlatformActionListener {

    private PlatformActionListener mBackListener;

    /**
     * 授权成功后是否关注官方的微博
     */
    private boolean mIsForkWeibo;

    @Override
    public void onCreate() {
        super.onCreate();

        String platName = getPlatformName();

        //隐藏右上角ShareSDK Logo
        hideShareSDKLogo();
        //禁止动画
        disablePopUpAnimation();

        //自定义标题栏
        NoteUtil.initTitleView(getActivity(), getTitleLayout());
        
        if (SinaWeibo.NAME.equals(platName)) {  //新浪微博，才拦截监听
            initCheckView();
            
//            interceptPlatformActionListener(platName);

        }

    }

    /**
     * 初始化关注微博的复选框
     * @return
     */
    private void initCheckView() {
        View bodyView = getBodyView().getChildAt(0);
        if (bodyView instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) bodyView;
            Resources resources = getActivity().getResources();

            CheckBox checkBox = new CheckBox(getActivity());
            checkBox.setChecked(true);
            checkBox.setText(R.string.share_fork_sina_weibo);
            checkBox.setTextColor(ResourcesCompat.getColor(resources, R.color.text_content_color, getActivity().getTheme()));
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            
            int marginHorizontal = resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            int marginVertical = resources.getDimensionPixelSize(R.dimen.content_padding);
            
            params.leftMargin = marginHorizontal;
            params.topMargin = marginVertical;
            params.bottomMargin = marginVertical;
            params.rightMargin = marginHorizontal;

            checkBox.setLayoutParams(params);

            linearLayout.addView(checkBox, params);

            mIsForkWeibo = true;

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mIsForkWeibo = isChecked;
                }
            });
            
        } else {
            KLog.d("initCheckView bodyView is not linearLayout");
        }
    }

    /**
     * 自定义监听的拦截
     * @param platName
     */
    private void interceptPlatformActionListener(String platName) {
        Platform plat = ShareSDK.getPlatform(platName);
        // 备份此前设置的事件监听器
        mBackListener = plat.getPlatformActionListener();
        // 设置新的监听器，实现事件拦截
        plat.setPlatformActionListener(this);
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
        if (action == Platform.ACTION_FOLLOWING_USER) {
            // 当作授权以后不做任何事情
            platform.setPlatformActionListener(mBackListener);
            if (mBackListener != null) {
                mBackListener.onComplete(platform, Platform.ACTION_AUTHORIZING, null);
            }
        } else if (mIsForkWeibo) {

            platform.setPlatformActionListener(mBackListener);
            if (mBackListener != null) {
                mBackListener.onComplete(platform, action, hashMap);
            }
            
            // 授权成功，执行关注
//            String account = "shelly珊珊";
//            platform.followFriend(account);
            
        } else {
            // 如果没有标记为“授权并关注”则直接返回
            platform.setPlatformActionListener(mBackListener);
            if (mBackListener != null) {
                // 关注成功也只是当作授权成功返回
                mBackListener.onComplete(platform, action, hashMap);
            }
        }
        KLog.d("---onComplete--platform---" + platform.getName() + "----action:" + action);
    }

    @Override
    public void onError(Platform platform, int action, Throwable throwable) {
        if (action == Platform.ACTION_AUTHORIZING) {
            // 授权时即发生错误
            platform.setPlatformActionListener(mBackListener);
            if (mBackListener != null) {
                mBackListener.onError(platform, action, throwable);
            }
        } else {
            // 关注时发生错误
            platform.setPlatformActionListener(mBackListener);
            if (mBackListener != null) {
                mBackListener.onComplete(platform, Platform.ACTION_AUTHORIZING, null);
            }
        }
        KLog.d("---onError--platform---" + platform.getName() + "----action:" + action + "----error:" + throwable);
    }

    @Override
    public void onCancel(Platform platform, int action) {
        KLog.d("---onCancel--platform---" + platform.getName() + "----action:" + action);
        platform.setPlatformActionListener(mBackListener);
        if (action == Platform.ACTION_AUTHORIZING) {
            // 授权前取消
            if (mBackListener != null) {
                mBackListener.onCancel(platform, action);
            }
        } else {
            // 当作授权以后不做任何事情
            if (mBackListener != null) {
                mBackListener.onComplete(platform, Platform.ACTION_AUTHORIZING, null);
            }

        }
    }
}
