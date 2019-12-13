package com.christopherzhz.downloader.service;

import com.christopherzhz.downloader.downloaderImpl.Downloader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static com.christopherzhz.downloader.utils.Constant.*;

@RestController
@RequestMapping("/downloader")
public class DownloaderService {

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
        try {
            Downloader downloader = new Downloader(url, destDir, nThreads);
            downloader.download();
        } catch (IOException | InterruptedException exp) {
            System.out.println("*** exp class : "+exp.getClass());
            return fail(exp.getMessage());
        }
        return success();
    }
}
