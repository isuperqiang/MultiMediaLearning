package com.richie.multimedialearning.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Richie on 2018.10.16
 */
public final class FileUtils {
    private static final String TAG = "FileUtils";

    private FileUtils() {
    }

    /**
     * 将 assets 里面的所有文件拷贝到应用外部存储目录下，保留文件树结构，不会覆盖原有数据
     *
     * @param context
     */
    public static void copyAssets2FileDir(Context context) {
        AssetManager assets = context.getAssets();
        List<String> fileTree = new ArrayList<>(16);
        listAssetsRecursively(assets, "", fileTree);
        File externalAssetsDir = getExternalAssetsDir(context);
        for (String s : fileTree) {
            try {
                InputStream is = assets.open(s);
                File dest = new File(externalAssetsDir, s);
                if (dest.exists()) {
                    continue;
                }
                copyFile(is, dest);
            } catch (IOException e) {
                Log.e(TAG, "copyAssets2FileDir: " + s, e);
            }
        }
    }

    private static void listAssetsRecursively(AssetManager assetManager, String dirName, List<String> fileTree) {
        try {
            String[] list = assetManager.list(dirName);
            if (list != null && list.length > 0) {
                // is dir
                for (String s : list) {
                    String dir = TextUtils.isEmpty(dirName) ? s : dirName + File.separator + s;
                    listAssetsRecursively(assetManager, dir, fileTree);
                }
            } else {
                fileTree.add(dirName);
            }
        } catch (IOException e) {
            Log.e(TAG, "listAssetsRecursively: " + dirName, e);
        }
    }

    public static void copyFile(InputStream is, File dest) throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            File parentFile = dest.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            if (dest.exists()) {
                dest.delete();
            }
            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] bytes = new byte[8192];
            int len;
            while ((len = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, len);
            }
            bos.flush();
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
    }

    public static File getExternalAssetsDir(Context context) {
        File fileDir = getFileDir(context);
        File assetsDir = new File(fileDir, "assets");
        if (!assetsDir.exists()) {
            assetsDir.mkdirs();
        }
        return assetsDir;
    }

    public static String getWavFilePath(Context context, String name) {
        File wavFileDir = getWavFileDir(context);
        File wavFile = new File(wavFileDir, name + ".wav");
        return wavFile.getAbsolutePath();
    }

    public static File getWavFileDir(Context context) {
        File fileDir = getFileDir(context);
        File wavFileDir = new File(fileDir, "wav");
        if (!wavFileDir.exists()) {
            wavFileDir.mkdirs();
        }
        return wavFileDir;
    }

    public static File getAacFileDir(Context context) {
        File fileDir = getFileDir(context);
        File aacFileDir = new File(fileDir, "aac");
        if (!aacFileDir.exists()) {
            aacFileDir.mkdirs();
        }
        return aacFileDir;
    }

    public static File getPcmFileDir(Context context) {
        File fileDir = getFileDir(context);
        File pcmFileDir = new File(fileDir, "pcm");
        if (!pcmFileDir.exists()) {
            pcmFileDir.mkdirs();
        }
        return pcmFileDir;
    }

    public static File getPhotoFileDir(Context context) {
        File fileDir = getFileDir(context);
        File photoFileDir = new File(fileDir, "photo");
        if (!photoFileDir.exists()) {
            photoFileDir.mkdirs();
        }
        return photoFileDir;
    }

    public static String getPcmFilePath(Context context, String name) {
        File pcmFileDir = getPcmFileDir(context);
        File pcmFile = new File(pcmFileDir, name + ".pcm");
        return pcmFile.getAbsolutePath();
    }

    public static File getFileDir(Context context) {
        File filesDir = context.getExternalFilesDir(null);
        if (filesDir == null) {
            filesDir = context.getFilesDir();
        }
        return filesDir;
    }

    /**
     * 获取 UUID 作为唯一标识 eg: c9ce6bdd155749be91153a6d76a484eb
     *
     * @return 32 个字符
     */
    public static String getUUID32() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase();
    }

    public static void deleteFileRecursively(File file) {
        if (file.isFile()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteFileRecursively(f);
                }
            }
        }
    }

    public static String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        if (columnIndex > -1) {
                            path = cursor.getString(columnIndex);
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
