/*******************************************************************************
 *
 * Copyright 2015 Walmart, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.oneops.inductor.stub;

import java.io.InputStream;
import java.security.KeyStore;

import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InductorStub {

    private ActiveMQConnection connection;
    //private MessageProducer producer;
    //private Session session;
    //private Queue requests;
    //private Queue responses;
    private static final Logger LOG = LoggerFactory.getLogger(InductorStub.class);

    //private static final String requestQueueName = "us-east-1a.controller.workorders";
    private static final String responseQueueName = "controller.response";
    //final private Gson gson = new Gson();

    private String url = "failover:(tcp://kloopzmq:61616?keepAlive=true)?initialReconnectDelay=1000";
    //private String url = "failover:(ssl://localhost:61617?keepAlive=true)?initialReconnectDelay=1000&startupMaxReconnectAttempts=2";

    //private long sleepTime;
    //private String status = "complete";

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java -jar inductor-stub-fat.jar queue-name queue-password wo-result(complete/failed) sleep-time-msec threads");
            System.out.println("example: java -jar inductor-stub-fat.jar public.packer.providers.aws-ec2.ec2.us-east-1a.controller.workorders us-east-1a complete 60000 100");
            return;
        }

        InductorStub is = new InductorStub();
        is.run(args[0], args[1], args[2], Long.parseLong(args[3]), Integer.parseInt(args[4]));
        LOG.info("out of main");
    }

    public void run(String requestQueueName, String queuePass, String status, long sleepTime, int threads) throws Exception {
        //this.sleepTime = sleepTime;
        //this.status = status;

        LOG.info("Listening on " + requestQueueName);

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(url);

        factory.setUserName("superuser");
        String pass = System.getenv("KLOOPZ_AMQ_PASS");
        if (pass == null) {
            pass = "ilijailibu";
        }
        factory.setPassword(pass);

        connection = (ActiveMQConnection) factory.createConnection();
        for (int i = 1; i <= threads; i++) {
            Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
            Queue requests = session.createQueue(requestQueueName);

            Queue responses = session.createQueue(responseQueueName);

            MessageConsumer consumer = session.createConsumer(requests);
            WoListener wol = new WoListener();
            wol.setSession(session);
            wol.setSleepTime(sleepTime);
            wol.setStatus(status);
            wol.setProducer(session.createProducer(responses));
            consumer.setMessageListener(wol);
            //producer = session.createProducer(responses);
            LOG.debug("Thread " + i + " is waiting for messages...");
        }
        connection.start();

    }


    protected TrustManager[] createTrustManager() throws Exception {
        TrustManager[] trustStoreManagers = null;
        KeyStore trustedCertStore = KeyStore.getInstance("jks");

        InputStream tsStream = getClass().getClassLoader().getResourceAsStream("client.ts");

        trustedCertStore.load(tsStream, null);
        TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        tmf.init(trustedCertStore);
        trustStoreManagers = tmf.getTrustManagers();
        return trustStoreManagers;
    }	
    
    /*
    @Override
	public void onMessage(Message msg) {
		try {
	    	if (msg instanceof TextMessage) { 
				//LOG.info("got message: " + ((TextMessage)msg).getText());
		    	String corelationId = msg.getJMSCorrelationID();
                String type = msg.getStringProperty("type");

                CmsWorkOrderSimpleBase wo;
                if(type.equals("opsprocedure")) {
                    wo = gson.fromJson(((TextMessage)msg).getText(), CmsActionOrderSimple.class);
                    LOG.info("got message: actionId = " + ((CmsActionOrderSimple)wo).getActionId());
                } else {
                    wo = gson.fromJson(((TextMessage)msg).getText(), CmsWorkOrderSimple.class);
                    LOG.info("got message: rfcId = " + ((CmsWorkOrderSimple)wo).rfcCi.getRfcId());
                }
                
		    	
		    	Thread.sleep(this.sleepTime);
		    	
		    	TextMessage responseMsg = session.createTextMessage();//(gson.toJson(wo));
		    	responseMsg.setStringProperty("type", type);
                if(type.equals("opsprocedure")) {
                    processOpsProcedure(wo,responseMsg);
                } else {
                    processDeployment(wo,responseMsg);
                }

		    	responseMsg.setText(gson.toJson(wo));
		    	responseMsg.setJMSCorrelationID(corelationId);
		    	producer.send(responseMsg);
		    	session.commit();
	    	}
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    private void processOpsProcedure(CmsWorkOrderSimpleBase wob, TextMessage responseMsg) throws JMSException {
        CmsActionOrderSimple wo = (CmsActionOrderSimple)wob;
        if (status.equalsIgnoreCase("complete")) {
            wo.setActionState(OpsActionState.complete);
            CmsCISimple resultCi = buildCi(wo.getCi());
            if (resultCi.getCiAttributes().containsKey("max_instances")) {
            	resultCi.getCiAttributes().put("max_instances", "1500");
            }
            wo.setResultCi(resultCi);
            responseMsg.setStringProperty("task_result_code", "200");
        } else {
            wo.setActionState(OpsActionState.failed);
            responseMsg.setStringProperty("task_result_code", "404");
        }
        responseMsg.setStringProperty("type", "opsprocedure");
    }

    private void processDeployment(CmsWorkOrderSimpleBase wob, TextMessage responseMsg) throws JMSException {
        CmsWorkOrderSimple wo = (CmsWorkOrderSimple)wob;
        if (status.equalsIgnoreCase("complete")) {
            CmsCISimple resultCi = buildCi(wo.getRfcCi());
            wo.setResultCi(resultCi);
            wo.setDpmtRecordState("complete");
            responseMsg.setStringProperty("task_result_code", "200");
        } else {
            wo.setDpmtRecordState("failed");
            responseMsg.setStringProperty("task_result_code", "404");
            wo.setComments("Failed by inductor stub");
        }
        responseMsg.setStringProperty("type", "deploybom");
    }

	private CmsCISimple buildCi(CmsRfcCISimple rfc) {
		CmsCISimple ci = new CmsCISimple();
		ci.setCiId(rfc.getCiId());
		ci.setCiClassName(rfc.getCiClassName());
		for (Map.Entry<String, String> attr : rfc.getCiAttributes().entrySet()) {
			ci.addCiAttribute(attr.getKey(), attr.getValue());
		}	
		return ci;
	}

	private CmsCISimple buildCi(CmsCISimple inputCi) {
		CmsCISimple ci = new CmsCISimple();
		ci.setCiId(inputCi.getCiId());
		ci.setCiClassName(inputCi.getCiClassName());
		for (Map.Entry<String, String> attr : inputCi.getCiAttributes().entrySet()) {
			ci.addCiAttribute(attr.getKey(), attr.getValue());
		}	
		return ci;
	}
	*/
}
