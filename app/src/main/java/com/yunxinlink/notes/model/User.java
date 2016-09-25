package com.yunxinlink.notes.model;

/**
 * 用户实体
 * @author huanghui1
 * @update 2016/3/7 17:41
 * @version: 0.0.1
 */
public class User {

    /**
     * 主键
     */
    private Integer id;

    /**
     * 用户的唯一id
     */
    private String username;

    /**
     * 用户登录密码
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户电话，可登录用
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户的唯一标识，手动生成
     */
    private String sid;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别，0：未知，1：男，2：女
     */
    private Integer gender;

    /**
     * 创建时间
     */
    private long createTime;

    /**
     * 用户的状态
     */
    private Integer state = State.NORMAL;

    /**
     * 最后一次同步的时间
     */
    private Long lastSyncTime;

    /**
     * 用户的第三方账号id
     */
    private String openUserId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public Long getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Long lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public String getOpenUserId() {
        return openUserId;
    }

    public void setOpenUserId(String openUserId) {
        this.openUserId = openUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * 检查该用户是否可用
     * @return
     */
    public boolean checkState() {
        return State.NORMAL == state;
    }

    /**
     * 检查用户的id是否可用，true：可用
     * @return
     */
    public boolean checkId() {
        return id != null && id > 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", nickname='" + nickname + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", sid='" + sid + '\'' +
                ", avatar='" + avatar + '\'' +
                ", gender=" + gender +
                ", createTime=" + createTime +
                ", state=" + state +
                ", lastSyncTime=" + lastSyncTime +
                ", openUserId='" + openUserId + '\'' +
                '}';
    }
}
