package com.yunxinlink.notes.api;

import com.yunxinlink.notes.api.model.AttachDto;
import com.yunxinlink.notes.api.model.DetailListDto;
import com.yunxinlink.notes.api.model.FolderDto;
import com.yunxinlink.notes.api.model.NoteDto;
import com.yunxinlink.notes.api.model.NoteInfoDto;
import com.yunxinlink.notes.api.model.PageInfo;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.Folder;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

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
     * @return 上传笔记的结果
     */
    @POST("note/{userSid}/up")
    Call<ActionResult<Void>> syncUpNote(@Path("userSid") String userSid, @Body NoteDto noteDto);

    /**
     * 上传笔记的附件
     * @param params 参数
     * @return 上传附件的结果
     */
    @Multipart
    @POST("note/att/upload")
    Call<ActionResult<Void>> uploadAttach(@PartMap Map<String, RequestBody> params);

    /**
     * 下载笔记本的sid集合
     * @param userSid 用户的sid
     * @param params 参数
     * @return 返回笔记本的sid集合
     */
    @GET("note/{userSid}/folder/sids")
    Call<ActionResult<PageInfo<List<Folder>>>> downFolderSid(@Path("userSid") String userSid, @QueryMap Map<String, String> params);

    /**
     * 根据指定的id数组获取笔记本的列表
     * @param userSid 用户的sid
     * @param params 参数
     * @return 笔记本的数据
     */
    @FormUrlEncoded
    @POST("note/{userSid}/folders/filter")
    Call<ActionResult<List<FolderDto>>> downFoldersFilter(@Path("userSid") String userSid, @FieldMap Map<String, String> params);

    /**
     * 分页获取笔记本的信息
     * @param userSid 用户的sid
     * @param params 参数
     * @return 笔记本的数据
     */
    @GET("note/{userSid}/folders")
    Call<ActionResult<PageInfo<List<FolderDto>>>> downFolders(@Path("userSid") String userSid, @QueryMap Map<String, String> params);

    /**
     * 分页获取笔记的数据
     * @param userSid list
     * @param params 参数
     * @return 笔记的数据
     */
    @GET("note/{userSid}/list")
    Call<ActionResult<PageInfo<List<NoteInfoDto>>>> downNotes(@Path("userSid") String userSid, @QueryMap Map<String, String> params);

    /**
     * 分页获取笔记的sid、hash等数据
     * @param userSid list
     * @param params 参数
     * @return 笔记的数据
     */
    @GET("note/{userSid}/list")
    Call<ActionResult<PageInfo<List<NoteInfoDto>>>> downNoteSids(@Path("userSid") String userSid, @QueryMap Map<String, String> params);

    /**
     * 根据指定的id数组获取笔记的列表
     * @param userSid 用户的sid
     * @param params 参数
     * @return 笔记的数据
     */
    @FormUrlEncoded
    @POST("note/{userSid}/list/filter")
    Call<ActionResult<List<NoteInfoDto>>> downNotesFilter(@Path("userSid") String userSid, @FieldMap Map<String, String> params);

    /**
     * 根据指定的id数组获取清单的列表
     * @param userSid 用户的sid
     * @param params 参数
     * @return 清单的数据
     */
    @FormUrlEncoded
    @POST("note/{userSid}/detaillist/filter")
    Call<ActionResult<List<DetailListDto>>> downDetailListFilter(@Path("userSid") String userSid, @FieldMap Map<String, String> params);

    /**
     * 根据指定的id数组获取附件的列表
     * @param userSid 用户的sid
     * @param params 参数
     * @return 附件的数据
     */
    @FormUrlEncoded
    @POST("note/{userSid}/attach/filter")
    Call<ActionResult<List<AttachDto>>> downAttachFilter(@Path("userSid") String userSid, @FieldMap Map<String, String> params);
}
