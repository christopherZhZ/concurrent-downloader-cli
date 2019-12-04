package com.christopherzhz.downloader.controller.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

public class DownloadBroker extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(DownloadBroker.class.getName());

    private int threadCnt;
    private long start;
    private long end;
    private File destFile;
    private URL url;
    private int TIMEOUT;

    public DownloadBroker(int threadCnt, long start, long end, File destFile, URL url, int timeout) {
        this.threadCnt = threadCnt;
        this.start = start;
        this.end = end;
        this.destFile = destFile;
        this.url = url;
        this.TIMEOUT = timeout;
    }

    @Override
    public void run() {
        boolean success = false;
        while (true) {// TODO: set re-try limit
            success = partialDownload();
        }
    }

    private boolean partialDownload() {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty(RANGE, genRangeString(start, end));
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.connect();

            long size = conn.getHeaderFieldLong("Content-Length", -1);
            long blockSize = end - start + 1;
            if (size != blockSize) return false;

//            if (out == null) {
//                String tempPath = String.format("%s.part%d.tmp", destFile.getAbsolutePath(), threadCnt);
//                out = new FileOutputStream(tempPath);
//            }

            // request has succeeded and the body contains the requested ranges of data
            if (conn.getResponseCode() == 206) {
                // ...
            }
            InputStream in = conn.getInputStream();


        } catch (IOException ioe) {
            LOG.error("[IOException] threadCnt = " + threadCnt);
            return false;
        }
        return true;
    }
}
