/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.constraintlayout.core.motion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.constraintlayout.core.motion.utils.CurveFit;
import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.motion.utils.HyperSpline;
import androidx.constraintlayout.core.motion.utils.LinearCurveFit;
import androidx.constraintlayout.core.motion.utils.Oscillator;
import androidx.constraintlayout.core.motion.utils.StopLogicEngine;

import org.junit.Test;

import java.text.DecimalFormat;

public class MotionBasicTest {

    @Test
    public void testBasic() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void unit_test_framework_working() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testHyperSpline01() throws Exception {
        double[][] points = {
                {0, 0}, {1, 1}, {2, 2}
        };
        HyperSpline spline = new HyperSpline(points);
        double value = spline.getPos(0.5, 1);
        assertEquals(1, value, 0.001);
    }

    @Test
    public void testCurveFit01() throws Exception {
        double[][] points = {
                {0, 0}, {1, 1}, {2, 0}
        };
        double[] time = {
                0, 5, 10
        };
        CurveFit spline = CurveFit.get(CurveFit.SPLINE, time, points);
        double value = spline.getPos(5, 0);
        assertEquals(1, value, 0.001);
        value = spline.getPos(7, 0);
        assertEquals(1.4, value, 0.001);
        value = spline.getPos(7, 1);
        assertEquals(0.744, value, 0.001);
    }

    @Test
    public void testCurveFit02() throws Exception {
        double[][] points = {
                {0, 0}, {1, 1}, {2, 0}
        };
        double[] time = {
                0, 5, 10
        };
        CurveFit spline = CurveFit.get(CurveFit.LINEAR, time, points);
        double value = spline.getPos(5, 0);
        assertEquals(1, value, 0.001);
        value = spline.getPos(7, 0);
        assertEquals(1.4, value, 0.001);
        value = spline.getPos(7, 1);
        assertEquals(0.6, value, 0.001);
    }

    @Test
    public void testEasing01() throws Exception {
        double value, diffValue;
        Easing easing;
        easing = Easing.getInterpolator("cubic=(1,1,0,0)");
        value = easing.get(0.5);
        assertEquals(0.5, value, 0.001);
        diffValue = easing.getDiff(0.5);
        assertEquals(1, diffValue, 0.001);
        diffValue = easing.getDiff(0.1);
        assertEquals(1, diffValue, 0.001);
        diffValue = easing.getDiff(0.9);
        assertEquals(1, diffValue, 0.001);

        easing = Easing.getInterpolator("cubic=(1,0,0,1)");
        value = easing.get(0.5);
        assertEquals(0.5, value, 0.001);

        diffValue = easing.getDiff(0.001);
        assertEquals(0, diffValue, 0.001);
        diffValue = easing.getDiff(0.9999);
        assertEquals(0, diffValue, 0.001);

        easing = Easing.getInterpolator("cubic=(0.5,1,0.5,0)");
        value = easing.get(0.5);
        assertEquals(0.5, value, 0.001);
        diffValue = easing.getDiff(0.5);
        assertEquals(0, diffValue, 0.001);
        diffValue = easing.getDiff(0.00001);
        assertEquals(2, diffValue, 0.001);
        diffValue = easing.getDiff(0.99999);
        assertEquals(2, diffValue, 0.001);

    }

    @Test
    public void testLinearCurveFit01() throws Exception {
        double value, diffValue;
        double[][] points = {
                {0, 0}, {1, 1}, {2, 0}
        };
        double[] time = {
                0, 5, 10
        };
        LinearCurveFit lcurve = new LinearCurveFit(time, points);
        value = lcurve.getPos(5, 0);
        assertEquals(1, value, 0.001);
        value = lcurve.getPos(7, 0);
        assertEquals(1.4, value, 0.001);
        value = lcurve.getPos(7, 1);
        assertEquals(0.6, value, 0.001);
    }

    @Test
    public void testOscillator01() throws Exception {
        Oscillator o = new Oscillator();
        o.setType(Oscillator.SQUARE_WAVE, null);
        o.addPoint(0, 0);
        o.addPoint(0.5, 10);
        o.addPoint(1, 0);
        o.normalize();
        assertEquals(19, countZeroCrossings(o, Oscillator.SIN_WAVE));
        assertEquals(19, countZeroCrossings(o, Oscillator.SQUARE_WAVE));
        assertEquals(19, countZeroCrossings(o, Oscillator.TRIANGLE_WAVE));
        assertEquals(19, countZeroCrossings(o, Oscillator.SAW_WAVE));
        assertEquals(19, countZeroCrossings(o, Oscillator.REVERSE_SAW_WAVE));
        assertEquals(20, countZeroCrossings(o, Oscillator.COS_WAVE));
    }

    @Test
    public void testStopLogic01() throws Exception {
        String[] results = {
                "[0.4, 0.36, 0.42, 0.578, 0.778, 0.938, 0.999, 1, 1, 1]",
                "[0.4, 0.383, 0.464, 0.64, 0.838, 0.967, 1, 1, 1, 1]",
                "[0.4, 0.405, 0.509, 0.697, 0.885, 0.986, 1, 1, 1, 1]",
                "[0.4, 0.427, 0.553, 0.75, 0.921, 0.997, 1, 1, 1, 1]",
                "[0.4, 0.449, 0.598, 0.798, 0.948, 1, 1, 1, 1, 1]",
                "[0.4, 0.472, 0.64, 0.838, 0.967, 1, 1, 1, 1, 1]",
                "[0.4, 0.494, 0.678, 0.87, 0.981, 1, 1, 1, 1, 1]",
                "[0.4, 0.516, 0.71, 0.894, 0.989, 1, 1, 1, 1, 1]",
                "[0.4, 0.538, 0.737, 0.913, 0.995, 1, 1, 1, 1, 1]",
                "[0.4, 0.56, 0.76, 0.927, 0.998, 1, 1, 1, 1, 1]"

        };

        for (int i = 0; i < 10; i++) {
            float[] f = stopGraph((i - 4) * .1f);
            assertEquals(" test " + i, results[i], arrayToString(f));
        }
    }

    private int countZeroCrossings(Oscillator o, int type) {
        int n = 1000;
        double last = o.getValue(0, 0);
        int count = 0;
        o.setType(type, null);
        for (int i = 0; i < n; i++) {

            double v = o.getValue(0.0001 + i / (double) n, 0);
            if (v * last < 0) {
                count++;
            }
            last = v;
        }
        return count;
    }

    String arrayToString(float[] f) {
        String ret = "[";
        DecimalFormat df = new DecimalFormat("###.###");
        for (int i = 0; i < f.length; i++) {
            Float aFloat = f[i];
            if (i > 0) {
                ret += ", ";
            }
            ret += df.format(f[i]);
        }
        return ret + "]";
    }

    private static float[] stopGraph(float vel) {
        StopLogicEngine breakLogic = new StopLogicEngine();
        breakLogic.config(.4f, 1f, vel, 1, 2f, 0.9f);
        float[] ret = new float[10];

        for (int i = 0; i < ret.length; i++) {
            float time = 2 * i / (float) (ret.length - 1);
            float pos = breakLogic.getInterpolation(time);
            ret[i] = pos;
        }
        assertTrue(breakLogic.isStopped());
        return ret;
    }

}
