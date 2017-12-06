#include <ESP8266WiFi.h>
#include <WString.h>
#include <DNSServer.h>
#include <ESP8266WebServer.h>
#include <WiFiManager.h>

#define COMMAND_DELIM " "
#define MAX_ARGS_COUNT 3
#define MIN_ARGS_COUNT 2
// GPIO 12 for S4, 13 for S5, 14 for S3, 16 for S2

// Static IP
const char* host = "192.168.1.20";

WiFiClient client;

void setup() {
    Serial.begin(57600);
    pinMode(12, OUTPUT);
    pinMode(13, OUTPUT);
    pinMode(14, OUTPUT);
    pinMode(16, OUTPUT);
    WiFiManager wifiManager;
    Serial.println("Connecting...");
    wifiManager.setAPStaticIPConfig(IPAddress(10,0,1,1), IPAddress(10,0,1,1), IPAddress(255,255,255,0));
    wifiManager.autoConnect("ControlboardAP");
    Serial.println("WiFi connected");
}

void loop() {
    if (client.connect(host, 9990)) {
        Serial.println("connected");
        client.println(1);
        delay(100);
        while (client.connected() && !client.available()) {
            delay(100);
        }
        if (!client.connected()) {
            client.stop();
            return;
        }
        String line = client.readStringUntil('\n');
        Serial.println(line);
        String tokens[MAX_ARGS_COUNT];
        int count = 0;
        char* str = new char[line.length() + 1];
        line.toCharArray(str, line.length() + 1);
        str[line.length()] = 0;
        char* p  = strtok(str, COMMAND_DELIM);
        while (count < MAX_ARGS_COUNT && p != NULL) {
            tokens[count++] = String(p);
            p = strtok(NULL, COMMAND_DELIM);
        }
        delete[] str;
        if (count < MIN_ARGS_COUNT) {
            Serial.println("Invalid data");
            client.stop();
            return;
        }
        int pin = tokens[1].toInt();
        if (tokens[0].equalsIgnoreCase("GET")) {
            client.println(digitalRead(pin) == HIGH? "TRUE" : "FALSE");
            Serial.println("GET command executed");
        } else if (count > 2 && tokens[0].equalsIgnoreCase("SET")) {
            int value = tokens[2].toInt();
            digitalWrite(pin, value == 1? HIGH : LOW);
            Serial.println("SET command executed");
            client.println(digitalRead(pin) == HIGH? "TRUE" : "FALSE");
        } else {
            Serial.println("Invalid command line");
            Serial.println(line);
        }
        client.stop();
        Serial.println("Disonnected");
        delay(100);
    }
}

