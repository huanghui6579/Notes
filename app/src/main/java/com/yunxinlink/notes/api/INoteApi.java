package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.NoteDto;
import com.yunxinlink.notes.model.ActionResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * 笔记的服务器端api
 * @author huanghui1
 * @update 2016/10/12 18:08
 * @version: 0.0.1
 */
public interface INoteApi {

    /**
     * 上传笔记的信息
     * @param userSid 用户sid
     * @param noteDto 笔记数据
     * @return
     */
    @POST("{userSid}/up")
    public Call<ActionResult<Void>> syncUpNote(@Path("userSid") String userSid, @Body NoteDto noteDto);
}
