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
