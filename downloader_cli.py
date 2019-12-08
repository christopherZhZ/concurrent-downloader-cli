import click
import requests
import os
import sys
from cli_helper import printer
from cli_config import *

@click.command(short_help=downloader_basic_help_msg)
@click.argument('url')
@click.option('-c', default=5, type=click.IntRange(min=1), help='Max number of threads (1-300, default 5)')
def downloader(url, c):
    """
    \b
    downloader <url>
    \b
    downloader <url> -c <nThreads>
    """
    if c > 300:
        printer.warn('nThreads adjusted to 300 automatically');
        nThreads = 300
    else:
        nThreads = c
    payload = {'url': url, 'destDir': os.getcwd(), 'nThreads': nThreads}
    resp = None
    try:
        resp = requests.post(downloader_url, params=payload)
    except:
        e = sys.exc_info()[0];
        printer.err(e)
        printer.err('The Spring Boot server might be down.')
        return
    json_resp = resp.json()
    printer.new_line()
    print(json_resp)
    if json_resp['status'] == 'fail':
        printer.fail('DOWNLOAD FAILED!')
        printer.err(json_resp['message'])
    else:
        printer.success('DOWNLOAD FINISHED!')
