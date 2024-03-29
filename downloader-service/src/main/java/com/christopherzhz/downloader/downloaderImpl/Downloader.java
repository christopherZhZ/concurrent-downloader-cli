package com.christopherzhz.downloader.downloaderImpl;

import com.christopherzhz.downloader.downloaderImpl.worker.DownloadWorker;
import com.christopherzhz.downloader.downloaderImpl.worker.NetworkMonitor;
import com.christopherzhz.downloader.downloaderImpl.worker.ProgressMonitor;
import lombok.Getter;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

@ToString
public class Downloader {

    private static Logger LOG = LoggerFactory.getLogger(Downloader.class.getSimpleName());

    private URL url;
    @Getter private File destFile;
    private int NTHREADS;
    private long fileSize;
    private boolean hasResumeFeature;
    private String taskID;

    // a lock object for progress monitor
    @ToString.Exclude private final Object progressMonitorLock = new Object();

    // a lock object for network monitor
    @ToString.Exclude private final Object networkMonitorLock = new Object();

    // an atomic size accumulator representing downloaded file size
    @ToString.Exclude AtomicLong downloadedSize = new AtomicLong(0);

    // an atomic counter of active threads for the current task
    @ToString.Exclude AtomicInteger runningThreads = new AtomicInteger(0);

    public Downloader(String url, String destDir, int nThreads)
            throws MalformedURLException {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(destDir) || nThreads < 1) {
            throw new IllegalArgumentException("Invalid URL / destDir / nThreads");
        }

        String processedUrl = StringUtils.trimAllWhitespace(url);
        String processedDir = StringUtils.trimTrailingCharacter(StringUtils.trimAllWhitespace(destDir), File.separatorChar);
        String fullPath =  processedDir + File.separator + getFileNameByUrl(processedUrl);
        this.url = new URL(processedUrl);
        this.destFile = new File(fullPath);
        this.NTHREADS = nThreads;
        this.taskID = UUID.randomUUID().toString();
    }

    public void download() throws IOException, InterruptedException {
        // initialize connection
        initConnection();

        // log basic info
        LOG.debug("[------TARGET FILE INFO------] " + this.toString());

//        if (fileSize < LARGE_FILE_LINE) {
            regularFileDownload();
//        } else {
//            veryLargeFileDownload();
//        }

        LOG.debug("[------DOWNLOAD FINISHED------] ");
    }

    private void regularFileDownload() throws IOException, InterruptedException {
        // create a file in destination directory with the same length and name
        RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
        raf.setLength(fileSize);
        raf.close();

        // create the network monitor
        NetworkMonitor nm = new NetworkMonitor(networkMonitorLock);

        // start download workers (threads)
        if (shouldUseSingleThread()) {
            new DownloadWorker(0, 0, fileSize-1,
                               destFile, url, downloadedSize, runningThreads,
                               networkMonitorLock, nm)
                .start();
        } else {
            long blockSize = fileSize / NTHREADS;
            IntStream.range(0, NTHREADS)
                .forEach(threadCnt -> {
                    long start = blockSize * threadCnt;
                    long end = threadCnt == NTHREADS - 1 ? fileSize - 1 : start + blockSize - 1;
                    new DownloadWorker(threadCnt, start, end,
                                       destFile, url, downloadedSize, runningThreads,
                                       networkMonitorLock, nm)
                        .start();
                });
        }

        // start progress monitor thread
        startMonitoring(destFile.getName());

        // wait for progress monitor to notify the completion
        try {
            synchronized (progressMonitorLock) {
                progressMonitorLock.wait();
            }
        } catch (InterruptedException ie) {
            LOG.error("[InterruptedException] Download interrupted!");
            throw ie;
        }
    }

    private void veryLargeFileDownload() throws IOException, InterruptedException {
        // very large file download approach
        // idea: when a download worker finished its job,
        //       let it take part of other workers' jobs
    }

    private void initConnection() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // fake as a browser client to avoid 403
        conn.setRequestProperty(USER_AGENT, BROWSER_AGENT);
        conn.setRequestProperty(RANGE, genRangeString(0, -1));
        conn.setReadTimeout(READ_TIMEOUT);
        int trying = 0, resCode;
        while (trying < MAX_CONNECT_ATTEMPTS) {
            try {
                conn.connect();
                resCode = conn.getResponseCode();
                LOG.debug("initConnection: res code: " + conn.getResponseCode());

                hasResumeFeature = resCode == 206;
                break;
            } catch (ConnectException ce) {
                LOG.error("[ConnectException] Connection failed! Retrying..");
                trying++;
            }
        }
        if (trying == MAX_CONNECT_ATTEMPTS) {
            throw new ConnectException("Cannot connect to the given URL");
        }
        fileSize = conn.getContentLengthLong();
        // adjust nThreads wisely
        NTHREADS = optimizeNThreads(NTHREADS, fileSize);
        LOG.debug("initConnection: optimizeNT result: " + NTHREADS);
        LOG.debug("Connection established!");
    }

    private boolean shouldUseSingleThread() {
        return !hasResumeFeature || fileSize < MIN_BYTES_TO_SPLIT_THREAD || NTHREADS == 1;
    }

    private void startMonitoring(String fileName) {
        ProgressMonitor pm = new ProgressMonitor(downloadedSize, runningThreads, progressMonitorLock, fileSize, fileName);
        pm.setDaemon(true);
        pm.start();
    }

    // for testing purpose
    public static void main(String[] args) throws IOException, InterruptedException {
        String testDestPath = "/Users/frankmac/Downloads";
        new Downloader("http://speedtest.dallas.linode.com/100MB-dallas.bin", testDestPath, 5).download();
    }
}
