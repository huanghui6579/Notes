package com.yunxinlink.notes.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import com.yunxinlink.notes.util.log.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 图片的工具类
 * @author huanghui1
 * @update 2016/7/5 20:03
 * @version: 0.0.1
 */
public class ImageUtil {
    private static final String TAG = "ImageUtil";
    
    private ImageUtil() {}

    /**
     * 获取图片的缩略图
     * @param imagePath 原始图片的路径，含完整文件名
     * @param savePath 存储缩略图的路径，包含文件名
     * @return 缩略图
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static boolean generateThumbImage(String imagePath, String savePath) {
        return generateThumbImage(imagePath, null/*new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT)*/, savePath);
    }

    /**
     * 获取图片的缩略图
     * @param imagePath 原始图片的路径，含完整文件名
     * @param savePath 存储缩略图的路径，包含文件名
     * @param compress 是否需要进行质量压缩                
     * @return 缩略图
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static boolean generateThumbImage(String imagePath, String savePath, boolean compress) {
        return generateThumbImage(imagePath, null/*new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT)*/, savePath, compress);
    }

    /**
     * 获取图片的缩略图
     * @param noteId 笔记的ID
     * @param filename 文件的名称，不含有目录
     * @return 缩略图的存储完整路径，含文件名
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static String generateNoteThumbImage(String imagePath, String noteId, String filename) {
        String savePath = SystemUtil.generateNoteThumbAttachFilePath(noteId, filename);
        boolean success = generateThumbImage(imagePath, null/*new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_HEIGHT)*/, savePath);
        if (success) {
            return savePath;
        } else {
            return null;
        }
    }

    /**
     * 同步加载图片的缩略图
     * @author tiger
     * @update 2015年3月7日 下午5:53:46
     * @param uri
     */
    public static Bitmap loadImageThumbnailsSync(String uri) {
        return loadImageThumbnailsSync(uri, null);
    }

    /**
     * 同步加载图片的缩略图
     * @author tiger
     * @update 2015年3月7日 下午5:53:46
     * @param imagePath 
     * @param imageSize
     */
    public static Bitmap loadImageThumbnailsSync(String imagePath, ImageSize imageSize) {
        ImageLoader imageLoader = ImageLoader.getInstance();
        String iconUri = null;
        ImageDownloader.Scheme scheme = ImageDownloader.Scheme.ofUri(imagePath);
        if (ImageDownloader.Scheme.UNKNOWN == scheme) { //没有前缀，则添加file://
            iconUri = ImageDownloader.Scheme.FILE.wrap(imagePath);
        } else {
            iconUri = imagePath;
        }
        return imageLoader.loadImageSync(iconUri, imageSize, getAlbumImageOptions());
    }

    /**
     * 获取图片的缩略图
     * @param imagePath 原始图片的路径，含完整文件名
     * @param imageSize 指定的缩略图的参考尺寸
     * @param savePath 存储缩略图的路径，包含文件名
     * @return 缩略图
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static boolean generateThumbImage(String imagePath, ImageSize imageSize, String savePath) {
        return generateThumbImage(imagePath, imageSize, savePath, true);
    }

    /**
     * 获取图片的缩略图
     * @param imagePath 原始图片的路径，含完整文件名
     * @param imageSize 指定的缩略图的参考尺寸
     * @param savePath 存储缩略图的路径，包含文件名
     * @param compress 是否需要进行质量压缩                     
     * @return 缩略图
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月3日 下午1:51:01
     */
    public static boolean generateThumbImage(String imagePath, ImageSize imageSize, String savePath, boolean compress) {
        if (imagePath != null) {
            Bitmap bitmap = loadImageThumbnailsSync(ImageDownloader.Scheme.FILE.wrap(imagePath), imageSize);
            if (bitmap != null) {
                try {
                    return compressImage(bitmap, savePath, compress);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * 将图片的全路径包装成uri的字符串，格式为:file://..
     * @param filePath
     * @return
     */
    private static String wrapImageUri(String filePath) {
        String iconUri = null;
        ImageDownloader.Scheme scheme = ImageDownloader.Scheme.ofUri(filePath);
        if (ImageDownloader.Scheme.UNKNOWN == scheme) { //没有前缀，则添加file://
            iconUri = ImageDownloader.Scheme.FILE.wrap(filePath);
        } else {
            iconUri = filePath;
        }
        return iconUri;
    }

    /**
     * 异步生成缩略图
     * @param imagePath 原始图片的文件的路径
     * @param imageSize 要生存缩略图的尺寸大小
     * @param loadingListener 生存缩略图的监听
     * @update 2015年7月27日 下午4:35:46
     */
    public static void generateThumbImageAsync(String imagePath, ImageSize imageSize, ImageLoadingListener loadingListener) {
        String iconUri = wrapImageUri(imagePath);
        ImageLoader imageLoader = ImageLoader.getInstance();
//        MemoryCacheUtils.removeFromCache(iconUri, imageLoader.getMemoryCache());
        imageLoader.loadImage(iconUri, imageSize, getAlbumImageOptions(), loadingListener);
    }

    /**
     * 显示图片
     * @param imagePath
     * @param imageAware 
     * @param loadingListener
     */
    public static void displayImage(String imagePath, ImageAware imageAware, ImageLoadingListener loadingListener) {
        String iconUri = wrapImageUri(imagePath);
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(iconUri, imageAware, getAlbumImageOptions(), loadingListener);
    }

    /**
     * 异步生成缩略图
     * @param uri 原始图片的文件的路径
     * @param imageSize 要生存缩略图的尺寸大小
     * @param loadingListener 生存缩略图的监听
     * @update 2015年7月27日 下午4:35:46
     */
    public static void generateThumbImageAsync(Uri uri, ImageSize imageSize, ImageLoadingListener loadingListener) {
        String iconUri = uri.toString();
        ImageLoader imageLoader = ImageLoader.getInstance();
//        MemoryCacheUtils.removeFromCache(iconUri, imageLoader.getMemoryCache());
        imageLoader.loadImage(iconUri, imageSize, getAlbumImageOptions(), loadingListener);
    }

    /**
     * 按质量压缩图片,默认10%
     * @param bitmap 要压缩的图片
     * @param savePath 要保存压缩后的图片的全路径，包含文件名
     * @return 是否压缩成功
     * @update 2015年8月21日 上午10:01:23
     */
    public static boolean compressImage(Bitmap bitmap, String savePath) {
        return compressImage(bitmap, savePath, false);
    }

    /**
     * 按质量压缩图片,默认10%
     * @param bitmap 要压缩的图片
     * @param savePath 要保存压缩后的图片的全路径，包含文件名
     * @param compress 是否需要进行质量压缩                 
     * @return 是否压缩成功
     * @update 2015年8月21日 上午10:01:23
     */
    public static boolean compressImage(Bitmap bitmap, String savePath, boolean compress) {
        int quality = 100;
        if (compress) {
            quality = 10;
        }
        return compressImage(bitmap, savePath, quality);
    }

    /**
     * 按质量压缩图片
     * @param bitmap 要压缩的图片
     * @param savePath 要保存压缩后的图片的全路径，包含文件名
     * @param quality 要压缩的质量
     * @return 是否压缩成功
     * @update 2015年8月21日 上午10:01:23
     */
    public static boolean compressImage(Bitmap bitmap, String savePath, int quality) {
        try {
            if (bitmap != null) {
                FileOutputStream fos = new FileOutputStream(savePath);
                return bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            }
        } catch (FileNotFoundException e) {
            Log.e(e.getMessage());
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        return false;
    }

    /**
     * 获取笔记编辑中默认的图片尺寸，默认是100x100
     * @return
     */
    public static ImageSize getNoteImageSize() {
        return new ImageSize(Constants.IMAGE_THUMB_WIDTH, Constants.IMAGE_THUMB_WIDTH);
    }
    
    /**
     * 获得相册的图片加载选项该选项没有磁盘缓存图片
     * @update 2014年11月8日 上午11:43:13
     * @return
     */
    public static DisplayImageOptions getAlbumImageOptions() {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
//                .showImageOnLoading(R.drawable.ic_image_default)
//                .showImageForEmptyUri(R.drawable.ic_image_default)
//                .showImageOnFail(R.drawable.ic_image_default)
                .cacheInMemory(true)
                .cacheOnDisk(false)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
//			.displayer(new FadeInBitmapDisplayer(100))
                .build();
        return options;
    }

    /**
     * 保存bitmap到本地文件
     * @param bitmap 图片对象
     * @param saveFile 本地的文件
     * @return
     * @throws IOException
     */
    public static boolean saveBitmap(Bitmap bitmap, File saveFile) throws IOException {
        return saveBitmap(bitmap, saveFile, Bitmap.CompressFormat.JPEG);
    }

    /**
     * 保存bitmap到本地文件
     * @param bitmap 图片对象
     * @param saveFile 本地的文件
     * @param format 图片的格式                
     * @return
     * @throws IOException
     */
    public static boolean saveBitmap(Bitmap bitmap, File saveFile, Bitmap.CompressFormat format) throws IOException {
        if(bitmap == null || saveFile == null) {
            return false;
        }
        try {
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(saveFile));
            bitmap.compress(format, 100, os);
            os.flush();
            os.close();
        } finally {
            bitmap.recycle();
        }
        return true;
    }

    /**
     * 计算图片的缩放值
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    /**
     * 清除图片的内存缓存
     * @param imagePath 图片的本地路径
     */
    public static void clearMemoryCache(String imagePath) {
        if (!TextUtils.isEmpty(imagePath)) {
            String fileUri = ImageDownloader.Scheme.FILE.wrap(imagePath);
            MemoryCacheUtils.removeFromCache(fileUri, ImageLoader.getInstance().getMemoryCache());
        }
    }

    /**
     * 将bitmap转换成drawable
     * @param context
     * @param bitmap
     * @return
     */
    public static Drawable bitmap2Drawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /**
     * 将drawable转换成bitmap
     * @param drawable
     * @return
     */
    public static Bitmap drawable2Bitmap(Drawable drawable) {
        return ((BitmapDrawable) drawable).getBitmap();
    }
}
