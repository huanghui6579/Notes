package com.yunxinlink.notes.model;

import android.text.TextUtils;

import java.util.Date;

/**
 * 用户反馈的问题或者建议
 * @author huanghui-iri
 * @date 2016年11月16日 上午10:55:32
 */
public class FeedbackInfo {
	
	/**
	 * 未解决：0
	 */
	public static int STATE_NONE = 0;
	/**
	 * 已解决：1
	 */
	public static int STATE_DONE = 1;
	
	/**
	 * 主键
	 */
	private int id;
	
	/**
	 * 关联用户的sid
	 */
	private String userSid;
	
	/**
	 * 反馈的内容
	 */
	private String content;
	
	/**
	 * 用户的手机miei号
	 */
	private String imei;
	
	/**
	 * 用户的系统类型，如Android、IOS
	 */
	private String os;
	
	/**
	 * 系统的版本号
	 */
	private String osVersion;
	
	/**
	 * 手机型号
	 */
	private String phoneModel;
	
	/**
	 * 手机品牌商
	 */
	private String brand;
	
	/**
	 * 客户端的版本号
	 */
	private int appVersionCode;
	
	/**
	 * 客户端的版本名称
	 */
	private String appVersionName;
	
	/**
	 * 用户的联系方式
	 */
	private String contactWay;
	
	/**
	 * 反馈时间
	 */
	private Date createTime;
	
	/**
	 * 解决状态，0：未解决；1、已解决
	 */
	private int state = STATE_NONE;
	
	public static int getSTATE_NONE() {
		return STATE_NONE;
	}

	public static void setSTATE_NONE(int sTATE_NONE) {
		STATE_NONE = sTATE_NONE;
	}

	public static int getSTATE_DONE() {
		return STATE_DONE;
	}

	public static void setSTATE_DONE(int sTATE_DONE) {
		STATE_DONE = sTATE_DONE;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getImei() {
		return imei;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public String getOs() {
		return os;
	}

	public void setOs(String os) {
		this.os = os;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getPhoneModel() {
		return phoneModel;
	}

	public void setPhoneModel(String phoneModel) {
		this.phoneModel = phoneModel;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public int getAppVersionCode() {
		return appVersionCode;
	}

	public void setAppVersionCode(int appVersionCode) {
		this.appVersionCode = appVersionCode;
	}

	public String getAppVersionName() {
		return appVersionName;
	}

	public void setAppVersionName(String appVersionName) {
		this.appVersionName = appVersionName;
	}

	public String getContactWay() {
		return contactWay;
	}

	public void setContactWay(String contactWay) {
		this.contactWay = contactWay;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getUserSid() {
		return userSid;
	}

	public void setUserSid(String userSid) {
		this.userSid = userSid;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	/**
	 * 检查是否有反馈内容,true:有内容
	 * @return
	 */
	public boolean checkContent() {
		return !TextUtils.isEmpty(content);
	}

	@Override
	public String toString() {
		return "FeedbackInfo [id=" + id + ", userSid=" + userSid + ", content=" + content + ", imei=" + imei + ", os="
				+ os + ", osVersion=" + osVersion + ", phoneModel=" + phoneModel + ", brand=" + brand
				+ ", appVersionCode=" + appVersionCode + ", appVersionName=" + appVersionName + ", contactWay="
				+ contactWay + ", createTime=" + createTime + ", state=" + state + "]";
	}
}
