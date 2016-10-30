package com.yunxinlink.notes.api.impl;

import android.content.Context;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.INoteApi;
import com.yunxinlink.notes.api.model.AttachDto;
import com.yunxinlink.notes.api.model.DetailListDto;
import com.yunxinlink.notes.api.model.FolderDto;
import com.yunxinlink.notes.api.model.NoteDto;
import com.yunxinlink.notes.api.model.NoteInfoDto;
import com.yunxinlink.notes.api.model.PageInfo;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.AttachManager;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 笔记的相关API
 * @author huanghui1
 * @update 2016/10/13 9:52
 * @version: 0.0.1
 */
public class NoteApi extends BaseApi {
    private static final String TAG = "NoteApi";

    /**
     * 向服务器端同步笔记信息,该方法为同步方法，即在主线程中运行，所在外部调用时需另开线程
     * @param context 上下文
     * @param folder 笔记所属的笔记本
     * @param noteInfos 要同步的笔记              
     * @param listener 同步完后的回调
     * @return
     */
    public static Call<?> syncUpNote(Context context, Folder folder, List<DetailNoteInfo> noteInfos, OnLoadCompletedListener<ActionResult<Void>> listener) throws IOException {
        KLog.d(TAG, "sync up note invoke...");
        if (context == null) {
            KLog.d(TAG, "sync up note invoke but context is null and will return");
            return null;
        }
        User user = getUser(context);
        if (user == null) {  //用户不可用
            KLog.d(TAG, "sync up note failed user is not available");
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_DATA_NOT_EXISTS, "user is null or not available");
            }
            return null;
        }
        
        String userSid = user.getSid();
        
        /*if (SystemUtil.isEmpty(noteInfos)) {    //笔记为空
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params is null");
            }
            KLog.d(TAG, "sync up note failed params is null");
            return null;
        }*/
        
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);

        NoteDto noteDto = fillNoteInfoDto(noteInfos);
        /*if (noteDto == null) {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params noteDto is null");
            }
            KLog.d(TAG, "sync up note failed params noteDto is null");
            return null;
        }*/
        
        noteDto.setUserSid(userSid);
        noteDto.setFolder(fillFolderDto(folder));
        Call<ActionResult<Void>> call = repo.syncUpNote(userSid, noteDto);
        Response<ActionResult<Void>> responseBody = call.execute();
        if (responseBody == null) {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_ERROR, "responseBody is null");
            }
            KLog.d(TAG, "sync up note failed responseBody is null");
            return null;
        }
        if (responseBody.isSuccessful() && responseBody.body() != null) {  //服务器请求成功
            ActionResult<Void> actionResult = responseBody.body();
            if (actionResult.isSuccess()) { //成功
                KLog.d(TAG, "sync up note data success and save or up data to local");
                updateNative(folder, noteInfos);
                if (!SystemUtil.isEmpty(noteInfos)) {   //有笔记
                    for (DetailNoteInfo detailNoteInfo : noteInfos) {
                        NoteInfo noteInfo = detailNoteInfo.getNoteInfo();
                        Map<String, Attach> attachMap = noteInfo.getAttaches();
                        if (noteInfo.hasAttach() && !SystemUtil.isEmpty(attachMap)) {   //有附件
                            Set<String> keys = attachMap.keySet();
                            for (String key : keys) {
                                Attach attach = attachMap.get(key);
                                if (attach.isSynced()) {
                                    continue;
                                }
                                uploadAttach(attach, null);
                            }
                        }
                    }
                }
                if (listener != null) {
                    listener.onLoadSuccess(actionResult);
                }
            } else {
                KLog.d(TAG, "sync up note response is successful but result error");
                int resultCode = actionResult.getResultCode();
                String reason = null;
                switch (resultCode) {
                    case ActionResult.RESULT_FAILED:    //同步结果是失败的
                        reason = "sync up note result is failed";
                        KLog.d(TAG, "sync up note response is successful but result is failed");
                        break;
                    case ActionResult.RESULT_ERROR:    //同步过程中遇到错误
                        reason = "sync up note result is error";
                        KLog.d(TAG, "sync up note response is successful but result is error");
                        break;
                    case ActionResult.RESULT_PARAM_ERROR:    //同步的参数错误
                        reason = "sync up note result params is error";
                        KLog.d(TAG, "sync up note response is successful but result params is error");
                        break;
                    case ActionResult.RESULT_STATE_DISABLE:    //用户被禁用
                    case ActionResult.RESULT_DATA_NOT_EXISTS:    //用户不存在或者被禁用
                        reason = "sync up note result user is not exists or disable";
                        KLog.d(TAG, "sync up note response is successful but result user is not exists or disable");
                        break;
                }
                if (listener != null) {
                    listener.onLoadFailed(resultCode, reason);
                }
            }
        } else {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_FAILED, "responseBody is not successful");
            }
            KLog.d(TAG, "sync up note failed is not successful");
            return null;
        }
        return call;
    }

    /**
     * 上传附件
     * @param attach
     * @param listener
     * @return
     * @throws IOException
     */
    public static Call<ActionResult<Void>> uploadAttach(Attach attach, OnLoadCompletedListener<ActionResult<Void>> listener) throws IOException {
        if (attach == null || TextUtils.isEmpty(attach.getSid()) || TextUtils.isEmpty(attach.getLocalPath())) {   //文件路径为null
            KLog.d(TAG, "note api upload attach but attach is null or or attach sid is null or local path is empty");
            return null;
        }
        String localPath = attach.getLocalPath();
        File file = new File(localPath);
        
        if (!file.exists()) {   //文件不存在
            KLog.d(TAG, "note api upload attach local file is not exists");
            return null;
        }

        KLog.d(TAG, "note api upload attach :" + attach);
        
        Map<String, RequestBody> map = new HashMap<>();
        String sid = attach.getSid();
        String noteSid = attach.getNoteId();
        
        map.put("sid", RequestBody.create(null, sid));
        map.put("noteSid", RequestBody.create(null, noteSid));
        String mime = attach.getMimeType();
        if (!TextUtils.isEmpty(mime)) {
            map.put("mimeType", RequestBody.create(null, mime));
        }
        String hash = attach.getHash();

        if (TextUtils.isEmpty(hash)) {  //生成hash
            hash = DigestUtil.md5FileHex(file);
            if (!TextUtils.isEmpty(hash)) {
                map.put("hash", RequestBody.create(null, hash));
            }
        } else {
            map.put("hash", RequestBody.create(null, hash));
        }
        map.put("size", RequestBody.create(null, String.valueOf(file.length())));
        map.put("createTime", RequestBody.create(null, String.valueOf(attach.getCreateTime())));
        map.put("modifyTime", RequestBody.create(null, String.valueOf(attach.getModifyTime())));
        DeleteState deleteState = attach.getDeleteState();
        if (deleteState != null) {
            map.put("deleteState", RequestBody.create(null, String.valueOf(deleteState.ordinal())));
        }
        int type = attach.getType();
        String filename = file.getName();
        map.put("type", RequestBody.create(null, String.valueOf(type)));
        RequestBody attachFile = RequestBody.create(MediaType.parse(mime), file);
        //attFile: 与服务器端的参数名相同
        map.put("attFile\"; filename=\"" + filename + "", attachFile);

        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Call<ActionResult<Void>> call = repo.uploadAttach(map);
        Response<ActionResult<Void>> responseBody = call.execute();
        if (responseBody != null && responseBody.isSuccessful()) {  //请求成功了
            ActionResult<Void> actionResult = responseBody.body();
            if (actionResult != null) {
                if (actionResult.isSuccess()) {
                    updateNativeAttach(attach);
                    KLog.d(TAG, "upload attach success:" + attach);
                } else {
                    int resultCode = actionResult.getResultCode();
                    String reason = actionResult.getReason();
                    switch (resultCode) {
                        case ActionResult.RESULT_FAILED:    //结果失败
                            KLog.d(TAG, "upload attach action result is not null but result code is failed:" + attach);
                            break;
                        case ActionResult.RESULT_PARAM_ERROR:   //参数错误
                            KLog.d(TAG, "upload attach action result is not null but result code is param error:" + attach);
                            break;
                        default:
                            KLog.d(TAG, "upload attach action result is not null but result code is default failed:" + attach);
                            break;
                    }
                    if (listener != null) {
                        listener.onLoadFailed(resultCode, reason);
                    }
                }
            } else {    //action result is null
                KLog.d(TAG, "upload attach response is success but action result is null :" + attach);
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_FAILED, null);
                }
            }
        } else {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_FAILED, "responseBody is not successful");
            }
            KLog.d(TAG, "upload attach failed is not successful:" + attach);
            return null;
        }
        return null;
    }

    /**
     * 获取需要下载笔记本信息的笔记本id集合，该方法在主线程中运行
     * @param context
     * @return
     */
    public static List<Integer> downFolderIds(Context context) {
        User user = getUser(context);
        if (user == null) { //用户不可用
            KLog.d(TAG, "down folder sid but user is null or sid is empty or disabled:" + user);
            return null;
        }
        List<Integer> list = new ArrayList<>();
        //递归执行
        try {
            obtainFolderSidForPage(list, user, 1);
        } catch (IOException e) {
            KLog.d(TAG, "down folder sid list error:" + e.getMessage());
            e.printStackTrace();
        }
        KLog.d(TAG, "down folder sid list:" + list);
        return list;
    }

    /**
     * 循环的获取笔记本的sid
     * @param user
     * @param pageNumber 从第几页开始获取,默认为1
     * @return
     */
    private static void obtainFolderSidForPage(List<Integer> list, User user, int pageNumber) throws IOException {
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> queryMap = new HashMap<>();
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        queryMap.put("offset", String.valueOf(pageNumber));
        queryMap.put("limit", String.valueOf(pageSize));
        KLog.d(TAG, "obtain folder sid page number is:" + pageNumber + " and page size is:" + pageSize);
        Call<ActionResult<PageInfo<List<Folder>>>> call = repo.downFolderSid(user.getSid(), queryMap);
        Response<ActionResult<PageInfo<List<Folder>>>> response = call.execute();
        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "obtain folder sid response is failed");
            return;
        }
        ActionResult<PageInfo<List<Folder>>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "obtain folder sid response is failed");
            return;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "obtain folder sid response is success but action result is not success:" + actionResult);
            return;
        }
        PageInfo<List<Folder>> pageInfo = actionResult.getData();
        if (pageInfo == null || SystemUtil.isEmpty(pageInfo.getData())) {   //没有数据了
            KLog.d(TAG, "obtain folder sid response is success and action result is success but no folder");
            return;
        }
        KLog.d(TAG, "obtain folder sid response is success and action result is success:" + actionResult);
        List<Folder> folderList = pageInfo.getData();
        Map<String, Folder> folderMap = FolderManager.getInstance().getFolders(user, null);
        if (SystemUtil.isEmpty(folderMap)) {    //本地没有笔记本，则直接全部同步服务器上的
            for (Folder folder : folderList) {
                list.add(folder.getId());
            }
        } else {
            for (Folder folder : folderList) {
                Folder tFolder = folderMap.get(folder.getSid());
                if (tFolder == null || (!SystemUtil.equalsStr(folder.getHash(), tFolder.getHash()) && !tFolder.isSyncUp())) {    //本地笔记不存在或者hash不一致
                    list.add(folder.getId());
                }
            }
        }
        KLog.d(TAG, "obtain folder sid list:" + list);
        //已加载过的数据
        long loadCount = (pageNumber - 1) * pageSize + folderList.size();
        long totalRecord = pageInfo.getCount();
        KLog.d(TAG, "obtain folder sid loadCount:" + loadCount + ", and totalCount:" + totalRecord);
        if (totalRecord > loadCount) { //需要继续加载下一页
            KLog.d(TAG, "obtain folder sid will load next page:" + (pageNumber + 1));
            try {
                obtainFolderSidForPage(list, user, pageNumber + 1);
            } catch (IOException e) {
                KLog.e(TAG, "obtain folder sid list: error:" + e.getMessage());
            }
        }
    }

    /**
     * 下载服务器的全部笔记本数据，分页下载，每页20条
     * @param context
     * @return
     */
    public static void downFolders(Context context) {
        User user = getUser(context);
        if (user == null) { //用户不可用
            KLog.d(TAG, "down folders but user is null or sid is empty or disabled");
            return;
        }
        //递归执行
        try {
            obtainFolders(user, 1);
        } catch (IOException e) {
            KLog.d(TAG, "down folders list error:" + e.getMessage());
            e.printStackTrace();
        }
        KLog.d(TAG, "down folders end");
    }

    /**
     * 递归获取服务器的笔记本信息,该方法在主线程中运行，调用者需另开线程
     * @param user 用户
     * @param pageNumber 获取笔记本信息的页码
     */
    private static void obtainFolders(User user, int pageNumber) throws IOException {
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> queryMap = new HashMap<>();
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        queryMap.put("offset", String.valueOf(pageNumber));
        queryMap.put("limit", String.valueOf(pageSize));
        KLog.d(TAG, "obtain folders page number is:" + pageNumber + " and page size is:" + pageSize);
        Call<ActionResult<PageInfo<List<FolderDto>>>> call = repo.downFolders(user.getSid(), queryMap);
        Response<ActionResult<PageInfo<List<FolderDto>>>> response = call.execute();
        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "obtain folders response is failed");
            return;
        }
        ActionResult<PageInfo<List<FolderDto>>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "obtain folder response is failed");
            return;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "obtain folder response is success but action result is not success:" + actionResult);
            return;
        }
        PageInfo<List<FolderDto>> pageInfo = actionResult.getData();
        if (pageInfo == null || SystemUtil.isEmpty(pageInfo.getData())) {   //没有数据了
            KLog.d(TAG, "obtain folders response is success and action result is success but no folder");
            return;
        }
        KLog.d(TAG, "obtain folders response is success and action result is success:" + actionResult);
        List<FolderDto> folderDtoList = pageInfo.getData();
        //本地保存笔记本
        saveFolders(user, folderDtoList);

        //已加载过的数据
        long loadCount = (pageNumber - 1) * pageSize + folderDtoList.size();
        long totalRecord = pageInfo.getCount();
        if (totalRecord > loadCount) { //需要继续加载下一页
            KLog.d(TAG, "obtain folders will load next page:" + (pageNumber + 1));
            try {
                obtainFolders(user, pageNumber + 1);
            } catch (IOException e) {
                KLog.e(TAG, "obtain folders list: error:" + e.getMessage());
            }
        }
    }

    /**
     * 循环获取对应列表的笔记本信息
     * @param folderIdList
     * @param context
     */
    public static void downFolders(List<Integer> folderIdList, Context context) {
        User user = getUser(context);
        if (user == null) {  //用户不可用
            KLog.e(TAG, "down folders but user is null or not available");
            return;
        }
        //开始为第一页
        int pageNumber = 1;
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        int size = folderIdList.size();
        if (size <= pageSize) {
            try {
                downFolders(folderIdList, user);
            } catch (IOException e) {
                KLog.e(TAG, "down folders error:" + e.getMessage());
            }
        } else {
            int count = size % pageSize == 0 ? (size / pageSize) : (size / pageSize + 1);
            KLog.e(TAG, "down folders by page count:" + count);
            for (int i = 0; i < count; i++) {
                int start = (pageNumber - 1) * pageSize;
                int end = start + pageSize;
                try {
                    List<Integer> idList = folderIdList.subList(start, end);
                    KLog.e(TAG, "down folders by page number:" + pageNumber + ", sub list:" + idList);
                    downFolders(idList, user);
                } catch (Exception e) {
                    KLog.e(TAG, "down folders by page error:" + e.getMessage());
                }
                pageNumber ++;
            }
            KLog.e(TAG, "down folders by page end");
        }
    }

    /**
     * 根据一组id获取对应笔记本的数据,该方法在主线程中运行，调用时需另开线程
     * @param idList 笔记本的id列表，该id是服务器的id
     * @param user
     * @return
     */
    private static void downFolders(List<Integer> idList, User user) throws IOException {
        if (SystemUtil.isEmpty(idList)) {
            KLog.d(TAG, "down folder id list is empty:" + idList);
            return;
        }
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> paramMap = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        for (Integer id : idList) {
            builder.append(id).append(Constants.TAG_COMMA);
        }
        int length = builder.length();
        if (length == 0) {
            KLog.d(TAG, "down folder id builder length is 0:" + builder);
            return;
        }
        builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_COMMA));
        paramMap.put("idStr", builder.toString());
        Call<ActionResult<List<FolderDto>>> call = repo.downFoldersFilter(user.getSid(), paramMap);
        Response<ActionResult<List<FolderDto>>> response = call.execute();
        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "down folder response is failed");
            return;
        }
        ActionResult<List<FolderDto>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "down folder response is failed");
            return;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "down folder response is success but action result is not success:" + actionResult);
            return;
        }
        List<FolderDto> folderDtoList = actionResult.getData();
        if (SystemUtil.isEmpty(folderDtoList)) {   //没有数据了
            KLog.d(TAG, "down folder response is success and action result is success but no folder");
            return;
        }
        //本地保存笔记本
        saveFolders(user, folderDtoList);

    }

    /**
     * 下载笔记,该方法在主线程中执行，调用处需要开线程
     * @param context
     * @return
     */
    public static void downNotes(Context context) {
        User user = getUser(context);
        if (user == null) { //用户不可用
            KLog.d(TAG, "down notes but user is null or not available");
            return;
        }
        try {
            downNotes(user, 1, -1);
        } catch (IOException e) {
            KLog.e(TAG, "down notes error:" + e.getMessage());
        }
    }

    /**
     * 下载笔记,该方法在主线程中执行，调用处需要开线程
     * @param user 用户
     * @param pageNumber 第几页，从1开始
     * @param totalCount 总页数， -1表示需要从服务器获取总记录数              
     * @return
     */
    private static void downNotes(User user, int pageNumber, long totalCount) throws IOException {
        KLog.d(TAG, "down notes page：" + pageNumber);
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("offset", String.valueOf(pageNumber));
        queryMap.put("limit", String.valueOf(pageSize));
        if (totalCount == -1) { //需要获取总记录数
            queryMap.put("countSize", String.valueOf(1));
        }

        Call<ActionResult<PageInfo<List<NoteInfoDto>>>> call = repo.downNotes(user.getSid(), queryMap);

        Response<ActionResult<PageInfo<List<NoteInfoDto>>>> response = call.execute();

        boolean result = checkNoteList(response);
        
        if (!result) {
            return;
        }
        
        ActionResult<PageInfo<List<NoteInfoDto>>> actionResult = response.body();
        PageInfo<List<NoteInfoDto>> pageInfo = actionResult.getData();
        List<NoteInfoDto> noteInfoDtoList = pageInfo.getData();
        if (totalCount == -1) {
            totalCount = pageInfo.getCount();
        }
        //保存内容到本地
        saveNotes(user, noteInfoDtoList);
        
        //已加载的数量
        long loadSize = noteInfoDtoList.size() + (pageNumber - 1) * pageSize;
        
        if (loadSize < totalCount) {   //没有加载完毕，需要继续加载
            pageNumber = pageNumber + 1;
            KLog.d(TAG, "down notes next page：" + pageNumber);
            try {
                downNotes(user, pageNumber, totalCount);
            } catch (IOException e) {
                KLog.e(TAG, "down notes error page number:" + pageNumber + ", error:" + e.getMessage());
            }
        } else {
            KLog.d(TAG, "down notes completed...");
        }
    }

    /**
     * 根据笔记的id来分页下载笔记
     * @param context
     */
    public static void downNotesWithIds(Context context) {
        User user = getUser(context);
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "down note width id but user is null or not available");
            return;
        }
        try {
            downNoteByIds(user, 1, -1);
        } catch (IOException e) {
            KLog.e(TAG, "down notes with ids error :" + e.getMessage());
        }
    }

    /**
     * 下载笔记服务器的笔记的ID集合，并带有hash等数据
     * @param user 当前登录用户
     * @param pageNumber 第几页
     * @param totalCount 总页数
     * @throws IOException
     */
    private static void downNoteByIds(User user, int pageNumber, long totalCount) throws IOException {
        KLog.d(TAG, "down note sids page：" + pageNumber);
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("offset", String.valueOf(pageNumber));
        queryMap.put("limit", String.valueOf(pageSize));
        if (totalCount == -1) { //需要获取总记录数
            queryMap.put("countSize", String.valueOf(1));
        }
        String userSid = user.getSid();
        Call<ActionResult<PageInfo<List<NoteInfoDto>>>> call = repo.downNoteSids(userSid, queryMap);

        Response<ActionResult<PageInfo<List<NoteInfoDto>>>> response = call.execute();

        boolean result = checkNoteList(response);

        if (!result) {
            return;
        }

        ActionResult<PageInfo<List<NoteInfoDto>>> actionResult = response.body();
        PageInfo<List<NoteInfoDto>> pageInfo = actionResult.getData();
        
        List<NoteInfoDto> noteInfoDtoList = pageInfo.getData();
        if (totalCount == -1) {
            totalCount = pageInfo.getCount();
        }
        //与本地比较，是否需要下载同步
        //根据下载的sid查询本地对应的sid的笔记，本地没有或者hash不一样，则都需要下载
        //[0]:笔记的ID集合
        //[1]:清单的ID集合
        //[2]:附件的ID集合
        //该集合包含这段记录中的笔记、附件和清单的id
        Map<Integer, String> idMap = checkLocalNotes(noteInfoDtoList);
        
        if (!SystemUtil.isEmpty(idMap)) {   //这20条记录中有一部分需要下载更新
            String noteIdStr = idMap.get(0);
            //仅下载笔记的基本数据，不包含清单、附件
            //TODO 修改下载笔记的逻辑
            List<NoteInfoDto> downNoteDtoList = null;
            if (!TextUtils.isEmpty(noteIdStr)) {
                downNoteDtoList = downNotes(user, noteIdStr);
            }
            if (!SystemUtil.isEmpty(downNoteDtoList)) {  //网络请求是OK的，但是没有数据，说明这组ID
                //转换成映射的集合,key 为noteSid
                Map<String, NoteInfoDto> noteDtoMap = new HashMap<>();

                for (NoteInfoDto noteInfoDto : downNoteDtoList) {
                    noteDtoMap.put(noteInfoDto.getSid(), noteInfoDto);
                }

                //下载清单数据
                String detailIdStr = idMap.get(1);
                if (!TextUtils.isEmpty(detailIdStr)) {  //有清单数据
                    //TODO 下载清单数据
                    List<DetailListDto> detailListDtoList = downDetailLists(user, detailIdStr);
                    if (!SystemUtil.isEmpty(detailListDtoList)) {   //有清单，则填充清单
                        for (DetailListDto detailListDto : detailListDtoList) {
                            String noteSid = detailListDto.getNoteSid();
                            if (noteSid == null) {
                                continue;
                            }
                            NoteInfoDto noteInfoDto = noteDtoMap.get(noteSid);
                            if (noteInfoDto == null) {
                                continue;
                            }
                            List<DetailListDto> detailListDtos = noteInfoDto.getDetails();
                            if (detailListDtos == null) {
                                detailListDtos = new ArrayList<>();
                                noteInfoDto.setDetails(detailListDtos);
                            }
                            detailListDtos.add(detailListDto);
                        }
                    }
                }
                String attIdStr = idMap.get(2);
                if (!TextUtils.isEmpty(attIdStr)) {
                    //下载附件的信息
                    List<AttachDto> attachDtoList = downAttachs(user, attIdStr);
                    if (!SystemUtil.isEmpty(attachDtoList)) {   //有附件
                        for (AttachDto attachDto : attachDtoList) {
                            String noteSid = attachDto.getNoteSid();
                            if (noteSid == null) {
                                continue;
                            }
                            NoteInfoDto noteInfoDto = noteDtoMap.get(noteSid);
                            if (noteInfoDto == null) {
                                continue;
                            }
                            List<AttachDto> attachDtos = noteInfoDto.getAttachs();
                            if (attachDtos == null) {
                                attachDtos = new ArrayList<>();
                                noteInfoDto.setAttachs(attachDtos);
                            }
                            attachDtos.add(attachDto);
                        }
                    }
                }

                //保存到本地
                saveNotes(user, downNoteDtoList);
            } else {
                KLog.d(TAG, "down notes but no notes noteIdStr:" + noteIdStr);
            }
            
        } else {
            KLog.d(TAG, "down note by ids but not id need down or update will do next page");
        }

        //已加载的数量
        long loadSize = noteInfoDtoList.size() + (pageNumber - 1) * pageSize;
        
        boolean loadCompleted = loadSize >= totalCount;

        if (!loadCompleted) {   //没有加载完毕，需要继续加载
            pageNumber = pageNumber + 1;
            KLog.d(TAG, "down note sids next page：" + pageNumber);
            try {
                downNoteByIds(user, pageNumber, totalCount);
            } catch (IOException e) {
                KLog.e(TAG, "down note sids error page number:" + pageNumber + ", error:" + e.getMessage());
            }
        } else {
            KLog.d(TAG, "down note sids completed...");
        }
    }

    /**
     * 下载指定的 id的笔记，仅下载笔记的数据，不下载该笔记拥有的附件和清单，该id是服务器端的笔记id ,字符串id用","分隔
     * @param user 当前登录的用户
     * @param idStr ID的字符串，用","分隔
     * @throws IOException
     */
    private static List<NoteInfoDto> downNotes(User user, String idStr) throws IOException {
        KLog.d(TAG, "down notes by ids:" + idStr);
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> map = new HashMap<>();
        map.put("idStr", idStr);
        map.put("simple", String.valueOf(true));
        Call<ActionResult<List<NoteInfoDto>>> call = repo.downNotesFilter(user.getSid(), map);
        Response<ActionResult<List<NoteInfoDto>>> response = call.execute();

        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "down note filter sid response is failed");
            return null;
        }

        ActionResult<List<NoteInfoDto>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "down note filter sid response is failed");
            return null;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "down notes filter sid response is success but action result is not success:" + actionResult);
            return null;
        }
        return actionResult.getData();
    }

    /**
     * 下载指定的 id的清单，该id是服务器端的清单id ,字符串id用","分隔
     * @param user 当前登录的用户
     * @param idStr ID的字符串，用","分隔
     * @throws IOException
     */
    private static List<DetailListDto> downDetailLists(User user, String idStr) throws IOException {
        KLog.d(TAG, "down detail list by ids:" + idStr);
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> map = new HashMap<>();
        map.put("idStr", idStr);
        Call<ActionResult<List<DetailListDto>>> call = repo.downDetailListFilter(user.getSid(), map);
        Response<ActionResult<List<DetailListDto>>> response = call.execute();

        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "down detail list filter sid response is failed");
            return null;
        }

        ActionResult<List<DetailListDto>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "down detail list filter sid response is failed");
            return null;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "down detail list filter sid response is success but action result is not success:" + actionResult);
            return null;
        }
        return actionResult.getData();
    }

    /**
     * 下载指定的 id的清单，该id是服务器端的清单id ,字符串id用","分隔
     * @param user 当前登录的用户
     * @param idStr ID的字符串，用","分隔
     * @throws IOException
     */
    private static List<AttachDto> downAttachs(User user, String idStr) throws IOException {
        KLog.d(TAG, "down attach list by ids:" + idStr);
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> map = new HashMap<>();
        map.put("idStr", idStr);
        Call<ActionResult<List<AttachDto>>> call = repo.downAttachFilter(user.getSid(), map);
        Response<ActionResult<List<AttachDto>>> response = call.execute();

        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "down attach list filter sid response is failed");
            return null;
        }

        ActionResult<List<AttachDto>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "down attach list filter sid response is failed");
            return null;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "down attach list filter sid response is success but action result is not success:" + actionResult);
            return null;
        }
        return actionResult.getData();
    }

    /**
     * 检查返回笔记数据的可用性
     * @param response 服务器返回的数据
     * @return
     */
    private static boolean checkNoteList(Response<ActionResult<PageInfo<List<NoteInfoDto>>>> response) {
        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "down notes response is failed");
            return false;
        }
        ActionResult<PageInfo<List<NoteInfoDto>>> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "down notes response action result is null");
            return false;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "down notes response is success but action result is not success:" + actionResult);
            return false;
        }
        PageInfo<List<NoteInfoDto>> pageInfo = actionResult.getData();
        if (pageInfo == null || SystemUtil.isEmpty(pageInfo.getData())) {   //没有数据了
            KLog.d(TAG, "down notes response is success and no data");
            return false;
        }
        return true;
    }

    /**
     * 检测本地笔记是否有需要下载更新的
     * @param noteInfoDtoList 服务器的笔记sid集合
     * @return 需要下载更新的笔记的ID字符串，用","分隔，且该ID是<strong>服务器的ID</strong>
     * <pre>
     *     [0]:笔记的ID集合
     *     [1]:清单的ID集合
     *     [2]:附件的ID集合
     * </pre>
     */
    private static Map<Integer, String> checkLocalNotes(List<NoteInfoDto> noteInfoDtoList) {
        //先根据sid的集合查询本地的笔记
        List<String> sidList = new ArrayList<>();
        //附件的sid集合
        List<String> attSidList = new ArrayList<>();
        List<String> detailSidList = new ArrayList<>();
        for (NoteInfoDto noteInfoDto : noteInfoDtoList) {
            sidList.add(noteInfoDto.getSid());
            //附件
            if (!SystemUtil.isEmpty(noteInfoDto.getAttachs())) {
                List<AttachDto> attachDtoList = noteInfoDto.getAttachs();
                for (AttachDto attachDto : attachDtoList) {
                    attSidList.add(attachDto.getSid());
                }
            }
            //清单
            if (noteInfoDto.isDetailListNote() && !SystemUtil.isEmpty(noteInfoDto.getDetails())) {
                List<DetailListDto> detailListDtoList = noteInfoDto.getDetails();
                for (DetailListDto detailListDto : detailListDtoList) {
                    detailSidList.add(detailListDto.getSid());
                }
            }
        }
        //根据sid的集合获取本地对应笔记的基本信息
        StringBuilder builder = new StringBuilder();
        KLog.d(TAG, "note sidList:" + sidList);
        Map<String, NoteInfo> map = NoteManager.getInstance().getBasicNoteList(sidList);
        KLog.d(TAG, "note key set:" + map.keySet());
        Map<String, Attach> attachMap = null;
        if (!SystemUtil.isEmpty(attSidList)) {   //这段记录中有附件
            attachMap = AttachManager.getInstance().getBasicAttachList(attSidList);
        }
        Map<String, DetailList> detailMap = null;
        if (!SystemUtil.isEmpty(detailSidList)) {
            detailMap = NoteManager.getInstance().getBasicDetailLists(detailSidList);
        }
        StringBuilder attBuilder = new StringBuilder();
        StringBuilder detailBuilder = new StringBuilder();
        for (NoteInfoDto noteInfoDto : noteInfoDtoList) {
            String sid = noteInfoDto.getSid();
            int id = noteInfoDto.getId();
            NoteInfo noteInfo = map.get(sid);
            if (noteInfo == null || !SystemUtil.equalsStr(noteInfo.getHash(), noteInfoDto.getHash())) { //本地不存在，则需下载
                builder.append(id).append(Constants.TAG_COMMA);
            }

            //附件
            if (!SystemUtil.isEmpty(noteInfoDto.getAttachs())) {
                List<AttachDto> attachDtoList = noteInfoDto.getAttachs();
                for (AttachDto attachDto : attachDtoList) {
                    int attId = attachDto.getId();
                    String attSid = attachDto.getSid();
                    if (SystemUtil.isEmpty(attachMap)) {
                        attBuilder.append(attId).append(Constants.TAG_COMMA);
                        continue;
                    }
                    Attach att = attachMap.get(attSid);
                    if (att == null || !SystemUtil.equalsStr(att.getHash(), attachDto.getHash())) { //本地不存在，则需下载
                        attBuilder.append(attId).append(Constants.TAG_COMMA);
                    }
                }
            }
            boolean noDetail = SystemUtil.isEmpty(detailMap);
            //清单
            if (noteInfoDto.isDetailListNote() && !SystemUtil.isEmpty(noteInfoDto.getDetails())) {
                List<DetailListDto> detailListDtoList = noteInfoDto.getDetails();
                for (DetailListDto detailListDto : detailListDtoList) {
                    int detailId = detailListDto.getId();
                    String detailSid = detailListDto.getSid();
                    if (noDetail) {
                        detailBuilder.append(detailId).append(Constants.TAG_COMMA);
                        continue;
                    }
                    DetailList detail = detailMap.get(detailSid);
                    if (detail == null || !SystemUtil.equalsStr(detail.getHash(), detailListDto.getHash())) { //本地不存在，则需下载
                        detailBuilder.append(detailId).append(Constants.TAG_COMMA);
                    }
                }
            }

        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.lastIndexOf(Constants.TAG_COMMA));
        }
        if (detailBuilder.length() > 0) {
            detailBuilder.deleteCharAt(detailBuilder.lastIndexOf(Constants.TAG_COMMA));
        }
        if (attBuilder.length() > 0) {
            attBuilder.deleteCharAt(attBuilder.lastIndexOf(Constants.TAG_COMMA));
        }
        String ids = builder.toString();
        Map<Integer, String> idMap = new HashMap<>();
        if (!TextUtils.isEmpty(ids)) {
            idMap.put(0, ids);
        }
        String detailIds = detailBuilder.toString();
        if (!TextUtils.isEmpty(detailIds)) {
            idMap.put(1, detailIds);
        }
        String attIds = attBuilder.toString();
        if (!TextUtils.isEmpty(attIds)) {
            idMap.put(2, attIds);
        }
        KLog.d(TAG, "check local note sid str is:" + idMap);
        return idMap;
    }

    /**
     * 保存笔记到本地
     * @param user 当前登录的用户
     * @param noteInfoDtoList 笔记列表
     */
    private static void saveNotes(User user, List<NoteInfoDto> noteInfoDtoList) {
        List<DetailNoteInfo> detailNoteInfoList = new ArrayList<>();
        for (NoteInfoDto noteInfoDto : noteInfoDtoList) {
            DetailNoteInfo detailNoteInfo = noteInfoDto.convert2NoteInfo(user);
            detailNoteInfoList.add(detailNoteInfo);
        }
        KLog.d(TAG, "save notes invoke...");
        NoteManager.getInstance().addOrUpdateNotes(detailNoteInfoList);
    }
    
    /**
     * 本地保存笔记本数据
     * @param user 用户
     * @param folderDtoList 笔记本的集合
     */
    private static void saveFolders(User user, List<FolderDto> folderDtoList) {
        List<Folder> folderList = new ArrayList<>();
        for (FolderDto folderDto : folderDtoList) {
            Folder folder = folderDto.convert2Folder(user);
            folderList.add(folder);
        }
        //保存并更新到本地
        FolderManager.getInstance().addOrUpdate(folderList);
    }

    /**
     * 更新本地的附件信息
     * @param attach 附件信息
     * @return
     */
    private static boolean updateNativeAttach(Attach attach) {
        KLog.d(TAG, "update native attach data...");
        attach.setSyncState(SyncState.SYNC_DONE);
        return AttachManager.getInstance().updateAttachSyncState(attach);
    }

    /**
     * 同步更新本地的数据
     * @param folder 笔记本
     * @param noteInfos 笔记的集合              
     * @return
     */
    private static boolean updateNative(Folder folder, List<DetailNoteInfo> noteInfos) {
        KLog.d(TAG, "update native note and folder data...");
        //先更新笔记
        boolean success = false;
        if (!SystemUtil.isEmpty(noteInfos)) {
            success = NoteManager.getInstance().updateDetailNotes(noteInfos, SyncState.SYNC_DONE);
            KLog.d(TAG, "update native detail note info list result:" + success + ", size:" + noteInfos.size());
        }
        if (folder != null && !folder.isEmpty()) { //非“所有”笔记本
            folder.setSyncState(SyncState.SYNC_DONE);
            success = FolderManager.getInstance().updateSyncFolder(folder);
            KLog.d(TAG, "update native folder result:" + success + ", folder:" + folder);
            return success;
        } else {
            return true;
        }
    }

    /**
     * 填充note info
     * @param detailNoteInfoList
     * @return
     */
    private static NoteDto fillNoteInfoDto(List<DetailNoteInfo> detailNoteInfoList) {
        NoteDto noteDto = new NoteDto();
        if (SystemUtil.isEmpty(detailNoteInfoList)) {
            return noteDto;
        }
        List<NoteInfoDto> noteInfoDtos = new ArrayList<>();
        for (DetailNoteInfo detailNoteInfo : detailNoteInfoList) {
            NoteInfoDto infoDto = new NoteInfoDto();
            NoteInfo noteInfo = detailNoteInfo.getNoteInfo();
            if (noteInfo == null || TextUtils.isEmpty(noteInfo.getSid())) {
                continue;
            }

            boolean isDetailNote = noteInfo.isDetailNote();
            if (isDetailNote) { //清单不需要上传笔记的内容
                infoDto.setContent(null);
            } else {
                infoDto.setContent(noteInfo.getContent());
            }

            infoDto.setSid(noteInfo.getSid());
            infoDto.setCreateTime(noteInfo.getCreateTime());
            infoDto.setDeleteState(noteInfo.getDeleteState() == null ? DeleteState.DELETE_NONE.ordinal() : noteInfo.getDeleteState().ordinal());
            infoDto.setFolderSid(noteInfo.getFolderId());
            infoDto.setHash(noteInfo.getHash());
            infoDto.setKind(noteInfo.getKind() == null ? NoteInfo.NoteKind.TEXT.ordinal() : noteInfo.getKind().ordinal());
            long currentTime = System.currentTimeMillis();
            long modifyTime = noteInfo.getModifyTime();
            if (modifyTime == 0) {
                modifyTime = currentTime;
            }
            infoDto.setModifyTime(modifyTime);
            infoDto.setRemindId(noteInfo.getRemindId());
            infoDto.setRemindTime(noteInfo.getRemindTime());
            infoDto.setTitle(noteInfo.getTitle());
            
            List<AttachDto> attachList = null;
            List<DetailListDto> detailDtos = null;
            Map<String, Attach> attachMap = noteInfo.getAttaches();
            if (!SystemUtil.isEmpty(attachMap)) {   //有附件
                attachList = new ArrayList<>();
                Set<String> keySet = attachMap.keySet();
                for (String key : keySet) {
                    Attach attach = attachMap.get(key);
                    if (attach == null || attach.isSynced()) {  //附件为空或者附件已同步
                        continue;
                    }
                    AttachDto attachDto = new AttachDto();
                    modifyTime = attach.getModifyTime();
                    if (modifyTime == 0) {
                        modifyTime = currentTime;
                    }
                    attachDto.setModifyTime(modifyTime);
                    attachDto.setCreateTime(attach.getCreateTime());
                    attachDto.setDeleteState(attach.getDeleteState() == null ? DeleteState.DELETE_NONE.ordinal() : attach.getDeleteState().ordinal());
                    attachDto.setDescription(attach.getDescription());
                    attachDto.setFilename(attach.getFilename());
                    attachDto.setHash(attach.getHash());
                    attachDto.setMimeType(attach.getMimeType());
                    attachDto.setNoteSid(attach.getNoteId());
                    attachDto.setSid(attach.getSid());
                    attachDto.setSize(attach.getSize());
                    attachDto.setType(attach.getType());

                    attachList.add(attachDto);
                }
            }
            if (isDetailNote && !SystemUtil.isEmpty(detailNoteInfo.getDetailList())) {    //清单类笔记
                List<DetailList> detailLists = detailNoteInfo.getDetailList();
                detailDtos = new ArrayList<>();
                for (DetailList detailList : detailLists) {
                    DetailListDto detailDto = new DetailListDto();
                    detailDto.setSid(detailList.getSid());
                    detailDto.setNoteSid(detailList.getNoteId());
                    detailDto.setChecked(detailList.isChecked());
                    detailDto.setCreateTime(detailList.getCreateTime());
                    modifyTime = detailList.getModifyTime();
                    if (modifyTime == 0) {
                        modifyTime = currentTime;
                    }
                    detailDto.setModifyTime(modifyTime);
                    detailDto.setDeleteState(detailList.getDeleteState() == null ? DeleteState.DELETE_NONE.ordinal() : detailList.getDeleteState().ordinal());
                    detailDto.setSort(detailList.getSort());
                    detailDto.setHash(detailList.getHash());
                    detailDto.setTitle(detailList.getTitle());
                    detailDtos.add(detailDto);
                }
            }
            infoDto.setAttachs(attachList);
            infoDto.setDetails(detailDtos);
            
            noteInfoDtos.add(infoDto);
        }
        
        noteDto.setNoteInfos(noteInfoDtos);
        return noteDto;
    }

    /**
     * 填充笔记本
     * @param folder
     * @return
     */
    private static FolderDto fillFolderDto(Folder folder) {
        if (folder == null) {
            return null;
        }
        FolderDto folderDto = new FolderDto();
        folderDto.setSid(folder.getSid());
        folderDto.setSort(folder.getSort());
        folderDto.setDeleteState(folder.getDeleteState() == null ? DeleteState.DELETE_NONE.ordinal() : folder.getDeleteState().ordinal());
        folderDto.setCount(folder.getCount());
        folderDto.setCreateTime(folder.getCreateTime());
        long modifyTime = folder.getModifyTime();
        if (modifyTime == 0) {
            modifyTime = System.currentTimeMillis();
        }
        folderDto.setModifyTime(modifyTime);
        folderDto.setLock(folder.isLock());
        folderDto.setName(folder.getName());
        folderDto.setHash(folder.getHash());
        return folderDto;
    }

    /**
     * 获取当前的用户
     * @param context
     * @return
     */
    private static User getUser(Context context) {
        NoteApplication app = (NoteApplication) context.getApplicationContext();
        User user = app.getCurrentUser();
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "sync task get user is not available");
            return null;
        }
        return user;
    }
}
