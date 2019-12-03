package com.christopherzhz.downloader.controller;

import com.christopherzhz.downloader.controller.broker.DownloadBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

public class Downloader {

    private static Logger LOG = LoggerFactory.getLogger(Downloader.class.getName());

    private URL url;
    private File destFile;
    private int NTHREADS;
    private long fileSize;
    private boolean hasResumeFeature;

    public Downloader(String url, String destDir, int nThreads) throws MalformedURLException, IOException {
        this.url = new URL(url);
        this.destFile = new File(destDir + File.pathSeparator + getFileNameByUrl(url));
        this.NTHREADS = nThreads;
        initConnection();
    }

    public void download() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
        raf.setLength(fileSize);
        raf.close();
        if (shouldUseSingleThread()) {
            new DownloadBroker(0, castToInt(fileSize), destFile, url, estimateTimeout(fileSize));
        } else {
            // ...
        }
    }

    private void initConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty(RANGE, genRangeString(0));
        while (true) {// TODO: set re-try limit
            try {
                conn.connect();
                fileSize = conn.getContentLengthLong();
                hasResumeFeature = conn.getResponseCode() == 206;
                conn.disconnect();
                break;
            } catch (ConnectException ce) {
                LOG.error("[ConnectException] Connection failed! Retrying..");
            }
        }
    }

    private boolean shouldUseSingleThread() {
        return !hasResumeFeature || fileSize < MIN_BYTES_TO_SPLIT_THREAD || NTHREADS == 1;
    }

    public static void main(String[] args) throws IOException {
        String testUrl = "http://speedtest.dallas.linode.com/100MB-dallas.bin";
        String testDestPath = "/Users/frankmac/Downloads/";
        Downloader downloader = new Downloader(testUrl, testDestPath, 5);
        // ...
    }
}
