/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.core.motion.utils;

import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * This class translates a series of floating point values into a continuous
 * curve for use in an easing function including quantize functions
 * it is used with the "spline(0,0.3,0.3,0.5,...0.9,1)" it should start at 0 and end at one 1
 */
public class StepCurve extends Easing {
    private static final boolean DEBUG = false;
    MonotonicCurveFit mCurveFit;

    StepCurve(String configString) {
        // done this way for efficiency
        mStr = configString;
        double[] values = new double[mStr.length() / 2];
        int start = configString.indexOf('(') + 1;
        int off1 = configString.indexOf(',', start);
        int count = 0;
        while (off1 != -1) {
            String tmp = configString.substring(start, off1).trim();
            values[count++] = Double.parseDouble(tmp);
            off1 = configString.indexOf(',', start = off1 + 1);
        }
        off1 = configString.indexOf(')', start);
        String tmp = configString.substring(start, off1).trim();
        values[count++] = Double.parseDouble(tmp);

        mCurveFit = genSpline(Arrays.copyOf(values, count));
    }

    @SuppressWarnings("unused")
    private static MonotonicCurveFit genSpline(String str) {
        String wave = str;
        String[] sp = wave.split("\\s+");
        double[] values = new double[sp.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = Double.parseDouble(sp[i]);
        }
        return genSpline(values);
    }

    private static MonotonicCurveFit genSpline(double[] values) {
        int length = values.length * 3 - 2;
        int len = values.length - 1;
        double gap = 1.0 / len;
        double[][] points = new double[length][1];
        double[] time = new double[length];
        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            points[i + len][0] = v;
            time[i + len] = i * gap;
            if (i > 0) {
                points[i + len * 2][0] = v + 1;
                time[i + len * 2] = i * gap + 1;

                points[i - 1][0] = v - 1 - gap;
                time[i - 1] = i * gap + -1 - gap;
            }
        }
        if (DEBUG) {
            String t = "t ";
            String v = "v ";
            DecimalFormat df = new DecimalFormat("#.00");
            for (int i = 0; i < time.length; i++) {
                t += df.format(time[i]) + " ";
                v += df.format(points[i][0]) + " ";
            }
            System.out.println(t);
            System.out.println(v);
        }
        MonotonicCurveFit ms = new MonotonicCurveFit(time, points);
        System.out.println(" 0 " + ms.getPos(0, 0));
        System.out.println(" 1 " + ms.getPos(1, 0));
        return ms;
    }

    // @TODO: add description
    @Override
    public double getDiff(double x) {
        return mCurveFit.getSlope(x, 0);
    }

    // @TODO: add description
    @Override
    public double get(double x) {
        return mCurveFit.getPos(x, 0);
    }
}
