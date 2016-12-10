package com.yunxinlink.notes.api.impl;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.socks.library.KLog;
import com.yunxinlink.notes.api.INoteApi;
import com.yunxinlink.notes.api.model.AttachDto;
import com.yunxinlink.notes.api.model.DetailListDto;
import com.yunxinlink.notes.api.model.FolderDto;
import com.yunxinlink.notes.api.model.NoteDto;
import com.yunxinlink.notes.api.model.NoteInfoDto;
import com.yunxinlink.notes.api.model.PageInfo;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.Observer;
import com.yunxinlink.notes.listener.OnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeleteState;
import com.yunxinlink.notes.model.DetailList;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.FeedbackInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.model.SyncState;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.AttachManager;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.DigestUtil;
import com.yunxinlink.notes.util.FileUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
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
     * @return 返回结果码
     */
    public static int syncUpNote(Context context, Folder folder, List<DetailNoteInfo> noteInfos) throws IOException {
        KLog.d(TAG, "sync up note invoke...");
        int resultCode = ActionResult.RESULT_ERROR;
        if (context == null) {
            KLog.d(TAG, "sync up note invoke but context is null and will return");
            return resultCode;
        }
        User user = getUser(context);
        if (user == null) {  //用户不可用
            KLog.d(TAG, "sync up note failed user is not available");
            resultCode = ActionResult.RESULT_DATA_NOT_EXISTS;
            return resultCode;
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

        NoteDto noteDto = fillNoteDto(noteInfos);
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
            KLog.d(TAG, "sync up note failed responseBody is null");
            return resultCode;
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
                        if (!noteInfo.hasAttach() || SystemUtil.isEmpty(attachMap) || noteInfo.checkDeleteState(DeleteState.DELETE_DONE)) {   //没有附件
                            continue;
                        }
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
                resultCode = ActionResult.RESULT_SUCCESS;
            } else {
                KLog.d(TAG, "sync up note response is successful but result error");
                int actionCode = actionResult.getResultCode();
                switch (actionCode) {
                    case ActionResult.RESULT_FAILED:    //同步结果是失败的
                        KLog.d(TAG, "sync up note response is successful but result is failed");
                        break;
                    case ActionResult.RESULT_ERROR:    //同步过程中遇到错误
                        KLog.d(TAG, "sync up note response is successful but result is error");
                        break;
                    case ActionResult.RESULT_PARAM_ERROR:    //同步的参数错误
                        KLog.d(TAG, "sync up note response is successful but result params is error");
                        break;
                    case ActionResult.RESULT_STATE_DISABLE:    //用户被禁用
                    case ActionResult.RESULT_DATA_NOT_EXISTS:    //用户不存在或者被禁用
                        KLog.d(TAG, "sync up note response is successful but result user is not exists or disable");
                        break;
                }
                resultCode = actionCode;
            }
        } else {
            resultCode = ActionResult.RESULT_FAILED;
            KLog.d(TAG, "sync up note failed is not successful");
            return resultCode;
        }
        return resultCode;
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
            downNotes(user, 1);
        } catch (IOException e) {
            KLog.e(TAG, "down notes error:" + e.getMessage());
        }
    }

    /**
     * 下载笔记,该方法在主线程中执行，调用处需要开线程
     * @param user 用户
     * @param pageNumber 第几页，从1开始
     * @return
     */
    private static void downNotes(User user, int pageNumber) throws IOException {
        KLog.d(TAG, "down notes page：" + pageNumber);
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("offset", String.valueOf(pageNumber));
        queryMap.put("limit", String.valueOf(pageSize));

        Call<ActionResult<PageInfo<List<NoteInfoDto>>>> call = repo.downNotes(user.getSid(), queryMap);

        Response<ActionResult<PageInfo<List<NoteInfoDto>>>> response = call.execute();

        boolean result = checkNoteList(response);
        
        if (!result) {  //没有数据了，加载完毕
            return;
        }
        
        ActionResult<PageInfo<List<NoteInfoDto>>> actionResult = response.body();
        PageInfo<List<NoteInfoDto>> pageInfo = actionResult.getData();
        List<NoteInfoDto> noteInfoDtoList = pageInfo.getData();
        //保存内容到本地
        saveNotes(user, noteInfoDtoList, true, null);
        
        pageNumber = pageNumber + 1;
        KLog.d(TAG, "down notes next page：" + pageNumber);
        try {
            downNotes(user, pageNumber);
        } catch (IOException e) {
            KLog.e(TAG, "down notes error page number:" + pageNumber + ", error:" + e.getMessage());
        }
    }

    /**
     * 根据笔记的id来分页下载笔记
     * @param context
     */
    public static void downNotesWithIds(Context context) {
        User user = getUser(context);
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "down note with id but user is null or not available");
            return;
        }
        try {
            downNoteByIds(user, 1);
        } catch (IOException e) {
            KLog.e(TAG, "down notes with ids error :" + e.getMessage());
        }
    }

    /**
     * 下载附件的文件
     * @param context
     */
    public static boolean downAttachFile(Context context, Attach attach) throws IOException {
        User user = getUser(context);
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "down attach file with id but user is null or not available");
            return false;
        }
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        
        String userSid = user.getSid();
        String sid = attach.getSid();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("userSid", userSid);
        Call<ResponseBody> call = repo.downAttachFile(sid, queryMap);

        Response<ResponseBody> response = call.execute();
        
        KLog.d(TAG, "download attach file invoke :" + attach.getSid() + ", filename:" + attach.getFilename());
        if (response == null || !response.isSuccessful() || response.body() == null) {
            KLog.d(TAG, "download attach file response is null or body is null so down failed");
            return false;
        }
        String filename = attach.getFilename();
        if (TextUtils.isEmpty(filename)) {  //从header中获取
            Headers headers = response.headers();
            filename = SystemUtil.getFilename(headers);
        }
        boolean saveResult = false;
        String filePath = null;
        try {
            File saveFile = SystemUtil.getAttachFile(attach.getNoteId(), attach.getType(), filename);
            if (saveFile != null) {
                filePath = saveFile.getAbsolutePath();
                saveResult = FileUtil.writeResponseBodyToDisk(response.body(), saveFile);
                KLog.d(TAG, "download attach file save file result:" + saveResult + ", and path:" + filePath);
            }
        } catch (Exception e) {
            KLog.e(TAG, "download attach file save file error:" + e.getMessage());
        }
        if (saveResult) {   //将数据保存到数据库中
            attach.setLocalPath(filePath);
            attach.setSyncState(SyncState.SYNC_DONE);
            KLog.d(TAG, "download attach file success and will save attach file info to local db:" + filePath);
            saveResult = AttachManager.getInstance().updateAttachSyncState(attach);
            if (saveResult) {
                //查询该附件对应的笔记
                String noteSid = attach.getNoteId();
                DetailNoteInfo detailNoteInfo = NoteManager.getInstance().getDetailNote(noteSid);
                if (detailNoteInfo != null) {
                    KLog.d(TAG, "note attach download notify :" + detailNoteInfo);
                    NoteManager.getInstance().notifyObservers(Provider.NoteColumns.NOTIFY_FLAG, Observer.NotifyType.MERGE, detailNoteInfo);
                }
            }
//            saveResult = UserManager.getInstance().update(user);
        }
        return saveResult;
    }

    /**
     * 下载笔记服务器的笔记的ID集合，并带有hash等数据
     * @param user 当前登录用户
     * @param pageNumber 第几页
     * @throws IOException
     */
    private static void downNoteByIds(User user, int pageNumber) throws IOException {
        KLog.d(TAG, "down note sids page：" + pageNumber);
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("offset", String.valueOf(pageNumber));
        queryMap.put("limit", String.valueOf(pageSize));
        String userSid = user.getSid();
        Call<ActionResult<PageInfo<List<NoteInfoDto>>>> call = repo.downNoteSids(userSid, queryMap);

        Response<ActionResult<PageInfo<List<NoteInfoDto>>>> response = call.execute();

        boolean result = checkNoteList(response);

        if (!result) {  //加载完毕了
            return;
        }

        ActionResult<PageInfo<List<NoteInfoDto>>> actionResult = response.body();
        PageInfo<List<NoteInfoDto>> pageInfo = actionResult.getData();
        
        List<NoteInfoDto> noteInfoDtoList = pageInfo.getData();
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
            List<NoteInfoDto> downNoteDtoList = null;
            Map<String, NoteInfoDto> noteDtoMap = null;
            if (!TextUtils.isEmpty(noteIdStr)) {
                downNoteDtoList = downNotes(user, noteIdStr);
                //转换成映射的集合,key 为noteSid
                noteDtoMap = new HashMap<>();

                if (!SystemUtil.isEmpty(downNoteDtoList)) {
                    for (NoteInfoDto noteInfoDto : downNoteDtoList) {
                        noteDtoMap.put(noteInfoDto.getSid(), noteInfoDto);
                    }
                }
            }
            //这一组清单所属的笔记集合
            Set<String> noteSidSet = new HashSet<>();
            //下载清单数据
            String detailIdStr = idMap.get(1);
            if (!TextUtils.isEmpty(detailIdStr)) {  //有清单数据
                List<DetailListDto> detailListDtoList = downDetailLists(user, detailIdStr);
                List<DetailList> detailListList = new ArrayList<>();
                if (!SystemUtil.isEmpty(detailListDtoList)) {   //有清单，则填充清单
                    if (SystemUtil.isEmpty(noteDtoMap)) {   //没有笔记，则说明单独下载的清单，则查询这一组清单所属的笔记

                        for (DetailListDto detailListDto : detailListDtoList) {
                            String noteSid = detailListDto.getNoteSid();
                            if (noteSid == null) {
                                continue;
                            }
                            noteSidSet.add(noteSid);
                            DetailList detailList = detailListDto.convert2DetailList(noteSid);
                            detailListList.add(detailList);
                        }

                    } else {
                        for (DetailListDto detailListDto : detailListDtoList) {
                            String noteSid = detailListDto.getNoteSid();
                            if (noteSid == null) {
                                continue;
                            }
                            NoteInfoDto noteInfoDto = noteDtoMap.get(noteSid);
                            if (noteInfoDto == null) {
                                //该清单是单独更新，所属的笔记已经更新完毕了
                                noteSidSet.add(noteSid);
                                DetailList detailList = detailListDto.convert2DetailList(detailListDto.getNoteSid());
                                detailListList.add(detailList);
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
                    if (!SystemUtil.isEmpty(detailListList)) {  //有清单需要单独更新
                        //保存清单到本地，但没有刷新，后面一起刷新
                        saveDetailList(detailListList);
                    }
                }
            }
            String attIdStr = idMap.get(2);
            if (!TextUtils.isEmpty(attIdStr)) {
                //下载附件的信息
                List<AttachDto> attachDtoList = downAttachs(user, attIdStr);
                if (!SystemUtil.isEmpty(attachDtoList)) {   //有附件
                    List<Attach> attachList = new ArrayList<>();
                    if (SystemUtil.isEmpty(noteDtoMap)) {   //没有更新笔记，则单独更新附件
                        for (AttachDto attachDto : attachDtoList) {
                            String noteSid = attachDto.getNoteSid();
                            if (noteSid == null) {
                                continue;
                            }
                            noteSidSet.add(noteSid);
                            Attach attach = attachDto.convert2Attach(user.getId(), noteSid);
                            attachList.add(attach);
                        }
                    } else {
                        for (AttachDto attachDto : attachDtoList) {
                            String noteSid = attachDto.getNoteSid();
                            if (noteSid == null) {
                                continue;
                            }
                            NoteInfoDto noteInfoDto = noteDtoMap.get(noteSid);
                            if (noteInfoDto == null) {
                                //该附件是单独更新了，所属的笔记已经更新了
                                noteSidSet.add(noteSid);
                                Attach attach = attachDto.convert2Attach(user.getId(), noteSid);
                                attachList.add(attach);
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
                    if (!SystemUtil.isEmpty(attachList)) {  //保存该附件集合
                        saveAttachList(attachList);
                    }
                }
            }
            //保存到本地
            saveNotes(user, downNoteDtoList, false, noteSidSet);
            
        } else {
            KLog.d(TAG, "down note by ids but not need down or update will do next page");
        }

        pageNumber = pageNumber + 1;
        KLog.d(TAG, "down note sids next page：" + pageNumber);
        try {
            downNoteByIds(user, pageNumber);
        } catch (IOException e) {
            KLog.e(TAG, "down note sids error page number:" + pageNumber + ", error:" + e.getMessage());
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
            KLog.d(TAG, "down attach list filter sid action result is failed");
            return null;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "down attach list filter sid response is success but action result is not success:" + actionResult);
            return null;
        }
        return actionResult.getData();
    }

    /**
     * 同步修改笔记的删除状态,该方法在主线程里执行
     * @param context 上下文
     * @param detailNoteInfos 笔记列表
     * @return 结果码
     * @see ActionResult
     */
    public static int updateNoteDeleteState(Context context, List<DetailNoteInfo> detailNoteInfos) {
        User user = getUser(context);
        int resultCode = ActionResult.RESULT_ERROR;
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "update note delete state but user is null or not available");
            resultCode = ActionResult.RESULT_STATE_DISABLE;
            return resultCode;
        }
        if (SystemUtil.isEmpty(detailNoteInfos)) {  //参数为空
            KLog.d(TAG, "update note delete state but note list is empty");
            resultCode = ActionResult.RESULT_SUCCESS;
            return resultCode;
        }
        
        int size = detailNoteInfos.size();
        int pageSize = Constants.PAGE_SIZE_DEFAULT;
        try {
            if (size > pageSize) {   //需要分页提交，每页20条数据
                int pageCount = size % pageSize == 0 ? size / pageSize : size / pageSize + 1;
                for (int i = 0; i < pageCount; i++) {
                    int pageNumber = i + 1;
                    KLog.d(TAG, "update note delete state with page number:" + pageNumber);
                    int offset = i * pageSize;
                    int end = offset + pageSize;
                    List<DetailNoteInfo> subList = detailNoteInfos.subList(offset, end);
                    resultCode = doUpdateNoteDeleteState(user, subList);
                }
            } else {    //不需要分页
                resultCode = doUpdateNoteDeleteState(user, detailNoteInfos);
            }
        } catch (Exception e) {
            KLog.d(TAG, "update note delete state error:" + e.getMessage());
        }
        return resultCode;
    }

    /**
     * 上传用户反馈的信息，该方法在后台线程中执行，无需另外再创建线程
     * @param context 上下文
     * @param feedbackInfo 反馈的信息
     * @param fileList 附件列表
     * @param listener 回调                
     */
    public static void makeFeedback(Context context, FeedbackInfo feedbackInfo, List<File> fileList, final OnLoadCompletedListener<ActionResult<Void>> listener) {
        if (feedbackInfo == null) {
            return;
        }
        User user = getUser(context);
        if (user != null) { //当前有用户登录
            feedbackInfo.setUserSid(user.getSid());
        }
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Map<String, RequestBody> param = new HashMap<>();
        Gson gson = new Gson();
        String json = gson.toJson(feedbackInfo);
        param.put("json", RequestBody.create(null, json));
        if (!SystemUtil.isEmpty(fileList)) {    //有附件
            for (File file : fileList) {
                String filename = file.getName();
                String mime = FileUtil.getMimeType(file);
                RequestBody attachFile = RequestBody.create(MediaType.parse(mime), file);
                //attachFile: 与服务器端的参数名相同
                param.put("attachFile\"; filename=\"" + filename + "", attachFile);
            }
        }
        Call<ActionResult<Void>> call = repo.makeFeedback(param);
        call.enqueue(new Callback<ActionResult<Void>>() {
            @Override
            public void onResponse(Call<ActionResult<Void>> call, Response<ActionResult<Void>> response) {
                int resultCode = 0;
                if (response == null || !response.isSuccessful()) { //http请求失败
                    resultCode = ActionResult.RESULT_ERROR;
                } else if (response.body() == null) {   //返回的内容为空
                    resultCode = ActionResult.RESULT_FAILED;
                } else {
                    ActionResult<Void> actionResult = response.body();
                    if (actionResult.isSuccess()) { //成功
                        resultCode = ActionResult.RESULT_SUCCESS;
                    } else {
                        resultCode = ActionResult.RESULT_FAILED;
                    }
                }
                if (listener != null) {
                    if (resultCode == ActionResult.RESULT_SUCCESS) {
                        listener.onLoadSuccess(null);
                    } else {
                        KLog.d(TAG, "note api make feedback on response success but result failed:" + resultCode);
                        listener.onLoadFailed(resultCode, null); 
                    }
                }
            }

            @Override
            public void onFailure(Call<ActionResult<Void>> call, Throwable t) {
                if (listener != null) {
                    listener.onLoadFailed(ActionResult.RESULT_ERROR, null);
                }
                KLog.e(TAG, "note api make feedback on failed:" + t);
            }
        });
    }

    /**
     * 更新笔记本的排序，该方法异步执行
     * @param context
     * @param folderList
     */
    public static void updateFoldderSortAsync(Context context, List<Folder> folderList) {
        User user = getUser(context);
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "note api sync up folder sort but user is null or not available");
            return;
        }
        if (SystemUtil.isEmpty(folderList)) {
            KLog.d(TAG, "note api sync up folder sort but folder list is empty");
            return;
        }
        List<FolderDto> folderDtoList = new ArrayList<>();
        for (Folder folder : folderList) {
            folderDtoList.add(folder.convert2Dto());
        }
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Call<ActionResult<Void>> call = repo.sortFolders(user.getSid(), folderDtoList);
        call.enqueue(new Callback<ActionResult<Void>>() {
            @Override
            public void onResponse(Call<ActionResult<Void>> call, Response<ActionResult<Void>> response) {
                if (response == null || !response.isSuccessful()) { //http 请求失败
                    KLog.d(TAG, "note api sync up folder sort response is null or not successful");
                    return;
                }
                ActionResult<Void> actionResult = response.body();
                if (actionResult == null || !actionResult.isSuccess()) {    //返回的数据异常
                    KLog.d(TAG, "note api sync up folder sort action result is null or not successful:" + actionResult);
                    return;
                }
                KLog.d(TAG, "note api sync up folder sort successful:" + actionResult);
            }

            @Override
            public void onFailure(Call<ActionResult<Void>> call, Throwable t) {
                KLog.e(TAG, "note api sync up folder sort on failed:" + t);
            }
        });
    }

    /**
     * 更新笔记的删除状态
     * @param user 当前的用户
     * @param detailNoteInfos 笔记列表
     * @return
     * @throws IOException
     */
    private static int doUpdateNoteDeleteState(User user, List<DetailNoteInfo> detailNoteInfos) throws IOException {
        int resultCode = ActionResult.RESULT_ERROR;
        List<NoteInfoDto> noteInfoDtos = fillNoteInfoDto(detailNoteInfos);
        if (SystemUtil.isEmpty(noteInfoDtos)) {
            KLog.d(TAG, "update note delete state but note info dto list is empty");
            resultCode = ActionResult.RESULT_SUCCESS;
            return resultCode;
        }
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);
        Call<ActionResult<Void>> call = repo.updateNoteState(user.getSid(), noteInfoDtos);

        Response<ActionResult<Void>> response = call.execute();

        if (response == null || !response.isSuccessful()) { //http 请求失败
            KLog.d(TAG, "update note delete state but response is failed");
            resultCode = ActionResult.RESULT_FAILED;
            return resultCode;
        }
        ActionResult<Void> actionResult = response.body();
        if (actionResult == null) {
            KLog.d(TAG, "update note delete state action result is failed");
            resultCode = ActionResult.RESULT_FAILED;
            return resultCode;
        }
        if (!actionResult.isSuccess()) {    //结果是失败的
            KLog.d(TAG, "update note delete state action response is success but action result is not success:" + actionResult);
            resultCode = actionResult.getResultCode();
            return resultCode;
        }
        //TODO 保存状态到本地
        DetailNoteInfo detailNoteInfo = detailNoteInfos.get(0);
        boolean success = true;
        if (detailNoteInfo.isRemovedNote()) {   //是否是彻底删除的信息
            KLog.d(TAG, "update note state and will remove local notes");
            success = NoteManager.getInstance().removeNotes(detailNoteInfos);
        }
        if (success) {  //本地删除数据库成功
            resultCode = ActionResult.RESULT_SUCCESS;
        } else {
            resultCode = ActionResult.RESULT_FAILED;
        }
        KLog.d(TAG, "update note state remove local notes result:" + success);
        return resultCode;
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
            KLog.d(TAG, "down notes response is success and no data or load completed");
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
        KLog.d(TAG, "check local note id str is:" + idMap);
        return idMap;
    }

    /**
     * 保存笔记到本地
     * @param user 当前登录的用户
     * @param noteInfoDtoList 笔记列表
     * @param hasExtraData 笔记是否自带附件、清单等信息                       
     * @param noteSidSet 若果有清单、附件单独更新，则这些清单和附件所属的笔记sid集合                       
     */
    private static void saveNotes(User user, List<NoteInfoDto> noteInfoDtoList, boolean hasExtraData, Set<String> noteSidSet) {
        if (SystemUtil.isEmpty(noteInfoDtoList)) {  //没有更新笔记
            if (!SystemUtil.isEmpty(noteSidSet)) {  //有需要更新的笔记
                //查询这些笔记的的信息，然后刷新
                NoteManager.getInstance().refreshNotes(noteSidSet);
            } else {
                KLog.d(TAG, "save notes no data or note sid set id empty");
            }
        } else {    //有笔记保存
            if (noteSidSet == null) {
                noteSidSet = new HashSet<>();
            }
            List<DetailNoteInfo> detailNoteInfoList = new ArrayList<>();
            for (NoteInfoDto noteInfoDto : noteInfoDtoList) {
                DetailNoteInfo detailNoteInfo = noteInfoDto.convert2NoteInfo(user);
                detailNoteInfoList.add(detailNoteInfo);

                if (!hasExtraData) {
                    noteSidSet.add(noteInfoDto.getSid());
                }
            }
            KLog.d(TAG, "save notes invoke...");
            NoteManager.getInstance().addOrUpdateNotes(detailNoteInfoList, hasExtraData);
            if (!hasExtraData) {    //手动刷界面
                NoteManager.getInstance().refreshNotes(noteSidSet);
            }
        }
    }

    /**
     * 保存一组清单，暂时不刷新界面
     * @param detailLists 要保存的清单列表
     */
    private static void saveDetailList(List<DetailList> detailLists) {
        KLog.d(TAG, "save detail lists invoke...");
        NoteManager.getInstance().addOrUpdateDetailList(detailLists);
    }

    /**
     * 保存一组附件信息，暂时不刷新界面
     * @param attachList 要保存的附件列表
     */
    private static void saveAttachList(List<Attach> attachList) {
        KLog.d(TAG, "save attach lists invoke...");
        AttachManager.getInstance().addOrUpdateAttachs(attachList);
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
    private static NoteDto fillNoteDto(List<DetailNoteInfo> detailNoteInfoList) {
        NoteDto noteDto = new NoteDto();
        if (SystemUtil.isEmpty(detailNoteInfoList)) {
            return noteDto;
        }
        List<NoteInfoDto> noteInfoDtos = new ArrayList<>();
        for (DetailNoteInfo detailNoteInfo : detailNoteInfoList) {
            NoteInfo noteInfo = detailNoteInfo.getNoteInfo();
            if (noteInfo == null || TextUtils.isEmpty(noteInfo.getSid())) {
                continue;
            }
            NoteInfoDto infoDto = new NoteInfoDto();

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
     * 填充笔记列表
     * @param detailNoteInfos 笔记的列表
     * @return 返回填充后的笔记列表
     */
    private static List<NoteInfoDto> fillNoteInfoDto(List<DetailNoteInfo> detailNoteInfos) {
        List<NoteInfoDto> noteInfoDtos = new ArrayList<>();
        long modifyTime = System.currentTimeMillis();
        for (DetailNoteInfo detailNoteInfo : detailNoteInfos) {
            NoteInfo noteInfo = detailNoteInfo.getNoteInfo();
            if (noteInfo == null || TextUtils.isEmpty(noteInfo.getSid())) {
                continue;
            }
            NoteInfoDto infoDto = new NoteInfoDto();
            infoDto.setSid(noteInfo.getSid());
            infoDto.setModifyTime(modifyTime);
            infoDto.setDeleteState(noteInfo.getDeleteState().ordinal());
            noteInfoDtos.add(infoDto);
        }
        return noteInfoDtos;
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
}
