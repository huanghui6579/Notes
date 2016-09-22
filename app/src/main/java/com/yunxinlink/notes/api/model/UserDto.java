package com.yunxinlink.notes.api.model;

import com.yunxinlink.notes.model.User;

/**
 * 与服务器API交互的实体
 */
public class UserDto {
	
	/**
	 * 用户
	 */
	private User user;
	
	/**
	 * 访问的token
	 */
	private String token;
	
	/**
	 * 第三方账号的用户id
	 */
	private String openUserId;
	
	/**
	 * 用户的类型，如果是本账号体系的，则为0，1：微信，2：QQ，3：微博
	 */
	private Integer type;
	
	/**
	 * token的过期时间
	 */
	private long expiresTime;

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getOpenUserId() {
		return openUserId;
	}

	public void setOpenUserId(String openUserId) {
		this.openUserId = openUserId;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public long getExpiresTime() {
		return expiresTime;
	}

	public void setExpiresTime(long expiresTime) {
		this.expiresTime = expiresTime;
	}

	@Override
	public String toString() {
		return "UserDto [user=" + user + ", token=" + token + ", openUserId=" + openUserId + ", type=" + type
				+ ", expiresTime=" + expiresTime + "]";
	}

}
