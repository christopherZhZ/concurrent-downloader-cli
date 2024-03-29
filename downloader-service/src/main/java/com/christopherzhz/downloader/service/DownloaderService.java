package com.christopherzhz.downloader.service;

import com.christopherzhz.downloader.downloaderImpl.Downloader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.christopherzhz.downloader.utils.Constant.*;

@RestController
@RequestMapping("/downloader")
public class DownloaderService {

    private static ConcurrentHashMap<String, HashSet<String>> urlDestMap = new ConcurrentHashMap<>();

    private static class DownloaderResp extends HashMap {
        private DownloaderResp() {}

        public static DownloaderResp create(boolean success, String msg) {
            DownloaderResp resp = new DownloaderResp();
            resp.put("status", success ? STATUS_SUCCESS : STATUS_FAIL);
            resp.put("message", msg);
            return resp;
        }
    }
    private CompletableFuture<DownloaderResp> success() {
        return CompletableFuture.completedFuture(DownloaderResp.create(true, ""));
    }
    private CompletableFuture<DownloaderResp> fail(String msg) {
        return CompletableFuture.completedFuture(DownloaderResp.create(false, msg));
    }

    @PostMapping("/download")
    @Async("asyncExecutor")
    public CompletableFuture<DownloaderResp> download(@RequestParam String url,
                                                      @RequestParam String destDir,
                                                      @RequestParam int nThreads) {
        if (urlDestMap.containsKey(url) && urlDestMap.get(url).contains(destDir)) {
            return fail("Task already exists!");
        }

        if (!urlDestMap.containsKey(url)) {
            urlDestMap.put(url, new HashSet<>(Collections.singletonList(destDir)));
        } else {
            urlDestMap.get(url).add(destDir);
        }
        Downloader downloader = null;
        try {
            downloader = new Downloader(url, destDir, nThreads);
            downloader.download();
        } catch (Exception exp) {
            if (downloader != null) {
                downloader.getDestFile().delete();
            }
            return fail(String.format("[%s] %s", exp.getClass().getSimpleName(), exp.getMessage()));
        } finally {
            urlDestMap.get(url).remove(destDir);
        }
        return success();
    }
}
