package com.yunxinlink.notes.model;

/**
 * 返回数据的实体，以json形式返回
 * @author huanghui1
 *
 * @param <T>
 */
public class ActionResult<T> {
	public static final int RESULT_SUCCESS = 100;

	public static final int RESULT_FAILED = 200;

	/**
	 * 参数错误
	 */
	public static final int RESULT_PARAM_ERROR = 201;

	/**
	 * 状态-不可用
	 */
	public static final int RESULT_STATE_DISABLE = 202;

	/**
	 * 数据不存在
	 */
	public static final int RESULT_DATA_NOT_EXISTS = 203;
	
	/**
	 * 返回码
	 */
	private int resultCode = RESULT_FAILED;
	
	private T data;
	
	/**
	 * 结果描述语
	 */
	private String reason;

	public int getResultCode() {
		return resultCode;
	}

	public void setResultCode(int resultCode) {
		this.resultCode = resultCode;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "ActionResult [resultCode=" + resultCode + ", data=" + data + ", reason=" + reason + "]";
	}

	/**
	 * 请求结果是否成功
	 * @return
     */
	public boolean isSuccess() {
		return RESULT_SUCCESS == resultCode;
	}
	
}
