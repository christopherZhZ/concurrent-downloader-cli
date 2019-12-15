package com.christopherzhz.downloader.utils;

import org.springframework.util.StringUtils;

import static com.christopherzhz.downloader.utils.Constant.*;

public class DownloaderUtils {

    private static final String[] SIZE_UNITS = {"KB","MB","GB","TB"};

    public static String genRangeString(long start, long end) {
        return end < 0 ? String.format("bytes=%d-", start, end) :
                String.format("bytes=%d-%d", start, end);
    }

    public static String getFileNameByUrl(String url) {
        String lastSegment = StringUtils.getFilename(url);
        int indQuesMark = lastSegment.indexOf('?');
        return indQuesMark < 0 ? lastSegment : lastSegment.substring(0, indQuesMark);
    }

    public static String genFileSizeString(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double size = (double) bytes;
        int u = -1;
        do {
            size /= SIZE_THRESH;
            ++u;
        } while(size >= SIZE_THRESH && u < SIZE_UNITS.length-1);
        return String.format("%.2f %s", size, SIZE_UNITS[u]);
    }

    public static int optimizeNThreads(int nThreads, long fileSize) {
        long blockSize = roundDivide(fileSize, nThreads);
        if (blockSize < MIN_BYTES_TO_SPLIT_THREAD) {
            return (int)roundDivide(fileSize, MIN_BYTES_TO_SPLIT_THREAD);
        }
        if (blockSize <= MAX_BYTES_PER_THREAD_IDEALLY) {
            return nThreads;
        }

        // blockSize > MAX_BYTES_PER_THREAD_IDEALLY; either nThreads too small, or file too large
        int idealNT = (int)roundDivide(fileSize, MAX_BYTES_PER_THREAD_IDEALLY);
        if (idealNT <= MAX_NUM_THREADS) {
            return idealNT;
        }
        return MAX_NUM_THREADS;
    }

    private static long roundDivide(long a, long b) {
        return (long)Math.ceil(a / (double)b);
    }

    private static int castToInt(long num) {
        long M = (long)1e9+7;
        return Math.toIntExact(num % M);
    }

    // for testing purpose
    public static void main(String[] args) {
//        System.out.println(getFileNameByUrl("http://mirror.filearena.net/pub/speed/SpeedTest_32MB.dat?_ga=2.160681917.1281845165.1575751193-1985933931.1575751193"));
        System.out.println(roundDivide(2147483648L, MAX_BYTES_PER_THREAD_IDEALLY));
    }

}
