package com.christopherzhz.downloader.downloaderImpl.worker;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

@AllArgsConstructor
public class DownloadWorker extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(DownloadWorker.class.getSimpleName());

    // local storage for this worker (thread) to store downloaded size in this block
    private static ThreadLocal<Long> localDownloadedSize = new ThreadLocal<>();

    private int threadCnt;
    private long start;
    private long end;
    private File destFile;
    private URL url;

    private AtomicLong downloadedSize;
    private AtomicInteger runningThreads;

    @Override
    public void run() {
        runningThreads.incrementAndGet();
        localDownloadedSize.set((long)0);
        boolean success;
//        int trying = 0;
//        while (trying < MAX_CONNECT_ATTEMPTS) {
        while (true) {
            success = partialDownload();
            if (success) {
                LOG.debug("[Thread #" + threadCnt + "] : partialDownload success.");
                break;
            }
            LOG.debug("[Thread #" + threadCnt + "] : partialDownload failed. Retrying..");
//            trying++;

            // Re-trying every 4 seconds
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ie) {
                LOG.error("[InterruptedException] Retrying sleep interrupted!");
            }
        }

//        if (trying == MAX_CONNECT_ATTEMPTS) {
//            try {
//                synchronized (lockObj) {
//                    lockObj.wait();
//                }
//            } catch (InterruptedException ie) {
//                LOG.error("[InterruptedException] Network monitor interrupted!");
//                throw ie;
//            }
//        }
        runningThreads.decrementAndGet();
    }

    private boolean partialDownload() {
        LOG.debug("[Thread #" + threadCnt + "] : partialDownload begin.");

        // if resume after network recovery
        long localDownloaded = localDownloadedSize.get();
        boolean resumeAfterRecovery = localDownloaded > 0;
        if (resumeAfterRecovery) {
            start = localDownloaded;
            LOG.debug("partialDownload (resume): globalDownloaded = " + downloadedSize.get());
            LOG.debug("partialDownload (resume): localDownloaded = " + localDownloaded);
            LOG.debug("partialDownload (resume): start = " + start);
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod(GET);
            conn.setRequestProperty(RANGE, genRangeString(start, end));

            long size = conn.getHeaderFieldLong("Content-Length", -1);
            long blockSize = end - start + 1;
            if (!resumeAfterRecovery && size != blockSize) {
                LOG.error("Unexpected block size!");
                return false;
            }

            // request has succeeded and the body contains the requested ranges of data
            LOG.debug("[Thread #" + threadCnt + "] : partialDownload: connect resCode: " + conn.getResponseCode());
            if (conn.getResponseCode() == 206) {
                RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
                // move the pointer to this worker's part
                raf.seek(start);
                InputStream in = conn.getInputStream();
                byte buffer[] = new byte[1024];
                int i;
                // read and write to the file
                while ((i = in.read(buffer)) != -1) {
                    raf.write(buffer, 0, i);
                    downloadedSize.addAndGet(i);
                    localDownloadedSize.set(localDownloadedSize.get() + i);
                }
            }
        } catch (SocketTimeoutException ste) {
            LOG.error("[SocketTimeoutException] threadCnt = " + threadCnt);
//            ste.printStackTrace();
            return false;
        } catch (IOException ioe) {
            LOG.error("[IOException] threadCnt = " + threadCnt);
//            ioe.printStackTrace();
            return false;
        }
        return true;
    }
}
