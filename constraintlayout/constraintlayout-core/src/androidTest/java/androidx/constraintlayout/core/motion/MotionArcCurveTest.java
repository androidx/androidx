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

import androidx.constraintlayout.core.motion.utils.ArcCurveFit;
import androidx.constraintlayout.core.motion.utils.CurveFit;

import org.junit.Test;

public class MotionArcCurveTest {
    @Test
    public void arcTest1() {
        double[][] points = {
                {0, 0}, {1, 1}, {2, 0}
        };
        double[] time = {
                0, 5, 10
        };
        int[] mode = {
                ArcCurveFit.ARC_START_VERTICAL,
                ArcCurveFit.ARC_START_HORIZONTAL,

        };
        CurveFit spline = CurveFit.getArc(mode, time, points);
        System.out.println("");
        for (int i = 0; i < time.length; i++) {
            assertEquals(points[i][0], spline.getPos(time[i], 0), 0.001);
            assertEquals(points[i][1], spline.getPos(time[i], 1), 0.001);
        }
        assertEquals(0, spline.getSlope(time[0] + 0.01, 0), 0.001);
        assertEquals(0, spline.getSlope(time[1] - 0.01, 1), 0.001);
        assertEquals(0, spline.getSlope(time[1] + 0.01, 1), 0.001);
        double dx = spline.getSlope((time[0] + time[1]) / 2, 0);
        double dy = spline.getSlope((time[0] + time[1]) / 2, 1);
        assertEquals(1, dx / dy, 0.001);
        double x = spline.getPos((time[0] + time[1]) / 2, 0);
        double y = spline.getPos((time[0] + time[1]) / 2, 1);
        assertEquals(1 - Math.sqrt(0.5), x, 0.001);
        assertEquals(Math.sqrt(0.5), y, 0.001);
    }
}
