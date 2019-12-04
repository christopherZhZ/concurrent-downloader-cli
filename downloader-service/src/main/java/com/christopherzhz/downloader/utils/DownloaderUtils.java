package com.christopherzhz.downloader.utils;

import static com.christopherzhz.downloader.utils.Constant.*;

public class DownloaderUtils {

//    public static String getTempStorageDirectory() {
//        String baseDir = Paths.get("").toAbsolutePath().toString();
//        return baseDir + "/temp";
//    }

    public static String genRangeString(long start, long end) {
        return String.format("bytes=%d-%d", start, end);
    }

    public static String genRangeString(long start) {
        return String.format("bytes=%d-", start);
    }

    public static String getFileNameByUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    public static String genFileSizeString(long bytes) {
        if (bytes < 1024) return bytes + " B";
        float size = (float) bytes;
        int u = -1;
        do {
            size /= SIZE_THRESH;
            ++u;
        } while(size >= SIZE_THRESH && u < SIZE_UNITS.length-1);
        return String.format("%.2f %s", size, SIZE_UNITS[u]);
    }

    public static int estimateTimeout(long fileSize) {
        return Constant.DEFAULT_TIMEOUT;
    }

    public static int castToInt(long num) {
        long M = (long)1e9+7;
        return Math.toIntExact(num % M);
    }

    private static final String[] SIZE_UNITS = {"KB","MB","GB","TB"};

}
