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
        boolean success;
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
        runningThreads.decrementAndGet();
    }

    private boolean partialDownload() {
        LOG.debug("[Thread #" + threadCnt + "] : partialDownload begin.");
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod(GET);
            conn.setRequestProperty(RANGE, genRangeString(start, end));

            long size = conn.getHeaderFieldLong("Content-Length", -1);
            long blockSize = end - start + 1;
            if (size != blockSize) {
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
                    // TODO: resume when connection failed and re-connected
                }
            }
        } catch (SocketTimeoutException ste) {
            LOG.error("[SocketTimeoutException] threadCnt = " + threadCnt);
            ste.printStackTrace();
            return false;
        } catch (IOException ioe) {
            LOG.error("[IOException] threadCnt = " + threadCnt);
            ioe.printStackTrace();
            return false;
        }
        return true;
    }
}
