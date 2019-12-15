package com.christopherzhz.downloader.downloaderImpl.worker;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private final Object networkMonitorLock;
    private final NetworkMonitor networkMonitor;

    @Override
    public void run() {
        runningThreads.incrementAndGet();
        localDownloadedSize.set((long)0);
        while (true) {
            boolean success = false;
            int trying = 0;
            while (trying < MAX_CONNECT_ATTEMPTS) {
                success = partialDownload();
                if (success) {
                    LOG.debug("[Thread #" + threadCnt + "] : partialDownload success.");
                    break;
                }
                LOG.debug("[Thread #" + threadCnt + "] : partialDownload failed. Retrying..");
                trying++;
            }
            if (success) break;
            if (trying == MAX_CONNECT_ATTEMPTS) {
                try {
                    networkMonitor.start();
                    LOG.debug("STARTED network monitor!");
                } catch (IllegalThreadStateException itse) {
                    // it's fine because other download worker has started the network monitor
                    LOG.debug("Duplicated monitor start attempt.");
                }
                try {
                    synchronized (networkMonitorLock) {
                        networkMonitorLock.wait();
                    }
                } catch (InterruptedException ie) {
                    LOG.error("[InterruptedException] Network monitor interrupted!");
                }
            }
        }
        runningThreads.decrementAndGet();
    }

    private boolean partialDownload() {
        LOG.debug("[Thread #" + threadCnt + "] : partialDownload begin.");

        // if resume after network recovery
        long localDownloaded = localDownloadedSize.get();
        boolean resumeAfterRecovery = localDownloaded > 0;
        long actualStart;
        if (resumeAfterRecovery) {
            actualStart = start + localDownloaded;
            LOG.debug("partialDownload (resume): globalDownloaded = " + downloadedSize.get());
            LOG.debug("partialDownload (resume): localDownloaded = " + localDownloaded);
            LOG.debug("partialDownload (resume): start = " + start);
        } else {
            actualStart = start;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // fake as a browser client to avoid 403
            conn.setRequestProperty(USER_AGENT, BROWSER_AGENT);
            conn.setRequestProperty(RANGE, genRangeString(actualStart, end));
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod(GET);

            long size = conn.getHeaderFieldLong("Content-Length", -1);
            long blockSize = end - actualStart + 1;
            if (!resumeAfterRecovery && size != blockSize) {
                LOG.error("Unexpected block size! size=" + size + " | blockSize=" + blockSize);
                return false;
            }

            // request has succeeded and the body contains the requested ranges of data
            LOG.debug("[Thread #" + threadCnt + "] : partialDownload: connect resCode: " + conn.getResponseCode());
            if (conn.getResponseCode() == 206) {
                RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
                // move the pointer to this worker's part
                raf.seek(actualStart);
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
