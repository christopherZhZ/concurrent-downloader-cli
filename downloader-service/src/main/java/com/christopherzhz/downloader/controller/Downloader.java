package com.christopherzhz.downloader.controller;

import com.christopherzhz.downloader.controller.worker.DownloadBroker;
import com.christopherzhz.downloader.controller.worker.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

public class Downloader {

    private static Logger LOG = LoggerFactory.getLogger(Downloader.class.getName());

    private URL url;
    private File destFile;
    private int NTHREADS;
    private long fileSize;
    private boolean hasResumeFeature;
    private Object waiting = new Object();

    AtomicLong downloadedSize = new AtomicLong(0);

    public Downloader(String url, String destDir, int nThreads) throws MalformedURLException {
        this.url = new URL(url);
        this.destFile = new File(destDir + File.pathSeparator + getFileNameByUrl(url));
        this.NTHREADS = nThreads;
        // TODO: if nthreads is too ridiculous with given file size, change it
    }

    public void download() throws IOException {
        // initialize connection
        initConnection();

        // create a file in destination directory with the same length and name
        RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
        raf.setLength(fileSize);
        raf.close();

        // start download brokers (threads)
        if (shouldUseSingleThread()) {
            new DownloadBroker(0, 0, fileSize-1, destFile, url, estimateTimeout(fileSize))
                .start();
        } else {
            long blockSize = fileSize / NTHREADS;
            IntStream.range(0, NTHREADS)
                .forEach(threadCnt -> {
                    long start = blockSize * threadCnt;
                    long end = threadCnt == NTHREADS - 1 ? fileSize - 1 : start + blockSize - 1;
                    new DownloadBroker(threadCnt, start, end, destFile, url, estimateTimeout(blockSize))
                        .start();
                    // TODO: add thread pool later
                });
        }

        // start progress monitor thread
        startMonitoring();

        // wait for progress monitor to notify the completion
        try {
            synchronized (waiting) {
                waiting.wait();
            }
        } catch (InterruptedException ie) {
            LOG.error("[InterruptedException] Download interrupted!");
        }

        // TODO: cleanups..
    }

    private void initConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty(RANGE, genRangeString(0));
        while (true) {// TODO: set re-try limit
            try {
                conn.connect();
                fileSize = conn.getContentLengthLong();
                hasResumeFeature = conn.getResponseCode() == 206;
//                conn.disconnect();
                break;
            } catch (ConnectException ce) {
                LOG.error("[ConnectException] Connection failed! Retrying..");
            }
        }
        LOG.info("Connection established!");
    }

    private boolean shouldUseSingleThread() {
        return !hasResumeFeature || fileSize < MIN_BYTES_TO_SPLIT_THREAD || NTHREADS == 1;
    }

    private void startMonitoring() {
        ProgressMonitor pm = new ProgressMonitor(downloadedSize, waiting, fileSize);
        pm.setDaemon(true);
        pm.start();
    }

    public static void main(String[] args) throws IOException {
        String testUrl = "http://speedtest.dallas.linode.com/100MB-dallas.bin";
        String testDestPath = "/Users/frankmac/Downloads/";
        Downloader downloader = new Downloader(testUrl, testDestPath, 5);
        // ...
    }
}
