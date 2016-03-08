/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

package org.hawkular.datamining.forecast.model;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hawkular.datamining.forecast.AccuracyStatistics;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.model.r.ModelData;
import org.hawkular.datamining.forecast.model.r.ModelReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class TripleExponentialSmoothingTest extends AbstractTest {

    private double ACCURACY_LOW = 0.65;
    private double ACCURACY_HIGH = 1.05;

    @Test
    public void testEmpty() {
        try {
            TimeSeriesModel model = TripleExponentialSmoothing.optimizer(12).minimizedMSE(Collections.EMPTY_LIST);
            Assert.fail();
        } catch (Throwable ex) {
            // ok
        }
    }

    @Test
    public void testMinimalPoints() throws IOException {
        int minimalPoints = 40;

        ModelData rModel = ModelReader.read("sineLowVarLong");

        TripleExponentialSmoothing.Optimizer optimizer = TripleExponentialSmoothing.optimizer(20);

        try {
            TimeSeriesModel model = optimizer.minimizedMSE(rModel.getData().subList(0, minimalPoints));
            Assert.assertNotNull(model);
        } catch (IllegalArgumentException ex) {
            Assert.fail();
        }

        try {
            TimeSeriesModel model = optimizer.minimizedMSE(rModel.getData().subList(0, minimalPoints - 1));
            Assert.fail();
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testBatchInitAndLearnStatistics() throws IOException {
        ModelData rModel = ModelReader.read("sineLowVarLong");

        TripleExponentialSmoothing.Optimizer optimizer = TripleExponentialSmoothing.optimizer(20);
        TimeSeriesModel modelInit = optimizer.minimizedMSE(rModel.getData());

        TripleExponentialSmoothing.State state =
                new TripleExponentialSmoothing.State(optimizer.result()[3], optimizer.result()[4],
                        Arrays.copyOfRange(optimizer.result(), 5, optimizer.result().length));

        TimeSeriesModel modelLearn = new TripleExponentialSmoothing(optimizer.result()[0], optimizer.result()[1],
                optimizer.result()[2], state);
        modelLearn.learn(rModel.getData());

        AccuracyStatistics batchInitStatistics = modelInit.initStatistics();
        AccuracyStatistics batchLearnStatistics = modelLearn.runStatistics();

        Assert.assertEquals(batchInitStatistics, batchLearnStatistics);
    }

    @Test
    public void testInit() throws IOException {
        ModelData rModel = ModelReader.read("sineLowVar");

        TripleExponentialSmoothing tripleExponentialSmoothing = new TripleExponentialSmoothing(20);

        AccuracyStatistics initStat = tripleExponentialSmoothing.init(rModel.getData());
        Assert.assertNotNull(initStat);
    }

    @Test
    public void testSineLowVarMedium() throws IOException {
        ModelData rModel = ModelReader.read("sineLowVarMedium");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer(20).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();
        System.out.println(seasonalStat);

        TimeSeriesModel doubleExpSmoot = DoubleExponentialSmoothing.optimizer().minimizedMSE(rModel.getData());
        AccuracyStatistics doubleExpSmootStat = doubleExpSmoot.initStatistics();

        Assert.assertTrue(seasonalStat.getMse() < doubleExpSmootStat.getMse());
        assertThat(seasonalStat.getMse()).withFailMessage("rModel: %s\nMy: %s\n%s", rModel,
                seasonalModel, seasonalStat)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testSineLowVarLong() throws IOException {
        double ACCURACY_HIGH = 1.6;

        ModelData rModel = ModelReader.read("sineLowVarLong");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer(20).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();
        System.out.println(seasonalStat);

        assertThat(seasonalStat.getMse()).withFailMessage("rModel: %s\nMy: %s\n%s", rModel, seasonalModel, seasonalStat)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testSineLowVarTrend() throws IOException {
        ModelData rModel = ModelReader.read("sineTrendLowVar");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer(20).minimizedMSE(rModel.getData());
        AccuracyStatistics initStatistics = seasonalModel.initStatistics();
        System.out.println(initStatistics);

        assertThat(initStatistics.getMse()).withFailMessage("rModel: %s\nMy: %s\n%s", rModel,
                seasonalModel, initStatistics)
                .isBetween(rModel.getMse()*ACCURACY_LOW,rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testAustourists() throws IOException {
        ModelData rModel = ModelReader.read("austourists");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer(4).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();
        System.out.println(seasonalStat);

        assertThat(seasonalStat.getMse()).withFailMessage("rModel: %s\nMy: %s\niniStat: %s", rModel,
                seasonalModel, seasonalStat)
                .isBetween(rModel.getMse()*ACCURACY_LOW, rModel.getMse()*ACCURACY_HIGH);
    }

    @Test
    public void testPrediction() throws IOException {
        ModelData rModel = ModelReader.read("austourists");

        TimeSeriesModel seasonalModel = TripleExponentialSmoothing.optimizer(4).minimizedMSE(rModel.getData());
        AccuracyStatistics seasonalStat = seasonalModel.initStatistics();
        System.out.println(seasonalStat);

        int nAhead = 15;
        List<DataPoint> forecast = seasonalModel.forecast(nAhead);
        DataPoint oneStepForecast = seasonalModel.forecast();

        Assert.assertTrue(forecast.size() == nAhead);
        Assert.assertTrue(oneStepForecast.getValue().equals(forecast.get(0).getValue()));

        double ACCURACY_LOW = 0.95;
        double ACCURACY_HIGH = 1.05;
        double[] expectedFromR = {60.02375, 37.51312, 46.33168, 49.98424, 62.01252, 39.50189, 48.32045,
                51.97301, 64.00129, 41.49066, 50.30923, 53.96178, 65.99007, 43.47944, 52.29800};
        for (int i = 0; i < nAhead; i++) {

            System.out.println(i);
            assertThat(forecast.get(i).getValue())
                    .isBetween(expectedFromR[i]*ACCURACY_LOW, expectedFromR[i]*ACCURACY_HIGH);
        }
    }
}
