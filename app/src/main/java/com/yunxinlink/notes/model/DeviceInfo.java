package com.yunxinlink.notes.model;

import android.text.TextUtils;

/**
 * 设备信息的实体
 * @author huanghui1
 *
 */
public class DeviceInfo {
	
	private Integer id;
	
	/**
	 * 设备唯一编号
	 */
	private String imei;
	
	/**
	 * 设备平台，如Android、IOS、Windows等
	 */
	private String os;
	
	/**
	 * 系统的版本号，如Android 6.0
	 */
	private String osVersion;
	
	/**
	 * 手机型号，如1505-A02
	 */
	private String phoneModel;
	
	/**
	 * 手机的厂商，如360 、小米、华为
	 */
	private String brand;

	/**
	 * 客户端软件的版本号
	 */
	private int appVersionCode;

	/**
	 * 客户端软件的版本名名称
	 */
	private String appVersionName;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	/**
	 * 检查设备信息是否为空，imei不存在则为空
	 * @return
	 */
	public boolean checkInfo() {
		return !TextUtils.isEmpty(imei) && !"0".equals(imei);
	}

	@Override
	public String toString() {
		return "DeviceInfo{" +
				"id=" + id +
				", imei='" + imei + '\'' +
				", os='" + os + '\'' +
				", osVersion='" + osVersion + '\'' +
				", phoneModel='" + phoneModel + '\'' +
				", brand='" + brand + '\'' +
				", appVersionCode=" + appVersionCode +
				", appVersionName='" + appVersionName + '\'' +
				'}';
	}
}
