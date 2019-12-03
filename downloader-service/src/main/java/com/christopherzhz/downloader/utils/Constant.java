package com.christopherzhz.downloader.utils;

public class Constant {

    // default values
    public static final int DEFAULT_TIMEOUT = 120000; // 2 min
    public static final int DEFAULT_NTHREADS = 10;

    // thresholds
    public static final int MAX_NUM_THREADS = 100;
    public static final long MIN_BYTES_TO_SPLIT_THREAD = 1024*1024; // 1 MB

    // strings
    public static final String RANGE = "Range";
}
