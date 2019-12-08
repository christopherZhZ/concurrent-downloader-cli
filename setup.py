from setuptools import setup, find_packages

setup(
    name='concurrent-downloader-cli',
    version='1.0',
    # packages=find_packages(),
    py_modules=['downloader-cli'],
    include_package_data=True,
    install_requires=[
        'click',
        'requests',
    ],
    entry_points='''
        [console_scripts]
        downloader=downloader_cli:downloader
    ''',
)
