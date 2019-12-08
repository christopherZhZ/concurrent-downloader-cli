#!/bin/bash

curl -i -X HEAD --header "Range: bytes=0-" $1
