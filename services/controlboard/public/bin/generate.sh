#!/bin/bash
sudo systemctl stop controlboard.service

sudo /home/ubuntu/.acme.sh/acme.sh --issue --standalone -d bytescheme.com -d www.bytescheme.com -d controller.bytescheme.com -d www.controller.bytescheme.com --certpath /controlboard/conf/security/ssl/bytescheme.cer --keypath /controlboard/conf/security/ssl/bytescheme.key --capath /controlboard/conf/security/ssl/ca.cer --fullchainpath /controlboard/conf/security/ssl/fullchain.cer

sudo openssl pkcs12 -export -in /controlboard/conf/security/ssl/bytescheme.cer -inkey /controlboard/conf/security/ssl/bytescheme.key  -out /controlboard/conf/security/ssl/bytescheme.p12

sudo keytool -importkeystore -srckeystore /controlboard/conf/security/ssl/bytescheme.p12 -srcstoretype PKCS12 -destkeystore /controlboard/conf/security/ssl/keystore.jks -deststoretype JKS

sudo systemctl start controlboard.service