package com.christopherzhz.downloader.utils;

import static com.christopherzhz.downloader.utils.Constant.*;

public class DownloaderUtils {

//    public static String getTempStorageDirectory() {
//        String baseDir = Paths.get("").toAbsolutePath().toString();
//        return baseDir + "/temp";
//    }

    public static String genRangeString(long start, long end) {
        return end < 0 ? String.format("bytes=%d-", start, end) :
                String.format("bytes=%d-%d", start, end);
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

    public static int optimizeNThreads(int nThreads) {
        // TODO: if nthreads is too ridiculous with given file size, change it
        // ...
        return nThreads;
    }

    public static int estimateTimeout(long fileSize) {
        return castToInt(fileSize / SIZE_THRESH);
    }

    public static int castToInt(long num) {
        long M = (long)1e9+7;
        return Math.toIntExact(num % M);
    }

    private static final String[] SIZE_UNITS = {"KB","MB","GB","TB"};

    // for testing purpose
    public static void main(String[] args) {
        System.out.println(genFileSizeString(100*1024*1024));
    }

}
