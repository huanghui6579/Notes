package ibaixin.net.notes.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ibaixin.net.notes.R;
import ibaixin.net.notes.listener.OnItemClickListener;
import ibaixin.net.notes.model.Archive;
import ibaixin.net.notes.util.Constants;
import ibaixin.net.notes.util.SystemUtil;

/**
 * 主界面
 * @author huanghui1
 * @update 2016/2/24 19:25
 * @version: 1.0.0
 */
public class MainActivity extends BaseActivity {

    private NavViewAdapter mNavAdapter;
    
    List<Archive> mArchives;
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_SUCCESS:
                    mNavAdapter.notifyDataSetChanged();
                    break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (mToolBar != null) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, mToolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }

        RecyclerView navigationView = (RecyclerView) findViewById(R.id.nav_view);

        mArchives = new ArrayList<>();
        Archive archive = new Archive();
        archive.setName(getString(R.string.default_archive));
        mArchives.add(archive);

        navigationView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
        mNavAdapter = new NavViewAdapter(this, mArchives);
        mNavAdapter.setItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                SystemUtil.makeShortToast("点击了item的" + position);
                view.setSelected(true);
            }
        });
        navigationView.setAdapter(mNavAdapter);
    }

    @Override
    protected void initData() {
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    Archive archive = new Archive();
                    archive.setName("测试分类" + i);
                    mArchives.add(archive);
                    
                    mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    class NavTextViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;

        public NavTextViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    class NavViewAdapter extends RecyclerView.Adapter<NavTextViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<Archive> mList;
        private OnItemClickListener mItemClickListener;

        public NavViewAdapter(Context context, List<Archive> items) {
            mList = items;
            mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void setItemClickListener(OnItemClickListener itemClickListener) {
            this.mItemClickListener = itemClickListener;
        }

        @Override
        public NavTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.nav_list_item, parent, false);
            return new NavTextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NavTextViewHolder holder, final int position) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClick(v, position);
                    }
                }
            });
            holder.mTextView.setText(mList.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }
}
