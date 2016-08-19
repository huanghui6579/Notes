package com.yunxinlink.notes.share;

import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.yunxinlink.notes.R;

import java.util.HashMap;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.TitleLayout;
import cn.sharesdk.framework.authorize.AuthorizeAdapter;

/**
 * 自定义的授权界面
 * @author huanghui1
 * @update 2016/8/19 20:04
 * @version: 0.0.1
 */
public class NoteAuthorizeAdapter extends AuthorizeAdapter implements View.OnClickListener, PlatformActionListener {

    @Override
    public void onCreate() {
        super.onCreate();

        //隐藏右上角ShareSDK Logo
        hideShareSDKLogo();
        //禁止动画
        disablePopUpAnimation();

        initTitleView();
    }
    
    private void initTitleView() {
        
        getActivity().setTheme(R.style.AppTheme_NoActionBar);
        
        TitleLayout titleLayout = getTitleLayout();
//        titleLayout.removeAllViews();
        TextView textView = titleLayout.getTvTitle();
        
        CharSequence title = textView.getText();

        titleLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.app_bar_layout, null);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        toolbar.setTitle(title);

        titleLayout.addView(toolbar);
        
        
        
    }

    /*private void initUi(String platName) {

        ctvFollow = new CheckedTextView(getActivity());
        try {
            ctvFollow.setBackgroundResource(R.drawable.auth_follow_bg);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        ctvFollow.setChecked(true);
        int dp_10 = cn.sharesdk.framework.utils.R.dipToPx(getActivity(), 10);
        ctvFollow.setCompoundDrawablePadding(dp_10);
        ctvFollow.setCompoundDrawablesWithIntrinsicBounds(R.drawable.auth_cb,
                0, 0, 0);
        ctvFollow.setGravity(Gravity.CENTER_VERTICAL);
        ctvFollow.setPadding(dp_10, dp_10, dp_10, dp_10);
        ctvFollow.setText(R.string.sm_item_fl_weibo);
        if (platName.equals("TencentWeibo")) {
            ctvFollow.setText(R.string.sm_item_fl_tc);
        }
        ctvFollow.setTextColor(0xff909090);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ctvFollow.setLayoutParams(lp);

        TitleLayout tt = getTitleLayout();
        tt.removeAllViews();
        ViewGroup.LayoutParams fff = tt.getLayoutParams();
        TextView tv = new TextView(getActivity());
        tv.setLayoutParams(fff);
        tv.setText("dfsdfsd");
        tv.setGravity(Gravity.CENTER);
        tt.addView(tv);

        LinearLayout llBody = (LinearLayout) getBodyView().getChildAt(0);
        llBody.addView(ctvFollow);
        ctvFollow.setOnClickListener(this);

        ctvFollow.measure(0, 0);
        int height = ctvFollow.getMeasuredHeight();
        TranslateAnimation animShow = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                Animation.ABSOLUTE, height, Animation.ABSOLUTE, 0);
        animShow.setDuration(1000);
        getWebBody().startAnimation(animShow);
        ctvFollow.startAnimation(animShow);
    }*/

    @Override
    public void onClick(View v) {
        
    }

    @Override
    public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
        
    }

    @Override
    public void onError(Platform platform, int i, Throwable throwable) {

    }

    @Override
    public void onCancel(Platform platform, int i) {

    }
}
