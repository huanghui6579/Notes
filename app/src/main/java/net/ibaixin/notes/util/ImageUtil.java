package net.ibaixin.notes.util;

import android.graphics.Bitmap;
import android.net.Uri;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

import net.ibaixin.notes.util.log.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

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
     * @param uri
     * @param imageSize
     */
    public static Bitmap loadImageThumbnailsSync(String uri, ImageSize imageSize) {
        ImageLoader imageLoader = ImageLoader.getInstance();
        return imageLoader.loadImageSync(uri, imageSize, getAlbumImageOptions());
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
        if (imagePath != null) {
            Bitmap bitmap = loadImageThumbnailsSync(ImageDownloader.Scheme.FILE.wrap(imagePath), imageSize);
            if (bitmap != null) {
                try {
                    return compressImage(bitmap, savePath);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * 异步生成缩略图
     * @param imagePath 原始图片的文件的路径
     * @param imageSize 要生存缩略图的尺寸大小
     * @param loadingListener 生存缩略图的监听
     * @update 2015年7月27日 下午4:35:46
     */
    public static void generateThumbImageAsync(String imagePath, ImageSize imageSize, ImageLoadingListener loadingListener) {
        String iconUri = ImageDownloader.Scheme.FILE.wrap(imagePath);
        ImageLoader imageLoader = ImageLoader.getInstance();
        MemoryCacheUtils.removeFromCache(iconUri, imageLoader.getMemoryCache());
        imageLoader.loadImage(iconUri, imageSize, getAlbumImageOptions(), loadingListener);
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
        return compressImage(bitmap, savePath, 10);
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
}
