# xbeetemperature_osgi
This is a module for Eclipse Kura 3.1 for taking XBee sensor readings and putting them on MQTT.
It's probably not for general use as-is since it's pretty specific to my application, but would be a handy 
starting point for anyone interested in using Kura as a gateway in their IoT application
where they want to put values in a MySQL or other database.

It implements the ConfigurableComponent interface, letting you set it up through the Kura web interface.

It uses the Kura CloudService to publish to the MQTT topic

## Requirements

* Eclipse Kura 3.1
* MQTT Server
* Periodic sensor values coming from a serial port


## Building

### OSGi .jar file bundle for testing

* In Eclipse, right click on the project and choose Export.  Select Plug-in Development - Deployable plug-ins and fragments.
Select the project and click Finish.  

## Deployment

The .jar file bundle of the project needs to be built first, as above.  Then, find the .dpp file in the Eclipse project structurm, 
under resources/dp.  Open it and it will update its configuration to use the newly built .jar file.  Right click on the .dpp file
and choose Quick Build. 
