package com.christopherzhz.downloader.utils;

import java.nio.file.Paths;

public class DownloaderUtils {

//    public static String getTempStorageDirectory() {
//        String baseDir = Paths.get("").toAbsolutePath().toString();
//        return baseDir + "/temp";
//    }

    public static String genRangeString(int start, int end) {
        return String.format("bytes=%d-%d", start, end);
    }

    public static String genRangeString(int start) {
        return String.format("bytes=%d-", start);
    }

    public static String getFileNameByUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    public static int estimateTimeout(long fileSize) {
        // ...
        return Constant.DEFAULT_TIMEOUT;
    }

    public static int castToInt(long num) {
        long M = (long)1e9+7;
        return Math.toIntExact(num % M);
    }

}
