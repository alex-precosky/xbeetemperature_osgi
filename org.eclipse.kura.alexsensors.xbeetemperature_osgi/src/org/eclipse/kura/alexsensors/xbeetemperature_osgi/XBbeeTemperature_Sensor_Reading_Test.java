package org.eclipse.kura.alexsensors.xbeetemperature_osgi;

import static org.junit.Assert.*;
import org.eclipse.kura.alexsensors.xbeetemperature_osgi.xbeetemperature_sensor_reading;

import org.junit.Test;

public class XBbeeTemperature_Sensor_Reading_Test {

	@Test
	public void testFrom_string_happy_path() {
		String input = "sensor_data:station=alexfridge&names=temp,battVoltage&values=23.33,4.17&units=C,V";
		
		xbeetemperature_sensor_reading reading = new xbeetemperature_sensor_reading();
	
		reading.from_string(input);
		
		assertEquals(23.33, reading.temperature_c, 0.01);
		assertEquals(4.17, reading.battery_V, 0.01);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testFrom_string_weird_input() {
		String input = "this is incorrect input";
		xbeetemperature_sensor_reading reading = new xbeetemperature_sensor_reading();
		reading.from_string(input);
		
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFrom_string_missing_voltage() {
		
		// Notice there is no voltage in the list of values
		String input = "sensor_data:station=alexfridge&names=temp,battVoltage&values=23.33,&units=C,V";
		xbeetemperature_sensor_reading reading = new xbeetemperature_sensor_reading();
		reading.from_string(input);
		
	}
}
