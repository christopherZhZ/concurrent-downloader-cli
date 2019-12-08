package com.christopherzhz.downloader.downloaderImpl.worker;

import lombok.AllArgsConstructor;
import me.tongfei.progressbar.ProgressBar;
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
        ProgressBar pb = new ProgressBar("Downloader", 100);
        String totalSizeStr = genFileSizeString(totalSize);
        long sizeCurr, sizeLastSec = 0;
        double percentage;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                LOG.error("[InterruptedException] Monitor interrupted!");
            }

            sizeCurr = downloadedSize.get();
            String speedStr = genFileSizeString(sizeCurr - sizeLastSec);
//            String msg = String.format("[Monitor] %s / %s (%.1f%%)  |  %s/s",
//                    genFileSizeString(sizeCurr), totalSizeStr, sizeCurr / (double)totalSize * 100, speedStr);
//            LOG.info(msg);

            percentage = sizeCurr / (double)totalSize * 100;
            pb.stepTo((long)Math.floor(percentage));
            pb.setExtraMessage(String.format("Downloading...  |  %s / %s", genFileSizeString(sizeCurr), totalSizeStr));
            sizeLastSec = sizeCurr;

            // notify Downloader that download finished
            if (runningThreads.get() == 0) {
                synchronized (monitorLock) {
                    monitorLock.notifyAll();
                }
                break;
            }
        }
        pb.close();
    }
}
