
package me.imid.swipebacklayout.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import net.ibaixin.notes.util.log.Log;

import me.imid.swipebacklayout.SwipeBackLayout;
import me.imid.swipebacklayout.Utils;

public class SwipeBackActivity extends AppCompatActivity implements SwipeBackActivityBase {
    private SwipeBackActivityHelper mHelper;

    /**
     * 是否允许滑动返回，默认为true
     */
    private boolean mSwipeBackEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSwipeBackEnabled = isSwipeBackEnabled();
        
        if (mSwipeBackEnabled) {
            mHelper = new SwipeBackActivityHelper(this);
            mHelper.onActivityCreate();
        }
        
    }

    /**
     * 是否可以滑动返回
     * @author huanghui1
     * @update 2016/6/27 14:28
     * @version: 1.0.0
     */
    private boolean canSwipeBack() {
        return mSwipeBackEnabled && mHelper != null;
    }

    /**
     * 是否允许滑动返回activity，默认为true
     * @author huanghui1
     * @update 2016/6/27 10:45
     * @version: 1.0.0
     */
    public boolean isSwipeBackEnabled() {
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (canSwipeBack()) {
            mHelper.onPostCreate();
        }
    }

    @Override
    public View findViewById(int id) {
        View v = super.findViewById(id);
        if (v == null && canSwipeBack())
            return mHelper.findViewById(id);
        return v;
    }

    @Override
    public SwipeBackLayout getSwipeBackLayout() {
        if (canSwipeBack()) {
            SwipeBackLayout backLayout = mHelper.getSwipeBackLayout();
            backLayout.setEdgeTrackingEnabled(SwipeBackLayout.EDGE_LEFT);
            return backLayout;
        } else {
            Log.d("---SwipeBackActivity------not ---enable---getSwipeBackLayout---is null-----");
            return null;
        }
    }

    @Override
    public void setSwipeBackEnable(boolean enable) {
        if (canSwipeBack()) {
            getSwipeBackLayout().setEnableGesture(enable);
        }
    }

    @Override
    public void scrollToFinishActivity() {
        if (canSwipeBack()) {
            Utils.convertActivityToTranslucent(this);
            getSwipeBackLayout().scrollToFinishActivity();
        }
    }
}
