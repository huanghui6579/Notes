package com.yunxinlink.notes.model;

/**
 * 账号类型，0：本系统的账号系统，1：微信，2：QQ，3：微博
 * @author huanghui1
 * @update 2016/9/21 17:23
 * @version: 0.0.1
 */
public interface AccountType {
    /**
     * 本账号系统，即使用用户名或者手机号和密码进行登录的
     */
    int TYPE_LOCAL = 0;

    /**
     * 使用微信进行登录:1
     */
    int TYPE_WECHAT = 1;

    /**
     * 使用QQ进行登录:2
     */
    int TYPE_QQ = 2;

    /**
     * 使用微博进行登录:3
     */
    int TYPE_WEIBO = 3;
}
