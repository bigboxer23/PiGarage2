PiGarage 2.1
============

Updated from version one to use springboot to allow easy configuration of HTTPS and maven.

To add HTTPS support, create a file named application.properties and place into the src/resources directory.  Example
file contents would be something like:

```
server.port: 443
server.ssl.key-store: keystore.p12
server.ssl.key-store-password: mySecurePassword
server.ssl.keyStoreType: PKCS12
```

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

<h3>Wiring devices to the pi</h3>
<h4>Sensor for garage door:</h4>
	<a href="http://www.smarthome.com/7455/Seco-Larm-SM-226L-Garage-Door-Contacts-for-Closed-Circuits/p.aspx">Seco-Larm SM-226L</a><br>
This is wired to <i>Pin 9 (Ground)</i> and <i>Pin 13 (GPIO 27, WiringPi 2)</i>.<br><br>

<h4>Sensor for garage house door:</h4>
<a href="https://www.amazon.com/gp/product/B07F314V3Z/ref=ppx_yo_dt_b_asin_title_o02_s00?ie=UTF8&psc=1">Magnetic Door Sensor</a><br>
The motion sensor wired up to <i>pin 22 (GPIO 25, WiringPi 6)</i>, <i>pin 20 (Ground)</i><br><br>

<h4>Solid State Relay:</h4>
<a href="http://www.amazon.com/gp/product/B00E0NTPP4/ref=ox_ya_os_product_refresh_T1">Solid State Relay</a><br>
The solid state relay is wired to <i>pin 6 (Ground)</i>, <i>pin 2 (5v)</i>, and <i>pin 7 (GPIO 4, WiringPi 7)</i> <br><br>

<h4>Motion Sensor:</h4>
<a href="http://www.elecfreaks.com/wiki/index.php?title=PIR_Motion_Sensor_Module:DYP-ME003">PIR_Motion_Sensor_Module:DYP-ME003</a><br>
The motion sensor wired up to <i>pin 15 (GPIO 22, WiringPi 3)</i>, <i>pin 2 (5v)</i>, <i>pin 6 (Ground)</i><br><br>

<h4>Temperature/Humidity Sensor (DHT22):</h4>
<a href="ftp://imall.iteadstudio.com/Sensor/IM120712007/DS_IM120712007.pdf">DHT22</a><br>
The temperature/humidity sensor wired up to <i>pin 19 (GPIO 10)</i>, <i>pin 17 (3.3v)</i>, <i>pin 25 (ground)</i><br>
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
GPIO.status.pin: Pin to use for the magnetic sensor (default is WiringPi 2, pin 13)<br>
GPIO.action.pin: Pin to use for the solid state relay (default is WiringPi 7, pin 7)<br>
GPIO.motion.pin: Pin to use for the motion sensor (default is WiringPi 3, pin 15)<br>
GPIO.temp.pin: Pin to use for the temperature sensor (default is GPIO 10, pin 19)
GPIO.status.house.pin: Pin to use for the garage house door (default is WiringPi 6, pin 22)

Example `application.properties`

```server.port: 443
server.ssl.key-store: keystore.p12
server.ssl.key-store-password: mysupersecretpw
server.ssl.keyStoreType: PKCS12
GarageOpenUrl=https://myHub/S/OpenHAB/GarageOpen/100
GarageCloseUrl=ttps://myHub/S/OpenHAB/GarageClosed/100
GarageMotionUrl=https://myHub/S/OpenHAB/GarageLights/100
GarageHouseDoorUrl=https://myHub/S/OpenHAB/GarageLights/100
GarageNotificationUrl=https:/myHub/S/Notification
logbackserver=192.168.0.234:5671
```

Setup on pi:
Add the folowing to `/etc/rc.local` to launch on startup

```cd /home/pi
sudo nohup java -jar /home/pi/com/bigboxer23/garageOpener/1.0.0/garageOpener-1.0.0.jar
exit 0
```

