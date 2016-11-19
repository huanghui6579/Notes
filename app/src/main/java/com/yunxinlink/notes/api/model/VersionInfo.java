package com.yunxinlink.notes.api.model;

import android.text.TextUtils;

/**
 * 软件的版本信息
 * @author huanghui-iri
 * @date 2016年11月16日 下午5:53:23
 */
public class VersionInfo implements Cloneable {
	
	private int id;
	
	/**
	 * 更新的记录
	 */
	private String content;
	
	/**
	 * 版本号
	 */
	private int versionCode;
	
	/**
	 * 版本名称
	 */
	private String versionName;
	
	/**
	 * 用户的系统平台，0:Android, 1:IOS
	 */
	private int platform = Platform.PLATFORM_ANDROID;
	
	/**
	 * 更新时间
	 */
	private long createTime;
	
	/**
	 * 是否是里程碑
	 */
	private boolean isMilestone;
	
	/**
	 * 软件包的大小
	 */
	private long size;
	
	/**
	 * 软件的hash值-MD5
	 */
	private String hash;

	/**
	 * 文件的本地全路径
	 */
	private String filePath;

	/**
	 * 文件名
	 */
	private String filename;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public Integer getPlatform() {
		return platform;
	}

	public void setPlatform(Integer platform) {
		this.platform = platform;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public void setPlatform(int platform) {
		this.platform = platform;
	}

	public boolean isMilestone() {
		return isMilestone;
	}

	public void setMilestone(boolean isMilestone) {
		this.isMilestone = isMilestone;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * 是否有内容，true：有更新日志内容
	 * @return
	 */
	public boolean checkContent() {
		return !TextUtils.isEmpty(content);
	}

	@Override
	public VersionInfo clone() {
		VersionInfo versionInfo = null;
		try {
			versionInfo = (VersionInfo) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return versionInfo;
	}

	@Override
	public String toString() {
		return "VersionInfo{" +
				"id=" + id +
				", content='" + content + '\'' +
				", versionCode=" + versionCode +
				", versionName='" + versionName + '\'' +
				", platform=" + platform +
				", createTime=" + createTime +
				", isMilestone=" + isMilestone +
				", size=" + size +
				", hash='" + hash + '\'' +
				", filePath='" + filePath + '\'' +
				", filename='" + filename + '\'' +
				'}';
	}
}
