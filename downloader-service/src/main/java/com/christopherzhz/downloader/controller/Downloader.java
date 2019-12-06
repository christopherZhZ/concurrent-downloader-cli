package com.christopherzhz.downloader.controller;

import com.christopherzhz.downloader.controller.worker.DownloadWorker;
import com.christopherzhz.downloader.controller.worker.ProgressMonitor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

@ToString
public class Downloader {

    private static Logger LOG = LoggerFactory.getLogger(Downloader.class.getSimpleName());

    private URL url;
    private File destFile;
    private int NTHREADS;
    private long fileSize;
    private boolean hasResumeFeature;

    @ToString.Exclude private Object monitorLock = new Object();
    @ToString.Exclude AtomicLong downloadedSize = new AtomicLong(0);
    @ToString.Exclude AtomicInteger runningThreads = new AtomicInteger(0);

    public Downloader(String url, String destDir, int nThreads) throws MalformedURLException {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(destDir) || nThreads < 1) {
            throw new IllegalArgumentException("Invalid URL / destDir / nThreads");
        }

        String processedUrl = StringUtils.trimAllWhitespace(url);
        String processedDir = StringUtils.trimTrailingCharacter(StringUtils.trimAllWhitespace(destDir), File.separatorChar);
        String fullPath =  processedDir + File.separator + getFileNameByUrl(processedUrl);
        this.url = new URL(processedUrl);
        this.destFile = new File(fullPath);
        this.NTHREADS = optimizeNThreads(nThreads);
    }

    public void download() throws IOException {
        // initialize connection
        initConnection();

        // log basic info
        LOG.debug("[------TARGET FILE INFO------] " + this.toString());

        // create a file in destination directory with the same length and name
        RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
        raf.setLength(fileSize);
        raf.close();

        // start download workers (threads)
        if (shouldUseSingleThread()) {
            new DownloadWorker(0, 0, fileSize-1, destFile, url, downloadedSize, runningThreads)
                .start();
        } else {
            long blockSize = fileSize / NTHREADS;
            IntStream.range(0, NTHREADS)
                .forEach(threadCnt -> {
                    long start = blockSize * threadCnt;
                    long end = threadCnt == NTHREADS - 1 ? fileSize - 1 : start + blockSize - 1;
                    new DownloadWorker(threadCnt, start, end, destFile, url, downloadedSize, runningThreads)
                        .start();
                    // TODO: add thread pool later
                });
        }

        // start progress monitor thread
        startMonitoring();

        // wait for progress monitor to notify the completion
        try {
            synchronized (monitorLock) {
                monitorLock.wait();
            }
        } catch (InterruptedException ie) {
            LOG.error("[InterruptedException] Download interrupted!");
            // TODO: error handling
        }
    }

    private void initConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty(RANGE, genRangeString(0, -1));
        int trying = 0, resCode;
        while (trying < MAX_CONNECT_ATTEMPTS) {
            try {
                conn.connect();
                resCode = conn.getResponseCode();

                if (resCode == 403) {
                    // TODO: fake as browser
                }
                hasResumeFeature = resCode == 206;
                LOG.debug("initConnection: res code: " + conn.getResponseCode());
                break;
            } catch (ConnectException ce) {
                LOG.error("[ConnectException] Connection failed! Retrying..");
                trying++;
            }
        }
        fileSize = conn.getContentLengthLong();
        LOG.debug("Connection established!");
    }

    private boolean shouldUseSingleThread() {
        return !hasResumeFeature || fileSize < MIN_BYTES_TO_SPLIT_THREAD || NTHREADS == 1;
    }

    private void startMonitoring() {
        ProgressMonitor pm = new ProgressMonitor(downloadedSize, runningThreads, monitorLock, fileSize);
        pm.setDaemon(true);
        pm.start();
    }

    // for testing purpose
    public static void main(String[] args) throws IOException {
        String testDestPath = "/Users/frankmac/Downloads";
        new Downloader(URL_100MB, testDestPath, 5).download();
    }
}
