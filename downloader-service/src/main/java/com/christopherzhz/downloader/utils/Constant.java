package com.christopherzhz.downloader.utils;

public class Constant {
    // default values
    public static final int DEFAULT_NTHREADS = 5;
    public static final long SIZE_THRESH = 1024;
    public static final int CONNECT_TIMEOUT = 120000; // 2 min
    public static final int READ_TIMEOUT = 5000; // 5 sec
    public static final int NETWORK_CHECK_FREQ = 10000; // 10 sec
    public static final int MAX_CONNECT_ATTEMPTS = 5;

    // async configs
    public static final int QUEUE_CAPACITY = 100;
    public static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    public static final int MAX_POOL_SIZE = CORE_POOL_SIZE;

    // thresholds
    public static final long LARGE_FILE_LINE = SIZE_THRESH*SIZE_THRESH*SIZE_THRESH*10; // 10GB
    public static final int MAX_NUM_THREADS = 300;
    public static final long MIN_BYTES_TO_SPLIT_THREAD = SIZE_THRESH*SIZE_THRESH*5; // 5 MB
    public static final long MAX_BYTES_PER_THREAD_IDEALLY = SIZE_THRESH*SIZE_THRESH*200; // 200 MB

    // strings
    public static final String RANGE = "Range";
    public static final String GET = "GET";
    public static final String USER_AGENT = "User-agent";
    public static final String BROWSER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.0.3705; .NET CLR 1.1.4322; .NET CLR 1.2.30703)";
    public static final String PING_CMD = "ping -c 1 www.google.com";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "fail";
}
