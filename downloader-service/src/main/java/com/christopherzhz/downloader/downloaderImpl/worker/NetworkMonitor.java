package com.christopherzhz.downloader.downloaderImpl.worker;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.christopherzhz.downloader.utils.Constant.*;

/**
 * NetworkMonitor is a thread used for checking the network recovery periodically
 */
@AllArgsConstructor
public class NetworkMonitor extends Thread {

    private static Logger LOG = LoggerFactory.getLogger(NetworkMonitor.class.getSimpleName());

    private final Object networkMonitorLock;

    @Override
    public void run() {
        System.out.println("No internet. Download paused.");
        while (true) {
            try {
                Process proc = Runtime.getRuntime().exec(PING_CMD);
                int exitValue = proc.waitFor();
                LOG.debug("PING res = " + exitValue);
                if (exitValue == 0) {
                    System.out.println("Internet re-connected. Will resume in a second.");
                    synchronized (networkMonitorLock) {
                        networkMonitorLock.notifyAll();
                    }
                    break;
                }

                Thread.sleep(NETWORK_CHECK_FREQ);
            } catch (IOException | InterruptedException ex) {
                LOG.error("[Exception] Network monitoring interrupted!");
            }
        }
    }

    // for testing purpose
    public static void main(String[] args) throws Exception {
        Process p1 = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.com");
        int returnVal = p1.waitFor();
        boolean reachable = (returnVal==0);
        System.out.println(reachable);
    }
}
