PiGarage 2.1
========

Updated from version one to use springboot to allow easy configuration of HTTPS and maven.

To add HTTPS support, create a file named application.properties and place into the src/resources directory.  Example 
file contents would be something like:

server.port: 443
server.ssl.key-store: keystore.p12
server.ssl.key-store-password: mySecurePassword
server.ssl.keyStoreType: PKCS12

To generate a self signed certificate for development, the following command can be used: 
keytool -genkey -alias tomcat -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650

Good resource for setting this up: https://drissamri.be/blog/java/enable-https-in-spring-boot/

This project runs on a raspberry pi.  It utilizes GPIO pins to track status of a magnetic sensor mounted to a garage door,
and a relay used to open/close the door.  It will trigger the garage door to close after a defined amount of time being
left open.  Additionally, it runs a small webserver which has REST service urls to trigger closing/opening the door
and checking status.  Additionally there's a dyp-me003 motion sensor wired up to reset the auto close timeout if motion
detected.  A temperature and humidity sensor can be attached to allow measurements to be fetched via REST url

Ties together these two projects:<br>
<a href="http://www.instructables.com/id/Raspberry-Pi-Garage-Door-Opener/?ALLSTEPS">Magnetic Sensor</a><br>
<a href="https://www.richlynch.com/2013/07/27/pi_garage_alert_1/">Opener</a><br><br>

Wiring devices to the pi<br>
Sensor for status:<br>
	<a href="http://www.smarthome.com/7455/Seco-Larm-SM-226L-Garage-Door-Contacts-for-Closed-Circuits/p.aspx">Seco-Larm SM-226L</a><br>
This is wired to Pin 9 (Ground) and 13 (GPIO 2 by default).<br><br>

SSR:<br>
<a href="http://www.amazon.com/gp/product/B00E0NTPP4/ref=ox_ya_os_product_refresh_T1">Solid State Relay</a><br>
The solid state relay is wired to pin 6 (Ground), pin 2 (5v), and pin 7 (GPIO 7 by default) <br><br>

Motion Sensor:<br>
<a href="http://www.elecfreaks.com/wiki/index.php?title=PIR_Motion_Sensor_Module:DYP-ME003">PIR_Motion_Sensor_Module:DYP-ME003</a><br>
The motion sensor wired up to pin 15 (GPIO3), pin 2 (5v), pin 6 (Ground)<br><br>

Temperature/Humidity Sensor (DHT22):<br>
<a href="ftp://imall.iteadstudio.com/Sensor/IM120712007/DS_IM120712007.pdf">DHT22</a><br>
The temperature/humidity sensor wired up to GPIO10 (non Pi4J) (pin 10), 3.3v (pin 17), grd (pin 25)<br>
Note:  This sensor requires the adafruit dht driver be installed in a path accessible location.<br>
Example:<br>
git clone git://github.com/adafruit/Adafruit-Raspberry-Pi-Python-Code.git<br>
cd Adafruit-Raspberry-Pi-Python-Code/Adafruit_DHT_Driver<br>
sudo cp Adafruit_DHT /usr/bin<br>
<br><br>

There are multiple configurable properties that can be set at runtime:<br>
log.location: Path to the file to log information about opener status, actions, etc.<br>
status.path: URL to get status from (default is "/Status2")<br>
close.path: URL to close garage door (default is "/Close")<br>
open.path: URL to open garage door (default is "/Open")<br>
close.delay: Number of milliseconds before closing the door once it's detected open (default is 10 minutes)<br>
triggerDelay: How long to leave the switch active before turning off (how long you press the physical button) (default is 400ms)<br>
GPIO.status.pin: Pin to use for the magnetic sensor (default is GPIO 2, pin 13)<br>
GPIO.action.pin: Pin to use for the solid state relay (default is GPIO 7, pin 7)<br>
GPIO.motion.pin: Pin to use for the motion sensor (default is GPIO 3, pin 15)<br>
GPIO.temp.pin: Pin to use for the temperature sensor (default is GPIO (non Pi4J) 10, pin 19)


Setup on pi:
create a new file at /etc/init.d called garageStartup
copy the following into the file

#! /bin/bash
cd /home/pi
 java -jar ~/garageOpener-1.0.0.jar

set executable (chmod 755)