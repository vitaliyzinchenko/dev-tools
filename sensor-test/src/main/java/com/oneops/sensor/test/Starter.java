package com.oneops.sensor.test;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.log4j.Logger;


public class Starter {
	
	static Logger logger = Logger.getLogger(Starter.class);
	static String usage = "usage: java -jar sensor-test-fat.jar ciId=370479 manifestId=224828 poolSize=2 source=tom-compute-cpu channel=my-channel bucket=1m metric=CpuIdle value=70 tries=5 sleep=3000";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println(usage);
			return;
		}
		
		Map<String,String> params = new HashMap<String,String>();
		for (String arg : args) {
			String[] parts = arg.split("=");
			params.put(parts[0], parts[1]);
		}
		
		PerfGenerator pg = new PerfGenerator();
		try {
			
			long startTime = System.currentTimeMillis();
			int tries = Integer.valueOf(params.get("tries")); 
			long sleep = Long.valueOf(params.get("sleep"));
			long ciId = Long.valueOf(params.get("ciId"));
			long manifestId = Long.valueOf(params.get("manifestId"));
			int poolSize = Integer.valueOf(params.get("poolSize"));
			String source = params.get("source");
			String bucket = params.get("bucket");
			String metric = params.get("metric");
			String channel = params.get("channel");
			double pValue = Double.valueOf(params.get("value"));
			double value = pValue;
			double stepUp = 0d;
			if (params.get("stepUp") != null) {
				stepUp = Double.valueOf(params.get("stepUp"));
			}

			pg.init(manifestId, poolSize);

			
			for (int i=0; i<tries; i++) {
				pg.publishEvent(ciId, manifestId, source, bucket, metric, value, channel);
				logger.info("iteration - " + i);
				logger.info("value - " + value);
				logger.info("time elapsed - " + Math.floor((System.currentTimeMillis() - startTime)/1000));
				//manifestId++;
				//ciId++;
				Thread.sleep(sleep);
				if (stepUp > 0) {
					value += stepUp;
				}
				if (i % 20 == 0) {
					value = pValue;
				}
			}
			pg.cleanup();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	
	}

}
