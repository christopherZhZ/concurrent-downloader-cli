package com.christopherzhz.downloader.controller.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

import static com.christopherzhz.downloader.utils.Constant.*;
import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

public class ProgressMonitor extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(ProgressMonitor.class.getName());

    private AtomicLong downloadedSize;
    private Object waiting;
    private long totalSize;

    public ProgressMonitor(AtomicLong downloadedSize, Object waiting, long totalSize) {
        this.downloadedSize = downloadedSize;
        this.waiting = waiting;
        this.totalSize = totalSize;
    }

    @Override
    public void run() {
        String totalSizeStr = genFileSizeString(totalSize);
        long sizeCurr = 0, sizeLastSec = 0;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOG.error("[InterruptedException] Monitor interrupted!");
            }

            sizeCurr = downloadedSize.get();
            String speedStr = genFileSizeString(sizeCurr - sizeLastSec);
            String msg = String.format("[Downloading] %s / %s (%.2fd%%)  |  %s/s",
                    genFileSizeString(sizeCurr), totalSizeStr, sizeCurr / (float)totalSize * 100, speedStr);
            LOG.info(msg);

            sizeLastSec = sizeCurr;

//            if (/* check if downloading done */) {
//                synchronized (waiting) {
//                    waiting.notifyAll();
//                }
//            }
        }
    }
}
