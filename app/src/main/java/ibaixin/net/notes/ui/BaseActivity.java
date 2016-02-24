package ibaixin.net.notes.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import ibaixin.net.notes.R;

/**
 * @author huanghui1
 * @update 2016/2/24 17:30
 * @version: 0.0.1
 */
public abstract class BaseActivity extends AppCompatActivity {
    protected static String TAG = null;
    
    protected Context mContext;
    
    protected Toolbar mToolBar;
    
    public BaseActivity() {
        TAG = this.getClass().getSimpleName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        
        setContentView(getContentView());

        initToolBar();

        initView();

        initData();
    }
    
    protected abstract int getContentView();
    
    /**
     * 初始化toolbar
     * @author huanghui1
     * @update 2016/2/24 17:39
     * @version: 1.0.0
     */
    protected void initToolBar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolBar != null) {
            setSupportActionBar(mToolBar);
        }
    }
    
    /**
     * 初始化数据
     * @author huanghui1
     * @update 2016/2/24 17:35
     * @version: 1.0.0
     */
    protected abstract void initData();
    
    /**
     * 初始化控件
     * @author huanghui1
     * @update 2016/2/24 17:35
     * @version: 1.0.0
     */
    protected abstract void initView();
}
