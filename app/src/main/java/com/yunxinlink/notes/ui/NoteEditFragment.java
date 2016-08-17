package com.yunxinlink.notes.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.listener.AttachAddCompleteListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.EditStep;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.richtext.AttachResolver;
import com.yunxinlink.notes.richtext.AttachSpec;
import com.yunxinlink.notes.richtext.NoteRichSpan;
import com.yunxinlink.notes.richtext.RichTextWrapper;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.NoteLinkify;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.widget.AttachSpan;
import com.yunxinlink.notes.widget.MessageBundleSpan;
import com.yunxinlink.notes.widget.NoteEditText;
import com.yunxinlink.notes.widget.NoteFrameLayout;
import com.yunxinlink.notes.widget.NoteLinkMovementMethod;
import com.yunxinlink.notes.widget.NoteTextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NoteEditFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NoteEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NoteEditFragment extends Fragment implements TextWatcher, View.OnClickListener, ActionFragment {
    private static final int MSG_AUTO_LINK = 2;
    
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    
    private static final String TAG = "NoteEditFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    //笔记的编辑框
    private NoteEditText mEtContent;

    //笔记的显示视图
    private NoteTextView mTvContent;

    private NoteFrameLayout mContentLayout;

    /**
     * 富文本的包装器
     */
    private RichTextWrapper mRichTextWrapper;

    private boolean mIsEnterLine;

    private AutoLinkTask mAutoLinkTask;

    private Handler mHandler = new MyHandler(this);

    private OnFragmentInteractionListener mListener;

    /**
     * 是查看模式或者编辑模式
     */
    private boolean mIsViewMode = true;
    
    //笔记的文本内容
    private CharSequence mText;
    
    //搜索关键字的span集合
    private List<SearchResult> mKeywordResults;
    
    //搜索结果的下一个聚焦索引
    private int mNextPosition = 0;
    
    //搜索结果每行的集合，不重复
    private List<Integer> mSearchLines;

    public NoteEditFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment NoteEditFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NoteEditFragment newInstance() {
        NoteEditFragment fragment = new NoteEditFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        
        KLog.d(TAG, "--NoteEditFragment---onCreate----");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        KLog.d(TAG, "--NoteEditFragment---onCreateView----");
        return inflater.inflate(R.layout.fragment_note_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initView(view);

        KLog.d(TAG, "--NoteEditFragment---onViewCreated----");
    }

    /**
     * 初始化view
     * @param view
     */
    private void initView(View view) {
        mEtContent = (NoteEditText) view.findViewById(R.id.et_content);
        mTvContent = (NoteTextView) view.findViewById(R.id.tv_content);

        if (mTvContent == null || mEtContent == null) {
            return;
        }

        mContentLayout = (NoteFrameLayout) view.findViewById(R.id.content_layout);

//        mRichTextWrapper = new RichTextWrapper(mEtContent, mHandler);
        mRichTextWrapper = new RichTextWrapper(mTvContent, mHandler);
        mRichTextWrapper.setMovementMethod(NoteLinkMovementMethod.getInstance());
        mRichTextWrapper.addResolver(AttachResolver.class);

        //初始化文本显示控件
        initTextView();


        if (mText != null) {
            mEtContent.setText(mText);
        }
        
        //初始化编辑框
//        initEditText(mEtContent);
        
        if (mListener != null) {
            mListener.onInitCompleted(mIsViewMode);
        }
    }

    public boolean isViewMode() {
        return mIsViewMode;
    }

    public void setViewMode(boolean isViewMode) {
        this.mIsViewMode = isViewMode;
    }

    @Override
    public void showNote(DetailNoteInfo detailNote, Map<String, Attach> map) {
        KLog.d(TAG, "-----showNote---note--" + detailNote);
        NoteInfo note = detailNote.getNoteInfo();
        CharSequence s = note.getContent();
        mText = s;
        setText(s);
        mRichTextWrapper.setText(s, map);
        NoteRichSpan richSpan = mRichTextWrapper.getRichSpan();
        if (richSpan instanceof NoteTextView) {
            autoLink(s);
        } else {
            initEditText((NoteEditText) richSpan);
        }
    }

    /**
     * 初始化编辑模式
     */
    public void initEditInfo() {
        KLog.d(TAG, "--NoteEditFragment---initEditInfo--");
        if (mContentLayout != null) {
            mRichTextWrapper.setRichSpan(mEtContent);
            mContentLayout.changeToEditMode();
        }
    }
    
    /**
     * 设置文本
     * @param text
     */
    public void setText(CharSequence text) {
        this.mText = text;
    }
    
    @Override
    public CharSequence getText() {
        CharSequence text = null;
        if (mRichTextWrapper != null) {
            text = mRichTextWrapper.getRichSpan().getTextContent();
        }
        text = text == null ? "" : text;
        return text;
    }

    @Override
    public CharSequence getTitle() {
        return null;
    }

    @Override
    public NoteInfo.NoteKind getNoteType() {
        return NoteInfo.NoteKind.TEXT;
    }

    public NoteEditText getEditTextView() {
        return mEtContent;
    }

    /**
     * 进入编辑模式
     */
    public void setupEditMode() {
        mContentLayout.changeToEditMode();
        mRichTextWrapper.setRichSpan(mEtContent);
    }

    /**
     * 显示图片的span
     * @param editable
     * @param editStep
     * @param uri
     */
    public void showSpanImage(final Editable editable, ImageSize imageSize, final EditStep editStep, String uri, final InsertTextCallback callback) {
        ImageUtil.generateThumbImageAsync(uri, imageSize, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                ImageSpan imageSpan = new ImageSpan(getContext(), loadedImage);
                CharSequence text = editStep.getContent();
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(text);
                builder.setSpan(imageSpan, 0, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                int selStart = editStep.getStart();
                if (callback != null) {
                    callback.beforeInsertText();
                }
                try {
                    if (selStart < 0 || mEtContent.getText() == null || selStart >= mEtContent.getText().length()) {
                        editable.append(builder);
                    } else {
                        editable.insert(selStart, builder);
                    }
                } catch (Exception e) {
                    KLog.e(TAG, "-----resetAttach--error----" + e.getMessage());
                }
                if (callback != null) {
                    callback.afterInsertText();
                }
//                editable.insert(editStep.getStart(), editStep.getContent());
//                    listener.onAddComplete(imageUri, editStep, attach);
            }
        });
    }

    /**
     * 根据不同的模式获取对应的显示控件
     * @return
     */
    public TextView getShowView() {
        if (mRichTextWrapper != null) {
            return mRichTextWrapper.getgetRichSpanView();
        }
        return null;
    }

    /**
     * 在光标处插入当前时间
     * @author tiger
     * @update 2016/3/13 10:27
     * @version 1.0.0
     */
    @Override
    public String insertTime() {
        String time = SystemUtil.getFormatTime();
        NoteUtil.insertText(mEtContent, time);
        return time;
    }

    @Override
    public CharSequence insertContact(String[] info) {
        CharSequence text = NoteUtil.formatContactInfo(info);
        if (!TextUtils.isEmpty(text)) {
            NoteUtil.insertText(mEtContent, text);
        }
        return text;
    }

    /**
     * 移除搜索的span
     * @param text 笔记的文本
     */
    private void removeSearchSpan(Spannable text) {
        if (mKeywordResults != null && mKeywordResults.size() > 0) {
            for (SearchResult result : mKeywordResults) {
                BackgroundColorSpan colorSpan = result.colorSpan;
                text.removeSpan(colorSpan);
            }
            mNextPosition = 0;
            mKeywordResults.clear();
            if (mSearchLines != null) {
                mSearchLines.clear();
            }
        }
    }

    @Override
    public void doSearch(String keyword) {
        CharSequence noteText = getText();
        if (noteText == null || !(noteText instanceof Spannable)) {
            return;
        }
        Spannable spannable = (Spannable) noteText;
        boolean noText = TextUtils.isEmpty(noteText);
        if (!noText) {
            removeSearchSpan(spannable);
        } else {
            KLog.d(TAG, "----doSearch--text--is---empty---");
            return;
        }
        if (TextUtils.isEmpty(keyword)) {
            KLog.d(TAG, "----doSearch--keyword--is---empty---");
            return;
        }
        
        String text = noteText.toString();
        int color = ((BaseActivity) getActivity()).getPrimaryColor();
        if (mKeywordResults == null) {
            mKeywordResults = new LinkedList<>();
        }
        if (mSearchLines == null) {
            mSearchLines = new ArrayList<>();
        }
        
        TextView textView = getShowView();
        Layout textViewLayout = textView.getLayout();
        
        Pattern pattern = Pattern.compile(keyword);
        Matcher matcher = pattern.matcher(text);
        Set<Integer> set = new HashSet<>();
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            SearchResult searchResult = new SearchResult();
            BackgroundColorSpan colorSpan = new BackgroundColorSpan(color);
            spannable.setSpan(colorSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            searchResult.colorSpan = colorSpan;

            // Get the rectangle of the clicked text
            searchResult.line = textViewLayout.getLineForOffset(start);

            set.add(searchResult.line);
            
            mKeywordResults.add(searchResult);
        }
        if (set.size() > 0) {
            mSearchLines.addAll(set);
            Collections.sort(mSearchLines);
        }
    }

    @Override
    public void cancelSearch() {
        CharSequence text = getText();
        if (TextUtils.isEmpty(text)) {
            KLog.d(TAG, "----matchWord--text--is---empty---");
            return;
        }
        setFindScroll(false, true);
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            removeSearchSpan(spannable);
        }
    }

    /**
     * 设置查找的滚动位置
     * @param next 是否是下一个
     * @param outSearch 是否退出搜索模式
     */
    private void setFindScroll(boolean next, boolean outSearch) {
        TextView textView = getShowView();
        if (textView != null) {
            int x = textView.getScrollX();
            boolean scroll = true;
            int line = 0;
            if (outSearch) {
                mNextPosition = 0;
            } else {
                if (mSearchLines != null && mSearchLines.size() > 0) {
                    if (next) {
                        mNextPosition++;
                        if (mNextPosition > mSearchLines.size() - 1) {   //下一个的索引大于总数，则从0开始
                            mNextPosition = 0;
                        }
                    } else {
                        mNextPosition--;
                        if (mNextPosition < 0) {
                            mNextPosition = mSearchLines.size() - 1; //上一个搜索，到最后
                        }
                    }
                    line = mSearchLines.get(mNextPosition);
                } else {
                    scroll = false;
                }
            }
            if (scroll) {
                int y = textView.getLayout().getLineTop(line); // e.g. I want to scroll to line 40
                KLog.d(TAG, "----onFindPrevious:" + mNextPosition + "---line:" + line);
                textView.scrollTo(x, y);
            }
        }
    }

    @Override
    public void onFindPrevious() {
        setFindScroll(false, false);
    }

    @Override
    public void onFindNext() {
        setFindScroll(true, false);
    }

    /**
     * 回撤附件
     * @param editStep
     * @param attach
     * @param callback
     */
    public void resetAttach(final EditStep editStep, final Attach attach, final InsertTextCallback callback) {
        final Editable editable = mEtContent.getEditableText();
        String uri = attach.getAvailableUri();
        int attachType = attach.getType();
        if (uri != null) {
            switch (attachType) {
                case Attach.IMAGE:  //显示图片
                    showSpanImage(editable, null, editStep, uri, callback);
                    break;
                case Attach.PAINT:  //显示绘画
                    ImageSize imageSize = mEtContent.getImageSize(attachType);
                    showSpanImage(editable, imageSize, editStep, uri, callback);
                    break;
                case Attach.VOICE:  //显示语音
                case Attach.ARCHIVE:    //显示压缩包
                case Attach.VIDEO:    //显示视频
                case Attach.FILE:    //显示视频
                    CharSequence text = editStep.getContent();
                    int selStart = editStep.getStart();
                    if (callback != null) {
                        callback.beforeInsertText();
                    }
                    mEtContent.showSpanAttach(text, selStart, attach);
                    if (callback != null) {
                        callback.afterInsertText();
                    }
                    break;
            }
        } else {
            editable.insert(editStep.getStart(), editStep.getContent());
        }
    }

    /**
     * 显示/隐藏软键盘
     */
    public void setSoftInputVisibility(boolean visible) {
        if (visible) {
            SystemUtil.showSoftInput(getContext(), mEtContent);
        } else {
            SystemUtil.hideSoftInput(getContext(), mEtContent);
        }
    }

    /**
     * 自动链接文本
     * @author tiger
     * @update 2016/3/13 10:46
     * @version 1.0.0
     */
    private void autoLink(CharSequence s) {
        mHandler.removeMessages(MSG_AUTO_LINK);
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_AUTO_LINK;
        msg.obj = s;
        mHandler.sendMessage(msg);
    }

    /**
     * 初始化文本显示
     */
    private void initTextView() {
        mContentLayout.setOnItemClickListener(this);
    }

    /**
     * 初始化编辑框
     * @param editText
     */
    public void initEditText(final NoteEditText editText) {
        editText.addTextChangedListener(this);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) { //回车换行
                    mIsEnterLine = true;
                    //光标所在行
                    int selectionStart = v.getSelectionStart();
                    String text = v.getText().toString();
                    //光标所在行的开头到光标处的文本
                    String selectionLineBeforeText = getSelectionLineBeforeText(text, selectionStart);
                    int tagLength = Constants.FORMAT_LIST_TAG_LENGTH;
                    if (Constants.TAG_FORMAT_LIST.equals(selectionLineBeforeText)) {    //清除格式列表
                        String endText = getSelectionLineEndText(text, selectionStart);
                        if (endText.length() == 0) {   //该行只有“- ”，则删除
                            Editable editable = v.getEditableText();
                            int start = selectionStart - tagLength;
                            try {
                                mIsEnterLine = false;
                                editable.delete(start, selectionStart);
                                return true;
                            } catch (Exception e) {
                                KLog.e(e.getMessage());
                            }
                        }
                    }
                }
                return false;
            }
        });
        editText.setOnSelectionChangedListener(new NoteEditText.SelectionChangedListener() {
            @Override
            public void onSelectionChanged(int selStart, int selEnd) {
                CharSequence text = editText.getText();
                ClickableSpan[] links = ((Spannable) text).getSpans(selStart,
                        selEnd, ClickableSpan.class);
                if (links != null && links.length > 0) {
                    ClickableSpan clickableSpan = links[0];
                    KLog.d(TAG, "----onSelectionChanged--clickableSpan---" + clickableSpan.toString());
                } else {
                    AttachSpan[] images = ((Spannable) text).getSpans(selStart,
                            selEnd, AttachSpan.class);
                    if (images != null && images.length > 0) {
                        KLog.d(TAG, "----onSelectionChanged---AttachSpan--");
                    }
                }
            }
        });

//        editText.setMovementMethod(NoteLinkMovementMethod.getInstance());
//        editText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(final View v) {
//                KLog.d(TAG, "----setOnClickListener-----");
//                if (isViewMode()) { //之前是阅读模式
//                    setupEditMode((EditText) v, false);
//                } else {    //编辑模式
//                    changeNoteMode(true);
//                }
//            }
//        });
    }

    /**
     * 重置editText,主要是清除一些监听器
     * @param editText
     */
    public void resetEdittext(final NoteEditText editText) {
        if (editText != null) {
            editText.removeTextChangedListener(this);
            editText.setOnEditorActionListener(null);
            editText.setOnSelectionChangedListener(null);
        }
    }
    
    public void beginBatchEdit() {
        mEtContent.beginBatchEdit();
    }
    
    public void endBatchEdit() {
        mEtContent.endBatchEdit();
    }
    
    public void addAttach(Attach attach, AttachAddCompleteListener listener) {
        mEtContent.addAttach(attach, listener);
    }
    
    public void addAttach(List<Attach> attachs, AttachAddCompleteListener listener) {
        for (Attach attach : attachs) {
            addAttach(attach, listener);
        }
    }

    /**
     * 在格式化列表直接切换
     * @author huanghui1
     * @update 2016/3/10 14:36
     * @version: 1.0.0
     */
    public void toggleFormatList() {
        String text = mEtContent.getText().toString();
        Editable editable = mEtContent.getEditableText();
        //光标的开始位置
        int selectionStart = mEtContent.getSelectionStart();
        //光标所在位置前面的文字
        String beforeText = text.substring(0, selectionStart);
        //光标所在行的第一位
        int lineStart = beforeText.lastIndexOf(Constants.TAG_NEXT_LINE) + 1;
        //光标后面的文字
        String endText = text.substring(selectionStart);
        //光标后面文字的第一个回车的索引
        int lineEnd = endText.indexOf(Constants.TAG_NEXT_LINE);
        //光标所在行的文字
        String lineText = null;
        if (lineEnd != -1) {    //光标后面的文字有回车换行
            String linEndText = endText.substring(0, lineEnd);
            String lineBeforeText = beforeText.substring(lineStart);
            lineText = lineBeforeText + linEndText;
        } else {
            lineText = text.substring(lineStart);
        }
        if (lineText.startsWith(Constants.TAG_FORMAT_LIST)) {  //之前有“- ”,则删除
            int end = lineStart + Constants.FORMAT_LIST_TAG_LENGTH;
            editable.delete(lineStart, end);
        } else {
            editable.insert(lineStart, Constants.TAG_FORMAT_LIST);
        }
    }

    /**
     * 清除文本内容
     */
    public void clearContent() {
        mEtContent.setText("");
    }
    
    /**
     * 处理图片的更新，主要是绘画
     * @param attachSpec
     * @param listener
     */
    public void handleUpdateImage(AttachSpec attachSpec, AttachAddCompleteListener listener) {
        mRichTextWrapper.getRichSpan().showImage(attachSpec, listener);
    }

    /**
     * 获取光标所在行的开头到光标处的文本
     * @author Administrator
     * @update 2016/3/12 14:30
     * @version: 1.0.0
     */
    private String getSelectionLineBeforeText(String text, int selectionStart) {
        String beforeText = text.substring(0, selectionStart);
        int lineStart = beforeText.lastIndexOf(Constants.TAG_NEXT_LINE) + 1;
        return beforeText.substring(lineStart);
    }

    /**
     * 获取光标所在行的光标处到该行结尾的文本
     * @author Administrator
     * @update 2016/3/12 22:29
     * @version: 1.0.0
     */
    private String getSelectionLineEndText(String text, int selectionStart) {
        String endText = text.substring(selectionStart);
        int lineEnd = endText.indexOf(Constants.TAG_NEXT_LINE);
        if (lineEnd != -1) {    //有回车换行，则截取
            endText = endText.substring(0, lineEnd);
        }
        return endText;
    }

    @Override
    public void onAttach(Context context) {
        KLog.d(TAG, "--NoteEditFragment---onAttach----");
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        KLog.d(TAG, "--NoteEditFragment---onDetach----");
        mListener = null;
        resetEdittext(mEtContent);
        
        super.onDetach();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mListener != null) {
            mListener.beforeNoteTextChanged(s, start, count, after);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mListener != null) {
            if (s != null) {
                autoLink(s);
            }
            mListener.onNoteTextChanged(s, start, before, count);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mListener != null) {
            mText = s;
            KLog.d(TAG, "--afterTextChanged-----" + s);
            if (mIsEnterLine) {  //手动按的回车键
                mIsEnterLine = false;
                int selectionStart = mEtContent.getSelectionStart();

                //回车前的光标索引
                int previousIndex = selectionStart - 1;
                String lineBeforeText = getSelectionLineBeforeText(s.toString(), previousIndex);
                if (lineBeforeText.startsWith(Constants.TAG_FORMAT_LIST)) {   //上一行有“- ”，则本行继续添加
                    s.insert(selectionStart, Constants.TAG_FORMAT_LIST);
                }
            }
            mListener.afterNoteTextChanged(s);
        }
    }

    @Override
    public void onClick(View v) {
        
    }

    /**
     * 自动识别链接的任务
     * @author tiger
     * @update 2016/3/13 10:41
     * @version 1.0.0
     */
    class AutoLinkTask implements Runnable {
        private CharSequence s;

        public AutoLinkTask(CharSequence s) {
            this.s = s;
        }

        @Override
        public void run() {
            KLog.d(TAG, "-----AutoLinkTask---run---");
            //判断附件
            TextView textView = (TextView) mRichTextWrapper.getRichSpan();
            NoteLinkify.addLinks(textView, textView.getAutoLinkMask(), MessageBundleSpan.class);
            MovementMethod movement = textView.getMovementMethod();
            if (mContentLayout.isEditMode() && (movement == null || movement instanceof LinkMovementMethod)) {

                //移除链接的点击事件
                mRichTextWrapper.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            }
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<NoteEditFragment> mTarget;

        public MyHandler(NoteEditFragment target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            NoteEditFragment fragment = mTarget.get();
            switch (msg.what) {
                case MSG_AUTO_LINK: //自动链接
                    if (fragment.mAutoLinkTask == null) {
                        CharSequence s = (CharSequence) msg.obj;
                        fragment.mAutoLinkTask = fragment.new AutoLinkTask(s);
                    }
                    post(fragment.mAutoLinkTask);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {

        void beforeNoteTextChanged(CharSequence s, int start, int count, int after);

        void onNoteTextChanged(CharSequence s, int start, int before, int count);

        void afterNoteTextChanged(Editable s);

        /**
         * 初始化完毕
         */
        void onInitCompleted(boolean isViewMode);
    }

    /**
     * 添加文本的监听器
     */
    public interface InsertTextCallback {

        void beforeInsertText();

        void afterInsertText();
    }

    /**
     * 搜索的结果
     */
    class SearchResult {
        /**
         * 关键字突出显示
         */
        BackgroundColorSpan colorSpan;
        /**
         * 改关键字所在的行
         */
        int line;
    }
}