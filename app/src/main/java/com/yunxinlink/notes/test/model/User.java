package com.yunxinlink.notes.test.model;

import java.io.Serializable;

/**
 * 用户实体
 * @author huanghui1
 *
 */
public class User implements Serializable {
	private static final long serialVersionUID = -3297032486782428619L;

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
	 * 用户电话，可登录用
	 */
	private String mobile;

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

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", password=" + password + ", mobile=" + mobile + "]";
	}
}
