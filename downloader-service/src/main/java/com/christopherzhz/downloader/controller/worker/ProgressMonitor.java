package com.christopherzhz.downloader.controller.worker;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

@AllArgsConstructor
public class ProgressMonitor extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(ProgressMonitor.class.getSimpleName());

    private AtomicLong downloadedSize;
    private AtomicInteger runningThreads;
    private Object monitorLock;
    private long totalSize;

    @Override
    public void run() {
        String totalSizeStr = genFileSizeString(totalSize);
        long sizeCurr, sizeLastSec = 0;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOG.error("[InterruptedException] Monitor interrupted!");
            }

            sizeCurr = downloadedSize.get();
            String speedStr = genFileSizeString(sizeCurr - sizeLastSec);
            String msg = String.format("[Downloading] %s / %s (%.2f%%)  |  %s/s",
                    genFileSizeString(sizeCurr), totalSizeStr, sizeCurr / (float)totalSize * 100, speedStr);
            LOG.info(msg);

            sizeLastSec = sizeCurr;

            // notify Downloader that download finished
            if (runningThreads.get() == 0) {
                synchronized (monitorLock) {
                    monitorLock.notifyAll();
                }
            }
        }
    }
}
