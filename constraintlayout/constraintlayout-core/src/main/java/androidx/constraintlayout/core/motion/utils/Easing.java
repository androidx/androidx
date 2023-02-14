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

import java.util.Arrays;

/**
 * Provide the engine for cubic spline easing
 *
 *
 */
public class Easing {
    static Easing sDefault = new Easing();
    String mStr = "identity";
    private static final String STANDARD = "cubic(0.4, 0.0, 0.2, 1)";
    private static final String ACCELERATE = "cubic(0.4, 0.05, 0.8, 0.7)";
    private static final String DECELERATE = "cubic(0.0, 0.0, 0.2, 0.95)";
    private static final String LINEAR = "cubic(1, 1, 0, 0)";
    private static final String ANTICIPATE = "cubic(0.36, 0, 0.66, -0.56)";
    private static final String OVERSHOOT = "cubic(0.34, 1.56, 0.64, 1)";

    private static final String DECELERATE_NAME = "decelerate";
    private static final String ACCELERATE_NAME = "accelerate";
    private static final String STANDARD_NAME = "standard";
    private static final String LINEAR_NAME = "linear";
    private static final String ANTICIPATE_NAME = "anticipate";
    private static final String OVERSHOOT_NAME = "overshoot";

    public static String[] NAMED_EASING =
            {STANDARD_NAME, ACCELERATE_NAME, DECELERATE_NAME, LINEAR_NAME};

    // @TODO: add description
    public static Easing getInterpolator(String configString) {
        if (configString == null) {
            return null;
        }
        if (configString.startsWith("cubic")) {
            return new CubicEasing(configString);
        } else if (configString.startsWith("spline")) {
            return new StepCurve(configString);
        } else if (configString.startsWith("Schlick")) {
            return new Schlick(configString);
        } else {
            switch (configString) {
                case STANDARD_NAME:
                    return new CubicEasing(STANDARD);
                case ACCELERATE_NAME:
                    return new CubicEasing(ACCELERATE);
                case DECELERATE_NAME:
                    return new CubicEasing(DECELERATE);
                case LINEAR_NAME:
                    return new CubicEasing(LINEAR);
                case ANTICIPATE_NAME:
                    return new CubicEasing(ANTICIPATE);
                case OVERSHOOT_NAME:
                    return new CubicEasing(OVERSHOOT);
                default:
                    System.err.println("transitionEasing syntax error syntax:"
                            + "transitionEasing=\"cubic(1.0,0.5,0.0,0.6)\" or "
                            + Arrays.toString(NAMED_EASING));
            }

        }
        return sDefault;
    }

    // @TODO: add description
    public double get(double x) {
        return x;
    }

    // @TODO: add description
    @Override
    public String toString() {
        return mStr;
    }

    // @TODO: add description
    public double getDiff(double x) {
        return 1;
    }

    static class CubicEasing extends Easing {

        private static double sError = 0.01;
        private static double sDError = 0.0001;
        double mX1, mY1, mX2, mY2;

        CubicEasing(String configString) {
            // done this way for efficiency
            mStr = configString;
            int start = configString.indexOf('(');
            int off1 = configString.indexOf(',', start);
            mX1 = Double.parseDouble(configString.substring(start + 1, off1).trim());
            int off2 = configString.indexOf(',', off1 + 1);
            mY1 = Double.parseDouble(configString.substring(off1 + 1, off2).trim());
            int off3 = configString.indexOf(',', off2 + 1);
            mX2 = Double.parseDouble(configString.substring(off2 + 1, off3).trim());
            int end = configString.indexOf(')', off3 + 1);
            mY2 = Double.parseDouble(configString.substring(off3 + 1, end).trim());
        }

        CubicEasing(double x1, double y1, double x2, double y2) {
            setup(x1, y1, x2, y2);
        }

        void setup(double x1, double y1, double x2, double y2) {
            this.mX1 = x1;
            this.mY1 = y1;
            this.mX2 = x2;
            this.mY2 = y2;
        }

        private double getX(double t) {
            double t1 = 1 - t;
            // no need for because start at 0,0 double f0 = (1 - t) * (1 - t) * (1 - t);
            double f1 = 3 * t1 * t1 * t;
            double f2 = 3 * t1 * t * t;
            double f3 = t * t * t;
            return mX1 * f1 + mX2 * f2 + f3;
        }

        private double getY(double t) {
            double t1 = 1 - t;
            // no need for because start at 0,0 double f0 = (1 - t) * (1 - t) * (1 - t);
            double f1 = 3 * t1 * t1 * t;
            double f2 = 3 * t1 * t * t;
            double f3 = t * t * t;
            return mY1 * f1 + mY2 * f2 + f3;
        }

        @SuppressWarnings("unused")
        private double getDiffX(double t) {
            double t1 = 1 - t;
            return 3 * t1 * t1 * mX1 + 6 * t1 * t * (mX2 - mX1) + 3 * t * t * (1 - mX2);
        }

        @SuppressWarnings("unused")
        private double getDiffY(double t) {
            double t1 = 1 - t;
            return 3 * t1 * t1 * mY1 + 6 * t1 * t * (mY2 - mY1) + 3 * t * t * (1 - mY2);
        }

        /**
         * binary search for the region
         * and linear interpolate the answer
         */
        @Override
        public double getDiff(double x) {
            double t = 0.5;
            double range = 0.5;
            while (range > sDError) {
                double tx = getX(t);
                range *= 0.5;
                if (tx < x) {
                    t += range;
                } else {
                    t -= range;
                }
            }

            double x1 = getX(t - range);
            double x2 = getX(t + range);
            double y1 = getY(t - range);
            double y2 = getY(t + range);

            return (y2 - y1) / (x2 - x1);
        }

        /**
         * binary search for the region
         * and linear interpolate the answer
         */
        @Override
        public double get(double x) {
            if (x <= 0.0) {
                return 0;
            }
            if (x >= 1.0) {
                return 1.0;
            }
            double t = 0.5;
            double range = 0.5;
            while (range > sError) {
                double tx = getX(t);
                range *= 0.5;
                if (tx < x) {
                    t += range;
                } else {
                    t -= range;
                }
            }

            double x1 = getX(t - range);
            double x2 = getX(t + range);
            double y1 = getY(t - range);
            double y2 = getY(t + range);

            return (y2 - y1) * (x - x1) / (x2 - x1) + y1;
        }
    }
}
