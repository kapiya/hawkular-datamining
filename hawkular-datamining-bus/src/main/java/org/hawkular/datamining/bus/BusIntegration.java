/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.datamining.bus;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.jms.JMSException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.datamining.bus.listener.MetricDataListener;

import org.jboss.logging.Logger;

/**
 * @author Pavol Loffay
 */
public class BusIntegration {

    private static final Logger LOG = Logger.getLogger(BusIntegration.class);

    @Official
    @Produces
    @Singleton
    public MetricDataListener getMetricDataListener() {
        LOG.debug("Initializing bus");

        MetricDataListener metricDataListener = null;
        try {
            ConnectionContextFactory factory = new ConnectionContextFactory(Configuration.BROKER_URL);

            Endpoint ENDPOINT = new Endpoint(Endpoint.Type.TOPIC, Configuration.TOPIC_METRIC_DATA);
            ConsumerConnectionContext context = factory.createConsumerConnectionContext(ENDPOINT);

            MessageProcessor processor = new MessageProcessor();
            metricDataListener = new MetricDataListener();
            processor.listen(context, metricDataListener);
        } catch (JMSException ex)  {
            ex.printStackTrace();
        }

        return metricDataListener;
    }
}