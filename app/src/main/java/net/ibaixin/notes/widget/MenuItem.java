package net.ibaixin.notes.widget;

/**
 * 菜单
 * @author huanghui1
 * @update 2016/2/24 10:35
 * @version: 0.0.1
 */
public class MenuItem {
    /**
     * 菜单的id
     */
    private int id;
    
    /**
     * 菜单图标id
     */
    private int mIconResId;

    /**
     * 菜单名称
     */
    private String mTitle;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIconResId() {
        return mIconResId;
    }

    public void setIconResId(int iconResId) {
        this.mIconResId = iconResId;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public MenuItem(int id, int mIconResId, String mTitle) {
        this.id = id;
        this.mIconResId = mIconResId;
        this.mTitle = mTitle;
    }

    public MenuItem() {
    }

    @Override
    public String toString() {
        return "MenuItem{" +
                "id=" + id +
                ", mIconResId=" + mIconResId +
                ", mTitle='" + mTitle + '\'' +
                '}';
    }
}
