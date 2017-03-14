#!/bin/bash
sudo systemctl stop controlboard.service

sudo /home/pi/.acme.sh/acme.sh --issue --standalone -d video1.bytescheme.com --certpath /controlboard/conf/security/ssl/bytescheme.cer --keypath /controlboard/conf/security/ssl/bytescheme-private.key --capath /controlboard/conf/security/ssl/ca.cer --fullchainpath /controlboard/conf/security/ssl/fullchain.cer --httpport 8080

sudo openssl pkcs12 -export -in /controlboard/conf/security/ssl/bytescheme.cer -inkey /controlboard/conf/security/ssl/bytescheme-private.key  -out /controlboard/conf/security/ssl/bytescheme.p12

sudo systemctl start controlboard.service

# To generate self-signed (Optional)
keytool -genkey -alias jetty -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650

server.ssl.key-store: bytescheme.p12
server.ssl.key-store-password: mypassword
server.ssl.keyStoreType: PKCS12
server.ssl.keyAlias: jetty