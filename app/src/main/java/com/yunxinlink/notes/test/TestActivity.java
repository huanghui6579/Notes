package com.yunxinlink.notes.test;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.ui.LockDigitalActivity;
import com.yunxinlink.notes.lock.ui.LockPatternActivity;
import com.yunxinlink.notes.lockpattern.utils.AlpSettings;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.log.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

public class TestActivity extends BaseActivity implements View.OnClickListener {
    // This is your preferred flag
    private static final int REQ_CREATE_PATTERN = 1;
    private static final int REQ_VERIFY_PATTERN = 2;
    private static final int REQ_VERIFY_CAPTCHA = 3;
    private static final int REQ_CREATE_DIGITAL = 4;
    private static final int REQ_VERIFY_DIGITAL = 5;
    
    private TextView tvDeviceInfo;

    @Override
    protected int getContentView() {
        return R.layout.activity_test;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

        /*SearchView searchView = (SearchView) findViewById(R.id.search_view);

        if (searchView != null) {
//            searchView.setIconifiedByDefault(true);
            searchView.setIconified(false);
        }*/

        /*final TextView textView = (TextView) findViewById(R.id.tv_content);
        final ImageView imageView = (ImageView) findViewById(R.id.iv_img);

        String filePath = "/sdcard/images/7dd98d1001e939011b6cf83f79ec54e736d19640.jpg";
        ImageUtil.generateThumbImageAsync(filePath, null, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (loadedImage != null) {
                    Drawable drawable = ImageUtil.bitmap2Drawable(mContext, loadedImage);
                    textView.setBackgroundDrawable(drawable);

                    imageView.setImageDrawable(drawable);
                }
            }
        });*/

        final TextView textView = (TextView) findViewById(R.id.tv_content);

        AlpSettings.Security.setAutoSavePattern(mContext, true);
        Button btnLock = (Button) findViewById(R.id.btn_lock);
        if (btnLock != null) {
            btnLock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LockPatternActivity.IntentBuilder
                            .newPatternCreator(mContext)
                            .startForResult(TestActivity.this, REQ_CREATE_PATTERN);
                }
            });
        }
        Button btnCompare = (Button) findViewById(R.id.btn_compare);
        if (btnCompare != null) {
            btnCompare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LockPatternActivity.IntentBuilder
                            .newPatternComparator(mContext)
                            .startForResult(TestActivity.this, REQ_VERIFY_PATTERN);
                }
            });
        }


        Button btnDigitalLock = (Button) findViewById(R.id.btn_digital_lock);
        if (btnDigitalLock != null) {
            btnDigitalLock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LockDigitalActivity.IntentBuilder
                            .newPatternCreator(mContext)
                            .startForResult(TestActivity.this, REQ_CREATE_DIGITAL);
                }
            });
        }
        Button btnDigitalCompare = (Button) findViewById(R.id.btn_digital_compare);
        if (btnDigitalCompare != null) {
            btnDigitalCompare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LockDigitalActivity.IntentBuilder
                            .newPatternComparator(mContext)
                            .startForResult(TestActivity.this, REQ_VERIFY_DIGITAL);
                }
            });
        }
        
        Button btnDeviceInfo = (Button) findViewById(R.id.btn_device_info);
        tvDeviceInfo = (TextView) findViewById(R.id.tv_device_info);
        if (btnDeviceInfo != null) {
            btnDeviceInfo.setOnClickListener(this);
        }

        /*
        Button btnKeyStore = (Button) findViewById(R.id.btn_keyStore);
        if (btnKeyStore != null) {
            btnKeyStore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    *//*
                 * 加密用的Key 可以用26个字母和数字组成，最好不要用保留字符，虽然不会错，至于怎么裁决，个人看情况而定
                 * 此处使用AES-128-CBC加密模式，key需要为16位。
                 *//*
                    String password = "1234567890123456";
                    String content = "有道词典手机版. 全新云图书，一站式的英语学习平台. iPhone立即下载 · App Store · PP助手一键 ... 单词本. 支持与桌面词典的复习计划互相同步，随时随地背单词。";
                    String encoderText = EncryptionUtil.AESEncrypt(password, content);
                    KLog.d("加密之前：" + content);
                    KLog.d("encoderText: " + encoderText);

                    String decoderText = EncryptionUtil.AESDecrypt(password, encoderText);
                    KLog.d("解密后的数据：" + decoderText);
//                    KeyStore keyStore = getKeyStore();
////                    refreshKeys(keyStore);
//
//                    String alias = "NoteAlias";
//
//                    createNewKeys(keyStore, alias);
//
//                    String text = "使用Android自身";
//                    KLog.d("加密的前的内容：" + text);
//                    String encryptString = encryptString(keyStore, alias, text);
//                    KLog.d("加密的后的内容：" + encryptString);
//
//                    String decryptString = decryptString(keyStore, alias, encryptString);
//                    KLog.d("解密的后的内容：" + decryptString);
                }
            });
        }*/

        /*ImageView imageView = (ImageView) findViewById(R.id.icon);

        Drawable drawable = getResources().getDrawable(R.drawable.ic_action_trash);
        
        int color = getResources().getColor(R.color.colorPrimary);

        EditText textView = (EditText) findViewById(R.id.tv_test);

        textView.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG); //中划线
        
        getTintDrawable(drawable, color);

        imageView.setImageDrawable(drawable);



        int disableColor = SystemUtil.adjustAlpha(Color.WHITE, Constants.MENU_ITEM_COLOR_ALPHA);
        ImageButton btnDo = (ImageButton) findViewById(R.id.btn_do);
        
        int resId = R.drawable.ic_action_undo;
        
        tintDoMenuIcon(btnDo, resId, disableColor);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQ_CREATE_PATTERN:
                    switch (resultCode) {
                        case RESULT_OK:
                            final char[] pattern = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN);
                            String text = new String(pattern);
                            KLog.d("text:" + text);
                            break;
                    }
                    break;
                case REQ_VERIFY_PATTERN:
                    switch (resultCode) {
                        case RESULT_OK:
                            int tryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 1);
                            KLog.d("tryCount:" + tryCount);
                            break;
                    }
                    break;
                case REQ_CREATE_DIGITAL:
                    switch (resultCode) {
                        case RESULT_OK:
                            final String pattern = data.getStringExtra(LockDigitalActivity.EXTRA_PATTERN);
                            KLog.d("digital text:" + pattern);
                            break;
                    }
                    break;
                case REQ_VERIFY_DIGITAL:
                    switch (resultCode) {
                        case RESULT_OK:
                            int tryCount = data.getIntExtra(LockDigitalActivity.EXTRA_RETRY_COUNT, 1);
                            KLog.d("tryCount digital:" + tryCount);
                            break;
                    }
                    break;
            }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void tintDoMenuIcon(ImageButton imageButton, int resId, int disableColor) {

        Drawable normalDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme());

        Drawable spinnerInkDrawable = getResources().getDrawable(R.drawable.ic_action_spinner_ink);

        int[] colors = new int[] {Color.RED, Color.BLACK};

        Drawable srcDrawable = getStateListDrawable(normalDrawable, colors);

        int[] bgColors = new int[] {Color.WHITE, Color.TRANSPARENT};
        Drawable bgDrawable = getStateListDrawable(spinnerInkDrawable, bgColors);
        

        //合成的选中后的图标
        Drawable[] drawables = new Drawable[2];
        drawables[0] = srcDrawable;
        drawables[1] = bgDrawable;
        LayerDrawable layerDrawable = new LayerDrawable(drawables);

//        Drawable normalDrawable = ResourcesCompat.getDrawable(getResources(), resId, getTheme());
//        getTintDrawable(normalDrawable, Color.WHITE);
//
//        Drawable disableDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_action_brush, getTheme());
//        getTintDrawable(disableDrawable, Color.RED);
//
//        listDrawable.addState(new int[] {android.R.attr.state_pressed}, disableDrawable);
//        listDrawable.addState(new int[] {}, normalDrawable);

        /*Drawable currentDrawable = getStateDrawable(listDrawable, StateSet.NOTHING);

        getTintDrawable(currentDrawable, Color.WHITE);

        Drawable pressedDrawable = getStateDrawable(listDrawable, new int[] {android.R.attr.state_pressed});
        if (pressedDrawable != null) {
            getTintDrawable(pressedDrawable, Color.RED);
        }*/

        imageButton.setImageDrawable(layerDrawable);
    }
    
    private Drawable getStateDrawable(StateListDrawable listDrawable, int[] state) {
        //给默认的图标着色
        Drawable drawable = null;
        try {
            Method method = StateListDrawable.class.getDeclaredMethod("getStateDrawableIndex", int[].class);
            if (method != null) {
                method.setAccessible(true);
                int index = (int) method.invoke(listDrawable, state);
                method = StateListDrawable.class.getDeclaredMethod("getStateDrawable", int.class);
                if (method != null) {
                    method.setAccessible(true);

                    drawable = (Drawable) method.invoke(listDrawable, index);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "---tintNormalDrawable----error---" + e.getMessage());
            e.printStackTrace();
        }
        return drawable;
    }
    
    protected Drawable getStateListDrawable(Drawable drawable, int[] colors) {
        int[][] states = new int[2][];


        states[0] = new int[] { android.R.attr.state_pressed};

        states[1] = new int[] {};


        ColorStateList colorList = new ColorStateList(states, colors);

        StateListDrawable stateListDrawable = new StateListDrawable();



        stateListDrawable.addState(states[0], drawable);//注意顺序

        stateListDrawable.addState(states[1], drawable);

        Drawable.ConstantState state = stateListDrawable.getConstantState();

//        normalDrawable = DrawableCompat.wrap(state == null ? stateListDrawable : drawable).mutate();
        Drawable srcDrawable = DrawableCompat.wrap(state == null ? stateListDrawable : state.newDrawable()).mutate();

        DrawableCompat.setTintList(srcDrawable, colorList);
        return srcDrawable;
    }

    public static KeyStore getKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            return keyStore;
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> refreshKeys(KeyStore keyStore) {
        List<String> keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                keyAliases.add(aliases.nextElement());
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        KLog.d(TAG, "keyAliases:" + keyAliases);
        return keyAliases;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void createNewKeys(KeyStore keyStore, String alias) {
        // Create new key if needed
        try {
            if (!keyStore.containsAlias(alias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT);
                        KeyGenParameterSpec spec = builder.setCertificateSubject(new X500Principal("CN=Sample Name, O=Android Authority"))
                                .setCertificateNotAfter(end.getTime())
                                .setCertificateNotBefore(start.getTime())
                                .setCertificateSerialNumber(BigInteger.ONE)
                                .setKeyValidityEnd(end.getTime())
                                .setKeyValidityStart(start.getTime()).build();

                        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                        generator.initialize(spec);
                        KeyPair keyPair = generator.generateKeyPair();
                        PrivateKey privateKey = keyPair.getPrivate();
                        KLog.d("privateKey:" + privateKey + " publicKey:" + keyPair.getPublic());
                    } else {
                        KeyPairGeneratorSpec.Builder builder = new KeyPairGeneratorSpec.Builder(mContext);
                        builder.setAlias(alias)
                                .setSubject(new X500Principal("CN=Sample Name, O=Android Authority"))
                                .setSerialNumber(BigInteger.ONE)
                                .setStartDate(start.getTime())
                                .setEndDate(end.getTime());

                        KeyPairGeneratorSpec spec = builder.build();
                        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                        generator.initialize(spec);
                        KeyPair keyPair = generator.generateKeyPair();
                        KLog.d("privateKey:" + keyPair.getPrivate() + " publicKey:" + keyPair.getPublic());
                    }
                }
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        refreshKeys(keyStore);
    }

    /**
     * 加密一个文本块由密钥对的公钥执行。我们检索公钥，请求一个密码，使用我们更喜欢的加密或解密转换（“RSA/ECB/PKCS1Padding”），
     * 然后初始化密码，使用检索到的公钥来执行加密（Cipher.ENCRYPT_MODE）。密码操作（和返回）一个字节 []。
     * 我们将密码包含在 CipherOutputStream 中，和 ByteArrayOutputStream 一起来处理加密复杂性。加密进程的结果就是转化成一个显示为 Base64 的字符串
     * @param keyStore
     * @param alias
     * @param text
     * @return
     */
    private String encryptString(KeyStore keyStore, String alias, String text) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);

            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            input.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, input);
            cipherOutputStream.write(text.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] vals = outputStream.toByteArray();
            return new String(vals);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String decryptString(KeyStore keyStore, String alias, String text) {
        try {
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);

            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            output.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(text.getBytes()), output);

            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }
            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }
            return new String(bytes, 0, bytes.length, "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_device_info:
//                getRingerMode(mContext);
                String info = getDeviceInfo(mContext);
                tvDeviceInfo.setText(info);
                break;
        }
    }
    
    private String getDeviceInfo(Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append("deviceId:" + getDeviceId(context)).append("\n")
                .append("Android Version：" + getBuildVersion()).append("\n")
                .append("Phone Model:" + getPhoneModel()).append("\n")
                .append("Phone Brand:" + getPhoneBrand()).append("\n")
                .append("Phone Product:" + getPhoneProduct()).append("\n")
                .append("Phone Board:" + getPhoneBoard()).append("\n")
                .append("Phone Device:" + Build.DEVICE).append("\n")
                .append("Phone Display:" + Build.DISPLAY).append("\n")
                .append("Phone Type:" + Build.TYPE).append("\n");
                
        return builder.toString();
    }

    /**
     * 获取手机Android 版本（4.4、5.0、5.1 ...）
     *
     * @return
     */
    public static String getBuildVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String getPhoneModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String getPhoneBoard() {
        return android.os.Build.BOARD;
    }

    /**
     * 获取手机品牌
     *
     * @return
     */
    public static String getPhoneBrand() {
        return android.os.Build.BRAND;
    }

    //android 获取当前手机品牌  
    public static String getPhoneProduct() {
        return  android.os.Build.PRODUCT;
    }

    /**
     * 获取设备的唯一标识，deviceId
     *
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();
        if (deviceId == null) {
            return "";
        } else {
            return deviceId;
        }
    }
    
    public void getRingerMode(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        //获取情景模式
        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:   //静默模式
                Log.e("RingerMode", "RINGER_MODE_SILENT");
                break;
            case AudioManager.RINGER_MODE_VIBRATE:  //振动模式
                Log.e("RingerMode", "RINGER_MODE_VIBRATE");
                break;
            case AudioManager.RINGER_MODE_NORMAL:   //铃声模式
                Log.e("RingerMode", "RINGER_MODE_NORMAL");
                break;
            default:
                break;
        }

        //获取震动模式 分为通知和铃音两种 VIBRATE_TYPE_NOTIFICATION / VIBRATE_TYPE_RINGER
        switch (audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            case AudioManager.VIBRATE_SETTING_ON:
                Log.e("getVibrateSetting NOTIFICATION", "VIBRATE_SETTING_ON");
                break;
            case AudioManager.VIBRATE_SETTING_OFF:
                Log.e("getVibrateSetting NOTIFICATION", "VIBRATE_SETTING_OFF");
                break;
            case AudioManager.VIBRATE_SETTING_ONLY_SILENT:
                Log.e("getVibrateSetting NOTIFICATION", "VIBRATE_SETTING_ONLY_SILENT");
                break;
            default:
                break;
        }

        switch (audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER)) {
            case AudioManager.VIBRATE_SETTING_ON:
                Log.e("getVibrateSetting RINGER", "VIBRATE_SETTING_ON");
                break;
            case AudioManager.VIBRATE_SETTING_OFF:
                Log.e("getVibrateSetting RINGER", "VIBRATE_SETTING_OFF");
                break;
            case AudioManager.VIBRATE_SETTING_ONLY_SILENT:
                Log.e("getVibrateSetting RINGER", "VIBRATE_SETTING_ONLY_SILENT");
                break;
            default:
                break;
        }
    }
}
