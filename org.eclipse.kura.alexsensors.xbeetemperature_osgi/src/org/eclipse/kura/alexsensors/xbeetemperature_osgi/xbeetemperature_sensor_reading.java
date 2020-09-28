package org.eclipse.kura.alexsensors.xbeetemperature_osgi;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class xbeetemperature_sensor_reading {
	public float temperature_c;
	public float battery_V;
	
	public void from_string(String serial_string) throws IllegalArgumentException
	{
		
		 Pattern p = Pattern.compile("(?<=values=).[\\d\\.,]+");
		 Matcher m = p.matcher(serial_string);

		 boolean found = m.find();
		 
		 if (true == found) {
			 String[] values = m.group().split(",");
			 
			 if (values.length == 2) {
				 this.temperature_c = Float.parseFloat(values[0]);
				 this.battery_V = Float.parseFloat(values[1]);						 
			 } else {
				 throw new IllegalArgumentException();
			 }
 
		 } else {
			 throw new IllegalArgumentException();
		 }
		
	}
	
}
