package com.christopherzhz.downloader.utils;

public class Constant {

    // default values
    public static final int DEFAULT_NTHREADS = 5;
    public static final long SIZE_THRESH = 1024;
    public static final int CONNECT_TIMEOUT = 120000; // 2 min
    public static final int READ_TIMEOUT = 5000; // 5 sec

    // thresholds
    public static final long LARGE_FILE_LINE = SIZE_THRESH*SIZE_THRESH*SIZE_THRESH*10; // 10GB
    public static final int MAX_NUM_THREADS = 100;
    public static final int MAX_CONNECT_ATTEMPTS = 20;
    public static final long MIN_BYTES_TO_SPLIT_THREAD = SIZE_THRESH*SIZE_THRESH; // 1 MB
    public static final long MAX_BYTES_PER_THREAD_IDEALLY = SIZE_THRESH*SIZE_THRESH*SIZE_THRESH; // 1 GB

    // strings
    public static final String RANGE = "Range";
    public static final String GET = "GET";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "fail";

    // test urls
    public static final String URL_5MB = "http://ipv4.download.thinkbroadband.com:8080/5MB.zip";
    public static final String URL_100MB = "http://speedtest.dallas.linode.com/100MB-dallas.bin";
}
