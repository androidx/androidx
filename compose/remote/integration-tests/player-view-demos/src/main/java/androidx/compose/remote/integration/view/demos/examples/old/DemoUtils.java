/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.integration.view.demos.examples.old;

import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.DEG;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.RAD;
import static androidx.compose.remote.creation.Rc.FloatExpression.SIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;
import static androidx.compose.remote.creation.Rc.FloatExpression.TAN;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import androidx.compose.remote.core.operations.TextFromFloat;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.Set;

/**
 * Utility functions for procedural demos.
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoUtils {

    private DemoUtils() {
    }

    /**
     * Calculate sunrise and sunset times in local time.
     *
     * @param rc             the writer.
     * @param timezoneOffset the timezone offset.
     * @param latitude       the latitude.
     * @param longitude      the longitude.
     * @return an array containing sunrise and sunset times.
     */
    public static float @NonNull [] sunriseSunset(@NonNull RemoteComposeWriter rc,
            float timezoneOffset, float latitude, float longitude) {

        float dayOfYear; // = Rc.Time.DAY_OF_YEAR
        long epocDays = rc.integerExpression(Rc.Time.INT_EPOCH_SECOND, 86400,
                Rc.IntegerExpression.L_DIV);
        dayOfYear = rc.floatExpression(Utils.asNan((int) (0xFFFFFF & epocDays)), 365.25f, MOD);

        // Solar declination angle
        // declination = 23.45f *  Math.sin(Math.toRadians(360.0 * (284 + dayOfYear) / 365.0));
        float declination = rc.floatExpression(dayOfYear, 284, ADD, (360 / 365f), MUL, RAD, SIN,
                23.45f, MUL);
        // Convert latitude to radians
        // latRad = Math.toRadians(latitude);
        // declRad =  Math.toRadians(declination);
        float latRad = rc.floatExpression(latitude, RAD);
        float declRad = rc.floatExpression(declination, RAD);
        // Hour angle calculation
        // hourAngleArg = - Math.tan(latRad) *  Math.tan(declRad);
        float hourAngleArg = rc.floatExpression(0, latRad, TAN, declRad, TAN, MUL, SUB);

        // hourAngle = (float)Math.acos(hourAngleArg);
        float hourAngle = rc.floatExpression(hourAngleArg, Rc.FloatExpression.ACOS);

        // Convert hour angle to hours
        // hourAngleHours = (float)Math.toDegrees(hourAngle) / 15.0f;
        float hourAngleHours = rc.floatExpression(hourAngle, DEG, 15.0f, DIV);

        // Calculate sunrise and sunset times in UTC
        // solarNoon = 12.0f - (longitude / 15.0f)  // Solar noon in UTC
        float solarNoon = rc.floatExpression(12.0f, longitude, 15.0f, DIV, SUB);

        // Apply timezone offset
        //  sunriseLocal = sunriseUTC + timezoneOffset;
        //  sunsetLocal = sunsetUTC + timezoneOffset;
        float sunriseLocal = rc.floatExpression(solarNoon, hourAngleHours, SUB, timezoneOffset,
                ADD);
        float sunsetLocal = rc.floatExpression(solarNoon, hourAngleHours, ADD, timezoneOffset,
                ADD);

        return new float[]{sunriseLocal, sunsetLocal};
    }

    static class Locations {
        float mUtcOffset;
        String mName;
        String mZoneId;
        float mLatitude;
        float mLongitude;
        static Locations[] sLoc;
        private static Set<String> sZoneIds = ZoneId.getAvailableZoneIds();

        static {
            sLoc = new Locations[]{
                    loc(-10, "Hawaii", 21.3, -157.9), loc(-9, "Anchorage", 61.2, -149.9), loc(-8,
                    "San Jose", "Los_Angeles", 37.3387, -121.8853), loc(-7, "Denver", 39.7,
                    -104.9), loc(-6, "Chicago", 41.8832, -87.6324), loc(-5, "New_York", 40.7,
                    -74.0), loc(-4, "Trinidad", "America/Port_of_Spain", 10.6667, -61.5), loc(-4,
                    "Caracas", 10.5, -66.9), loc(0, "London", 51.5, 0.1), loc(+1, "Berlin", 52.5,
                    13.4), loc(+8, "Shanghai", 31.2304, 121.4737), loc(+9, "Tokyo", 35.7, 139.7),
                    loc(+10, "Sydney", -33.9, 151.2)};
        }

        static float[] getLatitudes() {
            float[] ret = new float[sLoc.length];
            for (int i = 0; i < sLoc.length; i++) {
                ret[i] = sLoc[i].mLatitude;
            }
            return ret;
        }

        static float[] getLongitudes() {
            float[] ret = new float[sLoc.length];
            for (int i = 0; i < sLoc.length; i++) {
                ret[i] = sLoc[i].mLongitude;
            }
            return ret;
        }

        static String[] getNames() {

            String[] ret = new String[sLoc.length];
            for (int i = 0; i < sLoc.length; i++) {
                ret[i] = sLoc[i].mName;
            }
            return ret;
        }

        static float[] getOffsets() {
            float[] ret = new float[sLoc.length];
            for (int i = 0; i < sLoc.length; i++) {
                ret[i] = sLoc[i].mUtcOffset + 1; // for day light for now
            }
            return ret;
        }

        Locations(int off, String name, String tzName, float lat, float lon) {
            boolean found = false;
            for (String id : sZoneIds) {
                if (id.contains(tzName)) {
                    mZoneId = id;
                    found = true;
                    ZoneId zoneId = ZoneId.of(id);
                    ZonedDateTime now = ZonedDateTime.now(zoneId);
                    mUtcOffset = now.getOffset().getTotalSeconds() / 3600f;
                    break;
                }
            }
            if (!found) {
                System.err.println("COULD NOT FIND ZONE ID FOR " + name);
            }
            mUtcOffset = off;
            mName = name;
            mLatitude = lat;
            mLongitude = lon;
        }

        private static Locations loc(int off, String name, double lat, double lon) {
            return new Locations(off, name, name, (float) lat, (float) lon);
        }

        private static Locations loc(int off, String name, String tzName, double lat, double lon) {
            return new Locations(off, name, tzName, (float) lat, (float) lon);
        }
    }

    static void drawTimeHr(RemoteComposeWriter rc, String pre, float cx, float cy, float panX,
            float panY, float hr) {
        int hrDigits = rc.createTextFromFloat(hr, 2, 0, TextFromFloat.PAD_PRE_SPACE);
        int minDigits = rc.createTextFromFloat(rc.floatExpression(hr, 1, MOD, 60, MUL), 2, 0,
                TextFromFloat.PAD_PRE_ZERO);

        int timeText = rc.textMerge(rc.textMerge(hrDigits, rc.addText(":")), minDigits);
        if (pre != null) {
            timeText = rc.textMerge(rc.addText(pre), timeText);
        }

        rc.drawTextAnchored(timeText, cx, cy, panX, panY, Rc.TextAnchorMask.MONOSPACE_MEASURE);
    }

    /**
     * Simplex Noise Generator for 2D noise.
     */
    public static class SimplexNoiseGenerator {

        // Gradient vectors for 2D
        private static final int[][] GRAD_2D =
                {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        // Simplex noise constants
        private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
        private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

        private final int[] mPerm;
        private final int[] mPermMod8;

        /**
         * Constructor that initializes the permutation arrays with a given seed.
         *
         * @param seed the random seed.
         */
        public SimplexNoiseGenerator(long seed) {
            Random random = new Random(seed);

            // Create base permutation array
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) {
                p[i] = i;
            }

            // Shuffle the array
            for (int i = 255; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = p[i];
                p[i] = p[j];
                p[j] = temp;
            }

            // Extend permutation array to avoid wrapping
            mPerm = new int[512];
            mPermMod8 = new int[512];
            for (int i = 0; i < 512; i++) {
                mPerm[i] = p[i & 255];
                mPermMod8[i] = mPerm[i] % 8;
            }
        }

        /**
         * Generate a 2D array of simplex noise values.
         *
         * @param width       Width of the output array
         * @param height      Height of the output array
         * @param scale       Scale factor for the noise (smaller = larger patterns)
         * @param octaves     Number of octaves to combine
         * @param persistence How much each octave contributes (0.0 to 1.0)
         * @param lacunarity  Frequency multiplier between octaves (typically 2.0)
         * @return a {@link Bitmap} containing the noise.
         */
        public @NonNull Bitmap generateNoise2D(int width, int height, double scale, int octaves,
                double persistence, double lacunarity) {
            //  double[][] noise = new double[height][width];
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double value = 0.0;
                    double amplitude = 1.0;
                    double frequency = scale;
                    double maxValue = 0.0;

                    // Generate fractal noise by combining multiple octaves
                    for (int i = 0; i < octaves; i++) {
                        value += noise2D(x * frequency, y * frequency) * amplitude;
                        maxValue += amplitude;
                        amplitude *= persistence;
                        frequency *= lacunarity;
                    }

                    // Normalize to [0, 1] range
                    value /= maxValue;
                    // noise[y][x] = (value + 1.0) * 0.5; // Convert from [-1,1] to [0,1]
                    bitmap.setPixel(x, y, Color.rgb((int) (value * 255), (int) (value * 255),
                            (int) (value * 255)));
                }
            }

            return bitmap;
        }

        /**
         * Generate a simple 2D array of raw simplex noise (single octave).
         *
         * @param width        Width of the output array
         * @param height       Height of the output array
         * @param scale        Scale factor for the noise
         * @param scaleValues  Scale for the noise values.
         * @param offsetValues Offset for the noise values.
         * @return a {@link Bitmap} containing the noise.
         */
        public @NonNull Bitmap generateSimpleNoise2D(int width, int height, double scale,
                float scaleValues, float offsetValues) {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double value = noise2D(x * scale, y * scale);
                    bitmap.setPixel(x, y, Color.rgb((int) (value * scaleValues + offsetValues),
                            (int) (value * scaleValues + offsetValues),
                            (int) (value * scaleValues + offsetValues)));
                }
            }

            return bitmap;
        }

        /**
         * Core 2D simplex noise function
         *
         * @param xin X coordinate
         * @param yin Y coordinate
         * @return Noise value in range approximately [-1.0, 1.0]
         */
        private double noise2D(double xin, double yin) {
            double n0, n1, n2; // Noise contributions from the three corners

            // Skew the input space to determine which simplex cell we're in
            double s = (xin + yin) * F2; // Hairy factor for 2D
            int i = fastFloor(xin + s);
            int j = fastFloor(yin + s);
            double t = (i + j) * G2;
            double xX0 = i - t; // Unskew the cell origin back to (x,y) space
            double yY0 = j - t;
            double x0 = xin - xX0; // The x,y distances from the cell origin
            double y0 = yin - yY0;

            // For the 2D case, the simplex shape is an equilateral triangle.
            // Determine which simplex we are in.
            int i1, j1; // Offsets for second (middle) corner of simplex in (i,j) coords
            if (x0 > y0) {
                i1 = 1;
                j1 = 0; // lower triangle, XY order: (0,0)->(1,0)->(1,1)
            } else {
                i1 = 0;
                j1 = 1; // upper triangle, YX order: (0,0)->(0,1)->(1,1)
            }

            // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
            // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
            // c = (3-sqrt(3))/6
            double x1 = x0 - i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
            double y1 = y0 - j1 + G2;
            double x2 = x0 - 1.0 + 2.0 * G2; // Offsets for last corner in (x,y) unskewed coords
            double y2 = y0 - 1.0 + 2.0 * G2;

            // Work out the hashed gradient indices of the three simplex corners
            int ii = i & 255;
            int jj = j & 255;
            int gi0 = mPermMod8[ii + mPerm[jj]];
            int gi1 = mPermMod8[ii + i1 + mPerm[jj + j1]];
            int gi2 = mPermMod8[ii + 1 + mPerm[jj + 1]];

            // Calculate the contribution from the three corners
            double t0 = 0.5 - x0 * x0 - y0 * y0;
            if (t0 < 0) {
                n0 = 0.0;
            } else {
                t0 *= t0;
                n0 = t0 * t0 * dot(GRAD_2D[gi0], x0, y0);
            }

            double t1 = 0.5 - x1 * x1 - y1 * y1;
            if (t1 < 0) {
                n1 = 0.0;
            } else {
                t1 *= t1;
                n1 = t1 * t1 * dot(GRAD_2D[gi1], x1, y1);
            }

            double t2 = 0.5 - x2 * x2 - y2 * y2;
            if (t2 < 0) {
                n2 = 0.0;
            } else {
                t2 *= t2;
                n2 = t2 * t2 * dot(GRAD_2D[gi2], x2, y2);
            }

            // Add contributions from each corner to get the final noise value.
            // The result is scaled to return values in the interval [-1,1].
            return 70.0 * (n0 + n1 + n2);
        }

        /** Compute dot product between gradient vector and position vector */
        private double dot(int[] g, double x, double y) {
            return g[0] * x + g[1] * y;
        }

        /** Fast floor function */
        private int fastFloor(double x) {
            int xi = (int) x;
            return x < xi ? xi - 1 : xi;
        }
    }

    static Path buildPathFromText(String str, Paint paint, float scale) {
        Path path = new Path();
        Rect mTextBounds = new Rect();
        path.reset();

        int len = str.length();
        paint.getTextBounds(str, 0, len, mTextBounds);
        paint.getTextPath(str, 0, len, 0, 0, path);
        if (scale != 1.0f) {

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            path.transform(matrix);
        }
        mTextBounds.right--;
        mTextBounds.left++;
        mTextBounds.bottom++;
        mTextBounds.top--;
        return path;
    }

    /**
     * Parses an SVG path string into a {@link Path}.
     *
     * @param pathData the SVG path data.
     * @return the parsed {@link Path}.
     */
    public static @NonNull Path parsePath(@NonNull String pathData) {
        Path path = new Path();
        float[] cords = new float[6];

        String[] commands = pathData.split("(?=[MmZzLlHhVvCcSsQqTtAa])");
        for (String command : commands) {
            if (command.isEmpty()) continue;
            char cmd = command.charAt(0);
            String[] values = command.substring(1).trim().split("[,\\s]+");
            switch (cmd) {
                case 'M':
                    path.moveTo(Float.parseFloat(values[0]), Float.parseFloat(values[1]));
                    break;
                case 'L':
                    for (int i = 0; i < values.length; i += 2) {
                        path.lineTo(Float.parseFloat(values[i]), Float.parseFloat(values[i + 1]));
                    }
                    break;
                case 'H':
                    for (String value : values) {
                        path.lineTo(Float.parseFloat(value), cords[1]);
                    }
                    break;
                case 'C':
                    for (int i = 0; i < values.length; i += 6) {
                        path.cubicTo(Float.parseFloat(values[i]), Float.parseFloat(values[i + 1]),
                                Float.parseFloat(values[i + 2]), Float.parseFloat(values[i + 3]),
                                Float.parseFloat(values[i + 4]), Float.parseFloat(values[i + 5]));
                    }
                    break;
                case 'S':
                    for (int i = 0; i < values.length; i += 4) {
                        path.cubicTo(2 * cords[0] - cords[2], 2 * cords[1] - cords[3],
                                Float.parseFloat(values[i]), Float.parseFloat(values[i + 1]),
                                Float.parseFloat(values[i + 2]), Float.parseFloat(values[i + 3]));
                    }
                    break;
                case 'Z':
                    path.close();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported command: " + cmd);
            }
            if (cmd != 'Z' && cmd != 'H') {
                cords[0] = Float.parseFloat(values[values.length - 2]);
                cords[1] = Float.parseFloat(values[values.length - 1]);
                if (cmd == 'C' || cmd == 'S') {
                    cords[2] = Float.parseFloat(values[values.length - 4]);
                    cords[3] = Float.parseFloat(values[values.length - 3]);
                }
            }
        }

        return path;
    }
}
