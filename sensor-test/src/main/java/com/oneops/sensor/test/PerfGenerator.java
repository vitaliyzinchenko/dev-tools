package com.oneops.sensor.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.oneops.sensor.events.BasicEvent;
import com.oneops.sensor.events.PerfEvent;
import com.oneops.sensor.events.PerfEventPayload;

public class PerfGenerator {
	static Logger logger = Logger.getLogger(PerfGenerator.class);
	
    private String user = ActiveMQConnection.DEFAULT_USER;
    private String password = ActiveMQConnection.DEFAULT_PASSWORD;
    private String url = ActiveMQConnection.DEFAULT_BROKER_URL + "?connectionTimeout=1000";
    private String queue = "perf-in-q-";
    
    private Connection connection = null;
    private Session session = null; 
    private MessageProducer producer = null;
    private Gson gson = new Gson();
    
    private void showParameters() {
    	logger.info("Connecting to URL: " + url);
    	logger.info("Publishing a Message  to topic: " + queue);
 
    }

    public void init(long manifestId, int poolsize) throws JMSException {
		Properties properties = new Properties();				
		try {
			properties.load(this.getClass().getResourceAsStream ("/sensor-test.properties"));
		} catch (IOException e) {
			logger.error("got: "+e.getMessage());
		}

		long instanceId = manifestId % poolsize + 1;
		
		user = properties.getProperty("amq.user");
		password = properties.getProperty("amq.password");
		url = properties.getProperty("amq.url");
		queue = queue + instanceId;
		
		showParameters();
        // Create the connection.
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
        connection = connectionFactory.createConnection();
        connection.start();

        // Create the session
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queue);
        // Create the producer.
        producer = session.createProducer(destination);
        
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    }

	public void publishEvent(long ciId, 
							 long manifestId, 
							 String source,
							 String bucket,
							 String metric,
							 double value,
							 String channel) throws JMSException {
    	
		
		PerfEvent event = new PerfEvent();
		event.setCiId(ciId);
		event.setManifestId(manifestId);
		event.setSource(source);
		event.setTimestamp(Math.round(System.currentTimeMillis()/1000));
		event.setBucket(bucket);
		event.setSource(source);
		event.setChannel(channel);
		PerfEventPayload metrics = new PerfEventPayload();
		Map<String, Double> metricMap = new HashMap<String, Double>();
		metricMap.put(metric, value);
		metrics.setAvg(metricMap);
		event.setMetrics(metrics);
		publishMessage(event);	
		
		logger.info(gson.toJson(event));
    }
    
    public void publishMessage(BasicEvent event) throws JMSException {
    	ObjectMessage message = session.createObjectMessage(event);
    	message.setLongProperty("ciId", event.getCiId());
    	message.setLongProperty("manifestId", event.getManifestId());
    	message.setStringProperty("source", event.getSource());
    	producer.send(message);
    	logger.info("Published: ciId:" + event.getCiId() + "; source:" + event.getSource());
    }

    public void cleanup() {
    	logger.info("Closing AMQ connection");
    	closeConnection();
    }
    
    public void closeConnection() {
        try {
        	producer.close();
        	session.close();
        	connection.close();
        } catch (Throwable ignore) {
        }
    }

}
