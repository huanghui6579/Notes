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
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        NoteApplication app = (NoteApplication) context.getApplicationContext();
        User user = app.getCurrentUser();
        if (user == null || !user.isAvailable()) {  //用户不可用
            KLog.d(TAG, "sync up note failed user is not available");
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_DATA_NOT_EXISTS, "user is null or not available");
            }
            return null;
        }
        
        String userSid = user.getSid();
        
        if (SystemUtil.isEmpty(noteInfos)) {    //笔记为空
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params is null");
            }
            KLog.d(TAG, "sync up note failed params is null");
            return null;
        }
        
        Retrofit retrofit = buildRetrofit();
        INoteApi repo = retrofit.create(INoteApi.class);

        NoteDto noteDto = fillNoteInfoDto(noteInfos);
        if (noteDto == null) {
            if (listener != null) {
                listener.onLoadFailed(ActionResult.RESULT_PARAM_ERROR, "params noteDto is null");
            }
            KLog.d(TAG, "sync up note failed params noteDto is null");
            return null;
        }
        
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
     * 同步更新本地的数据
     * @param folder 笔记本
     * @param noteInfos 笔记的集合              
     * @return
     */
    private static boolean updateNative(Folder folder, List<DetailNoteInfo> noteInfos) {
        KLog.d(TAG, "update native note and folder data...");
        //先更新笔记
        boolean success = NoteManager.getInstance().updateDetailNotes(noteInfos, SyncState.SYNC_DONE);
        KLog.d(TAG, "update native detail note info list result:" + success + ", size:" + noteInfos.size());
        if (folder != null && !folder.isRootFolder()) { //非“所有”笔记本
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
        if (SystemUtil.isEmpty(detailNoteInfoList)) {
            return null;
        }
        NoteDto noteDto = new NoteDto();
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
                    if (attach == null) {
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
        if (folder.getModifyTime() == 0) {
            folderDto.setModifyTime(System.currentTimeMillis());
        }
        folderDto.setLock(folder.isLock());
        folderDto.setName(folder.getName());
        return folderDto;
    }
}
