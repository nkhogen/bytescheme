#!/bin/bash
sudo mkdir -p /controlboard/conf/security/ssl
sudo mkdir -p /controlboard/libs
sudo mkdir -p /controlboard/logs
sudo mkdir -p /controlboard/run
sudo mkdir -p /controlboard/bin

# Access video device /dev/video0
sudo modprobe bcm2835-v4l2