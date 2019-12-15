package com.christopherzhz.downloader.utils;

public class Constant {
    // default values
    public static final int DEFAULT_NTHREADS = 5;
    public static final long SIZE_THRESH = 1024;
    public static final int CONNECT_TIMEOUT = 120000; // 2 min
    public static final int READ_TIMEOUT = 5000; // 5 sec
    public static final int MAX_CONNECT_ATTEMPTS = 20;

    // async configs
    public static final int QUEUE_CAPACITY = 100; // as many tasks as user can wait on
    public static final int CORE_POOL_SIZE = 8; // constraint by number of logical cores of server machine's CPU
    public static final int MAX_POOL_SIZE = 16;

    // thresholds
    public static final long LARGE_FILE_LINE = SIZE_THRESH*SIZE_THRESH*SIZE_THRESH*10; // 10GB
    public static final int MAX_NUM_THREADS = 400;
    public static final long MIN_BYTES_TO_SPLIT_THREAD = SIZE_THRESH*SIZE_THRESH*5; // 5 MB
    public static final long MAX_BYTES_PER_THREAD_IDEALLY = SIZE_THRESH*SIZE_THRESH*200; // 200 MB

    // strings
    public static final String RANGE = "Range";
    public static final String GET = "GET";
    public static final String USER_AGENT = "User-agent";
    public static final String BROWSER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.0.3705; .NET CLR 1.1.4322; .NET CLR 1.2.30703)";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "fail";

    // test urls
    public static final String URL_16MB = "http://mirror.filearena.net/pub/speed/SpeedTest_16MB.dat?_ga=2.160681917.1281845165.1575751193-1985933931.1575751193";
    public static final String URL_32MB = "http://mirror.filearena.net/pub/speed/SpeedTest_32MB.dat?_ga=2.160681917.1281845165.1575751193-1985933931.1575751193";
    public static final String URL_100MB = "http://speedtest.dallas.linode.com/100MB-dallas.bin";
}
