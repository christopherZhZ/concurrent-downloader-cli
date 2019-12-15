package com.christopherzhz.downloader.downloaderImpl.worker;

import lombok.AllArgsConstructor;
import me.tongfei.progressbar.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.christopherzhz.downloader.utils.DownloaderUtils.*;

/**
 * ProgressMonitor is a thread that monitors the download progress of tasks
 */
@AllArgsConstructor
public class ProgressMonitor extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(ProgressMonitor.class.getSimpleName());

    private AtomicLong downloadedSize;
    private AtomicInteger runningThreads;
    private final Object progressMonitorLock;

    private long totalSize;
    private String fileName;

    @Override
    public void run() {
        System.out.println(String.format("Start downloading file: '%s'", fileName));
        ProgressBar pb = new ProgressBar("Task: " + fileName, 100);
        String totalSizeStr = genFileSizeString(totalSize);
        long sizeCurr, sizeLastSec = 0;
        int percentage;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOG.error("[InterruptedException] Monitor interrupted!");
            }

            sizeCurr = downloadedSize.get();
            String deltaSize = genFileSizeString(sizeCurr - sizeLastSec);
            String tailMessage = String.format("[%s / %s]  %s/s",
                    genFileSizeString(sizeCurr), totalSizeStr, deltaSize);
            percentage = (int)Math.floor(sizeCurr / (double)totalSize * 100);

            pb.stepTo(percentage);
            pb.setExtraMessage(tailMessage);
            sizeLastSec = sizeCurr;

            // notify Downloader that download finished
            if (runningThreads.get() == 0) {
                synchronized (progressMonitorLock) {
                    progressMonitorLock.notifyAll();
                }
                break;
            }
        }
        pb.close();
        System.out.println(String.format("Download completed for file: '%s' ", fileName));
    }
}
