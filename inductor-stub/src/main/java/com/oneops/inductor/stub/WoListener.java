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

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.oneops.cms.cm.ops.domain.OpsActionState;
import com.oneops.cms.domain.CmsWorkOrderSimpleBase;
import com.oneops.cms.simple.domain.CmsActionOrderSimple;
import com.oneops.cms.simple.domain.CmsCISimple;
import com.oneops.cms.simple.domain.CmsRfcCISimple;
import com.oneops.cms.simple.domain.CmsWorkOrderSimple;

public class WoListener implements MessageListener {
    private MessageProducer producer;

    public MessageProducer getProducer() {
        return producer;
    }

    public void setProducer(MessageProducer producer) {
        this.producer = producer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    private String status;
    private long sleepTime;
    final private Gson gson = new Gson();
    private Session session = null;

    private static final Logger LOG = LoggerFactory.getLogger(WoListener.class);

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg instanceof TextMessage) {
                //LOG.info("got message: " + ((TextMessage)msg).getText());
                String corelationId = msg.getJMSCorrelationID();
                String type = msg.getStringProperty("type");

                CmsWorkOrderSimpleBase wo;
                if (type.equals("opsprocedure")) {
                    wo = gson.fromJson(((TextMessage) msg).getText(), CmsActionOrderSimple.class);
                    LOG.info("got message: actionId = " + ((CmsActionOrderSimple) wo).getActionId());
                } else {
                    wo = gson.fromJson(((TextMessage) msg).getText(), CmsWorkOrderSimple.class);
                    LOG.info("got message: rfcId = " + ((CmsWorkOrderSimple) wo).rfcCi.getRfcId());
                }


                //Assuming we forked msg process hew ACK the MSG
                //msg.acknowledge();
                /*
		    	if (wo.getServices() != null) {
		    		System.out.println("Services for this wo = " + gson.toJson(wo.getServices()));
		    	}
		    	*/

                Thread.sleep(this.sleepTime);

                TextMessage responseMsg = session.createTextMessage();//(gson.toJson(wo));
                responseMsg.setStringProperty("type", type);
                if (type.equals("opsprocedure")) {
                    processOpsProcedure(wo, responseMsg);
                } else {
                    processDeployment(wo, responseMsg);
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
        CmsActionOrderSimple wo = (CmsActionOrderSimple) wob;
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
        CmsWorkOrderSimple wo = (CmsWorkOrderSimple) wob;
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
        ci.getAttrProps().putAll(rfc.getCiAttrProps());
        for (Map.Entry<String, String> attr : rfc.getCiAttributes().entrySet()) {
                ci.addCiAttribute(attr.getKey(), attr.getValue());
        }
        return ci;
    }

    private CmsCISimple buildCi(CmsCISimple inputCi) {
        CmsCISimple ci = new CmsCISimple();
        ci.setCiId(inputCi.getCiId());
        ci.setCiClassName(inputCi.getCiClassName());
        ci.getAttrProps().putAll(inputCi.getAttrProps());
        for (Map.Entry<String, String> attr : inputCi.getCiAttributes().entrySet()) {
                ci.addCiAttribute(attr.getKey(), attr.getValue());
        }
        return ci;
    }

}
