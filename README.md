# concurrent-downloader-cli
A command line tool for concurrently downloading file with given url.


## Features

- Customizable threads number
- Resuming after recovery from internet failure
- File partitioned in blocks intelligently, and then concurrently downloaded
- No quality loss
- Thread pool for download tasks to avoid throughput degradation and overhead


## Installation

- Clone the project
- Go to project's root directory in terminal
- Run:
```sh
pip install -- editable .
```


## Usage

- Go to `/concurrent-downloader-cli/downloader-service` in terminal
- Run: (Make sure Maven is installed and `mvn` cli is added to $PATH)
```sh
mvn spring-boot:run
```
to start the downloader web service.
- In a new terminal window, you can run the following commands (in any directory):
```sh
downloader <URL>
downloader <URL> -c <nThreads>
```
to download the file to the current directory.

- In the terminal which runs spring boot server, you can view the download progress:

![alt text](./screenshots/screenshot-monitor.png)


## Project Design

#### 1. Overview

concurrent-downloader-cli is built with the Python package [Click](https://click.palletsprojects.com/en/7.x/) and Spring Boot. The downloading process is implemented in Java and exposed as web service using Spring Boot. Click is used for wrapping the api call to a CLI tool.

The file isn't delivered through the api response. Downloaded files are side effects from calling download api as users are required to run downloader-service locally. The api response only contains the task status (successful / failed).

When a download request is sent through command, the application first does the argument check and adjust the `nThreads` intelligently based on the file size. The file in destination directory is first created. Then the download task is partitioned into `nThreads` approximately equal-sized parts (only if the url's server supports download resume). A `DownloadWorker` is responsible for reading the partial file and writing to the corresponding part to destination file. Meanwhile, a daemon thread `ProgressMonitor` is running and monitoring the download progress.


#### 2. Design Decisions

- ##### Connection Failure

  The original implementation for connection failure handling is, retrying the connection check in a `while(true)` loop as long as `SocketTimeoutException` is detected. (Similar to a spinlock)

  The drawback is that, during an internet failure, `nThreads` threads are spinning the `while(true)` loop at the same time. This costs a lot of meaningless computation.

  The current implementation is locking all `DownloadWorker`s after 5 failed attempts of connection, and then starting a `NetworkMonitor` thread that checks network recovery every 10 seconds. As soon as the network recovers, wake up all `DownloadWorker`s. Because each `DownloadWorker` locally stores the current downloaded bytes for its own part, `DownloadWorker` knows the byte position to continue.

- ##### Resource Usage

  Spring Boot's `ThreadPoolTaskExecutor` is used to avoid overhead in bulk download requests. The default and max core pool size is equal to the number of logical cores on the machine running the downloader service. The queue capacity is 100.

  It means that in the extreme case, all logical cores of user's CPU will be used on download tasks; also, the thread pool accept at most 100 download tasks concurrently.

  Based on my research, the maximal number of threads is typically 32K (by Linux kernel limit). Thus, I made this rule: each download task can have at most 300 download workers (threads). By having this limitation, handling bulk requests with maximized `nThreads` is guaranteed to be safe.

- ##### Unexpected User Behaviors

  It is possible that user doesn't use the tool wisely. For example, user may:

  provide wrong/malformed/non-downloadable url;

  specify a ridiculous `nThreads` (too many threads for a small file, or few threads for a large file);

  try to download a file larger than available space;

  try to send duplicated download request (same url at same destination directory)

  interrupt the CLI during downloading;

  etc.

  These are all handled properly:

  Invalid urls and CLI interruption will lead to failure messages.

  `nThreads` is optimized according to the following rules:
    - Each thread's part is at least 5MB
    - Each thread's part is at most 200MB (unless the file is so large such that `size/300 > 200MB`)

  No enough space will cause `IOException`, task will stop and created file will be deleted.

  Tasks' urls and destination directories are stored in a `ConcurrentHashMap`, and sending duplicated download request will get failure message.

- ##### Concurrent Downloading

  As mentioned in overview, file downloading happens concurrently, which is faster than single-threaded approach.

  Threads are actual threads because Java's `Thread` is used in this project. That's also the reason I chose Spring Boot over NodeJS.

- ##### Downloaded File Quality

  No loss on file quality as they are written using the raw bytes, and downloaded resuming from network recovery is implemented nicely (refer to Connection Failure). As long as exceptions occur, the file gets deleted. The result file either gets downloaded completely, or doesn't exist in exception.

- ##### Scalability

  This application uses a bounded thread pool design in order to avoid throughput degradation, overheads, and the danger of having more than 30K threads.

  The drawback of this design is of course, the low performance in huge number of requests. But in my opinion, the safety and robustness of application are more important than speed in bulk requests.

#### 3. Things to Improve

- For very large file, my idea was to let the `DownloadWorker` who finished its job earlier take some other workers' job (the busiest worker's job). That is, take half of the job from the worker whose file block has the largest un-downloaded part.
This idea is quite complex to implement.

- Image file type not supported yet.

- Progress bar works best when one download task runs at a time. When multiple tasks run together, currently there are no separate bars for each task.
