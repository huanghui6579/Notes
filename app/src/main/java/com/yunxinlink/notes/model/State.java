package com.yunxinlink.notes.model;

/**
 * 用户的可用状态
 * @author huanghui1
 *
 */
public interface State {
	/**
	 * 用户状态-可用
	 */
	public static final int NORMAL = 0;
	
	/**
	 * 用户状态-不可用
	 */
	public static final int DISABLE = 1;
}
