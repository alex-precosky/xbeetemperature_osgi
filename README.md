# xbeetemperature\_osgi

This is a module for Eclipse Kura 4.1.1 for taking XBee sensor readings and putting them on MQTT.
It's probably not for general use as-is since it's pretty specific to my application, but would be a handy 
starting point for anyone interested in using Kura as a gateway in their IoT application
where they want to put values in a MySQL or other database.

It is built with maven.

It implements the ConfigurableComponent interface, letting you set it up through the Kura web interface.

It uses the Kura CloudService to publish to the MQTT topic

## Eclipse Kura
Kura is a Java framework for IoT gateways. https://www.eclipse.org/kura/index.php

## Requirements
* Eclipse Kura 4.1+ (probably also works with Kura 3.1)
- Eclipse Kura user workspace
- Maven
- MQTT Broker - one provided by Kura is fine. The topics listened to are hard-coded
* Periodic sensor values coming from a serial port

# Setup
## Kura User Workspace
This must be present for building. Download it from here:
https://www.eclipse.org/kura/downloads.php

And put it in ~/Downloads/user_workspace_archive_4.1.1

# Building

Run:

```
cd org.eclipse.kura.alexsensors.xbeetemperaturesensor_osgi
mvn clean initialize
mvn package
```

The build artifacts of interest are an OSGi .jar file bundle for testing, and a
.dp deployment package.

# Deployment
See:
https://eclipse.github.io/kura/dev/deploying-bundles.html

Especially section *Making Deployment Permanent*.

# Todo
- Code cleanup (whitespace, spelling... the basics)
