package org.eclipse.kura.alexsensors.xbeetemperature_osgi;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.kura.configuration.ConfigurableComponent;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.HashMap;
import org.eclipse.kura.comm.CommURI;
import org.eclipse.kura.comm.CommConnection;
import java.util.concurrent.Future;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.io.ConnectionFactory;

import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.alexsensors.xbeetemperature_osgi.xbeetemperature_sensor_reading;


public class XBeeTemperatureReader implements ConfigurableComponent, CloudClientListener {

	private static final Logger s_logger = LoggerFactory.getLogger(XBeeTemperatureReader.class);
	private static final String APP_ID = "org.eclipse.kura.alexsensors.XBeeTemperatureReader";
	private Map<String, Object> m_properties;
	private ScheduledThreadPoolExecutor m_worker;
	private Future<?> m_handle;
	private InputStream m_commIs;
	
	private ConnectionFactory m_connectionFactory;
	private CommConnection m_commConnection;
	
	private CloudService m_cloudService;
	private CloudClient m_cloudClient;
		
	
	private static final String SERIAL_DEVICE_PROP_NAME= "serial.device";
	private static final String SERIAL_BAUDRATE_PROP_NAME= "serial.baudrate";
	private static final String SERIAL_DATA_BITS_PROP_NAME= "serial.data-bits";
	private static final String SERIAL_PARITY_PROP_NAME= "serial.parity";
	private static final String SERIAL_STOP_BITS_PROP_NAME= "serial.stop-bits";
	private static final String MQTT_TOPIC_TEMPERATURE_PROP_NAME="mqtt.topic.temperature";
	private static final String MQTT_TOPIC_BAT_VOLTAGE_PROP_NAME="mqtt.topic.batVoltage";

	private static final String MQTT_APP_ID = "alexsensors";
	
	  public void setConnectionFactory(ConnectionFactory connectionFactory) {
		    this.m_connectionFactory = connectionFactory;
		  }

	  public void unsetConnectionFactory(ConnectionFactory connectionFactory) {
	    this.m_connectionFactory = null;
	  }

	  public void setCloudService(CloudService cloudService) {
		    this.m_cloudService = cloudService;
		    s_logger.info("Cloud service set!");
		  }

	  public void unsetCloudService(ConnectionFactory connectionFactory) {
	    this.m_cloudService = null;
	  }
	  
	
    protected void activate(ComponentContext componentContext,  Map<String,Object> properties) {
    	s_logger.info("Starting " + APP_ID);
    	
    	m_worker = new ScheduledThreadPoolExecutor(1);
    	m_properties = new HashMap<String, Object>();

    	try {
    		s_logger.info("Getting CloudClient for {}...", APP_ID);
    		if( this.m_cloudService != null )
    		{
    			m_cloudClient = m_cloudService.newCloudClient(MQTT_APP_ID);	
 	   	    	this.m_cloudClient.addCloudClientListener(this);
 	   	    	doUpdate(properties);
    		}
			
		} catch (Exception e) {
            s_logger.error("Error during component activation", e);
            throw new ComponentException(e);
        }
    	

    	
        s_logger.info("Bundle " + APP_ID + " has started!");
    }
    
    protected void deactivate(ComponentContext componentContext) {
    	m_handle.cancel(true);
    	m_worker.shutdownNow();
    	closePort();
    	
    	m_cloudClient.release();
    	
        s_logger.info("Bundle " + APP_ID + " has stopped!");
    }
    
    public void updated(Map<String, Object> properties) {
    	s_logger.info("Updating XBeeTemperatureReader...");
    	        
        // log the new properties
        if(properties != null && !properties.isEmpty()) {
            Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = it.next();
                s_logger.info("New property - " + entry.getKey() + " = " +
                entry.getValue() + " of type " + entry.getValue().getClass().toString());
            }
        }
        
        doUpdate(properties);
        s_logger.info("Updating XBeeTemperatureReader...Done.");
    }
    
    
    // Called by the updated and activate events
    private void doUpdate(Map<String, Object> properties) {

        try {
            for (String s : properties.keySet()) {
              s_logger.info("Update - "+s+": "+properties.get(s));
            }

            // cancel a current worker handle if one if active
            if (m_handle != null) {
              m_handle.cancel(true);
            }

            //close the serial port so it can be reconfigured
            closePort();

            //store the properties
        	m_properties = new HashMap<String, Object>();
            m_properties.clear();
            m_properties.putAll(properties);

            //reopen the port with the new configuration
            openPort();

            //start the worker thread
            m_handle = m_worker.submit(new Runnable() {
              @Override
              public void run() {
                doSerial();
              }
            });

          } catch (Throwable t) {
              s_logger.error("Unexpected Throwable", t);
          	}    	
    }
    
    
    private void openPort() {
        String port = (String) m_properties.get(SERIAL_DEVICE_PROP_NAME);

        if (port == null) {
          s_logger.info("Port name not configured");
          return;
        }    	
        
        int baudRate = Integer.valueOf((String) m_properties.get(SERIAL_BAUDRATE_PROP_NAME));
        int dataBits = Integer.valueOf((String) m_properties.get(SERIAL_DATA_BITS_PROP_NAME));
        int stopBits = Integer.valueOf((String) m_properties.get(SERIAL_STOP_BITS_PROP_NAME));
        String sParity = (String) m_properties.get(SERIAL_PARITY_PROP_NAME);
        int parity = CommURI.PARITY_NONE;

        if (sParity.equals("none")) {
          parity = CommURI.PARITY_NONE;
        } else if (sParity.equals("odd")) {
            parity = CommURI.PARITY_ODD;
        } else if (sParity.equals("even")) {
            parity = CommURI.PARITY_EVEN;
        }
        
        String uri = new CommURI.Builder(port)
        	    .withBaudRate(baudRate)
        	    .withDataBits(dataBits)
        	    .withStopBits(stopBits)
        	    .withParity(parity)
        	    .withTimeout(1000)
        	    .build().toString();
        
        try {
            m_commConnection = (CommConnection) m_connectionFactory.createConnection(uri, 1, false);
            m_commIs = m_commConnection.openInputStream();
            s_logger.info(port+" open");
          } catch (IOException e) {
            s_logger.error("Failed to open port " + port, e);
            closePort();
          }        
        
    }
    
    
    private void doSerial() {
        if (m_commIs != null) {
          try {
            int c = -1;
            StringBuilder sb = new StringBuilder();
            while (m_commIs != null) {
              if (m_commIs.available() != 0) {
                c = m_commIs.read();
              } else {
                try {
                  Thread.sleep(100);
                  continue;
                } catch (InterruptedException e) {
                    return;
                }
              }

            // on reception of CR, publish the received sentence
            if (c==13) {
              s_logger.info("Serial line: " + sb.toString());
              sb.append("\r\n");
              String dataRead = sb.toString();
              
              // do something here with the received message
              processSensorMessage(dataRead);
             
              
              //reset the buffer
              sb = new StringBuilder();
            } else if (c!=10) {
              sb.append((char) c);
            }
          } // while m_commIs != null

          } catch (IOException e) {
              s_logger.error("Cannot read port", e);
          } finally {
              try {
                m_commIs.close();
              } catch (IOException e) {
                s_logger.error("Cannot close buffered reader", e);
              }
          }
        } // if m_commIs != null
        s_logger.info("doSerial() is exiting for some reason");
    } // doSerial()
    
    
    private void processSensorMessage(String dataRead) {
		xbeetemperature_sensor_reading reading = new xbeetemperature_sensor_reading();
		reading.from_string(dataRead);
		
		s_logger.info("Sensor reading: Temperature: " + reading.temperature_c + " voltage: " + reading.battery_V);
		doPublish(reading);
		
	}

	private void closePort()
    {
        if (m_commIs != null) {
            try {
              s_logger.info("Closing port input stream...");
              m_commIs.close();
              s_logger.info("Closed port input stream");
            } catch (IOException e) {
                s_logger.error("Cannot close port input stream", e);
            }
            m_commIs = null;
          }


          if (m_commConnection != null) {
            try {
              s_logger.info("Closing port...");
              m_commConnection.close();
              s_logger.info("Closed port");
            } catch (IOException e) {
                s_logger.error("Cannot close port", e);
            }
            m_commConnection = null;
          }   	
    }
	
	
	private void doPublish(xbeetemperature_sensor_reading reading)
	{
		String tempStr = Float.toString(reading.temperature_c);
		String temptopic = (String) this.m_properties.get(MQTT_TOPIC_TEMPERATURE_PROP_NAME);

		String batVoltageStr = Float.toString(reading.battery_V);
		String batTopic = (String) this.m_properties.get(MQTT_TOPIC_BAT_VOLTAGE_PROP_NAME);		
		
		try {
			byte[] tempBytes = tempStr.getBytes();
			byte[] voltageBytes = batVoltageStr.getBytes();
		
			m_cloudClient.publish(temptopic, tempBytes, 0, false, 1);
			m_cloudClient.publish(batTopic, voltageBytes, 0, false, 1);
		}
		catch(Exception e)
		{
			s_logger.error("Exception converting reading to bytes", e);
		}
		
	
	}
	
	
	  @Override
	    public  void onConnectionEstablished() {
	        s_logger.info("Connection established");
	  }
	
	  
	   @Override
	    public  void onConnectionLost() {
	        s_logger.warn("Connection lost!");
	    }
	   
	   @Override
	    public  void onControlMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
	        s_logger.info("Control message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
	    }

	    @Override
	    public  void onMessageArrived(String deviceId, String appTopic, KuraPayload msg, int qos, boolean retain) {
	        s_logger.info("Message arrived on assetId: {} and semantic topic: {}", deviceId, appTopic);
	    }

	    @Override
	    public  void onMessagePublished(int messageId, String appTopic) {
	        s_logger.info("Published message with ID: {} on application topic: {}", messageId, appTopic);
	    }

	    @Override
	    public  void onMessageConfirmed(int messageId, String appTopic) {
	        s_logger.info("Confirmed message with ID: {} on application topic: {}", messageId, appTopic);
	    }
	    
}
