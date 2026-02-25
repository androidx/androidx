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

import static androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC;
import static androidx.compose.remote.core.RemoteContext.FLOAT_FONT_SIZE;
import static androidx.compose.remote.creation.Rc.FloatExpression.ABS;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.COS;
import static androidx.compose.remote.creation.Rc.FloatExpression.DEG;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.DUP;
import static androidx.compose.remote.creation.Rc.FloatExpression.LN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MAX;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.POW;
import static androidx.compose.remote.creation.Rc.FloatExpression.RAD;
import static androidx.compose.remote.creation.Rc.FloatExpression.ROUND;
import static androidx.compose.remote.creation.Rc.FloatExpression.SIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;
import static androidx.compose.remote.creation.Rc.FloatExpression.TAN;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * A collection of clock demos using procedural animations and time zones.
 */
@SuppressLint("RestrictedApiAndroidX")
public class ClockDemo2 {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private ClockDemo2() {
    }
    /**
     * Creates a clock demo with time zones.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter clock2() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        float utcOff = rc.addFloatArray(Locations.getOffsets());
        float lat = rc.addFloatArray(Locations.getLatitudes());
        float lon = rc.addFloatArray(Locations.getLongitudes());
        rc.root(
                () -> {
                    rc.startBox(
                            new RecordingModifier().fillMaxSize(),
                            BoxLayout.START,
                            BoxLayout.TOP);
                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 2, DIV);
                    float cy = rc.floatExpression(h, 2, DIV);
                    float rad = rc.floatExpression(w, h, MIN);
                    float clipRad1 = rc.floatExpression(rad, 2f, DIV);
                    float rad1 = rc.floatExpression(rad, 2, DIV);
                    float rad2 = rc.floatExpression(rad, 5, DIV);

                    int pat2 = genPath(rc, cx, cy, clipRad1, rad2);

                    rc.addClipPath(pat2);
                    rc.save();
                    float a2 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 360f, MOD);

                    rc.getPainter()
                            .setRadialGradient(
                                    cx,
                                    cy,
                                    rad,
                                    new int[]{0xff555500, 0xFF999999},
                                    null,
                                    Shader.TileMode.CLAMP)
                            .commit();

                    rc.drawCircle(cx, cy, rad);
                    rc.getPainter()
                            .setShader(0)
                            .setColor(Color.BLACK)
                            .setStyle(Paint.Style.STROKE)
                            .setStrokeWidth(62f)
                            .setStrokeCap(Paint.Cap.ROUND)
                            .commit();

                    rc.getPainter().setStyle(Paint.Style.FILL).commit();
                    rc.restore();
                    rc.getPainter().setShader(0).commit();

                    drawTimeZones(rc, pat2, w, h, cx, cy, Locations.getNames(), utcOff, lat, lon);
                    drawTicks(rc, cx, cy, rad1, rad2, a2);
                    drawClock(rc, cx, cy);
                    rc.endCanvas();
                    rc.endBox();
                });
        return rc;
    }

    static int genPath(
            RemoteComposeWriter rc, float centerX, float centerY, float rad1, float rad2) {
        float second = rc.createFloatId();

        // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
        float sqrt2 = (float) Math.sqrt(2);
        float n =
                rc.floatExpression(
                        1,
                        0.5f,
                        sqrt2,
                        rad2,
                        rad1,
                        DIV,
                        1,
                        sqrt2,
                        SUB,
                        MUL,
                        ADD,
                        LN,
                        2,
                        LN,
                        DIV,
                        SUB,
                        DIV);

        float n1 = rc.floatExpression(1, n, DIV);
        float pi = (float) Math.PI;
        int pid =
                rc.pathCreate(
                        centerX, rc.floatExpression(centerY, rad1, SUB));
        rc.loop(
                Utils.idFromNan(second),
                0f,
                0.2f,
                60,
                () -> {
                    float ang =
                            rc.floatExpression(
                                    second,
                                    2 * pi / 60,
                                    MUL,
                                    pi / 2,
                                    SUB);
//                    float angDeg = rc.floatExpression(second, 6, MUL);
                    float cosAng = rc.floatExpression(ang, COS);
                    float sinAng = rc.floatExpression(ang, SIN);
                    float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
                    float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

                    float polarRadius =
                            rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n1, POW, DIV);
                    float offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD);
                    float offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD);
                    rc.pathAppendLineTo(pid, offsetX, offsetY);
                });

        rc.pathAppendClose(pid);
        return pid;
    }

    static void drawTicks(
            RemoteComposeWriterAndroid rc,
            float centerX,
            float centerY,
            float rad1,
            float rad2,
            float sec) {
        float second = rc.createFloatId();

        float fontSize = rc.floatExpression(FLOAT_FONT_SIZE, 2, MUL);
        // rc.addDebugMessage("default font size ", fontSize);
        rc.getPainter().setColor(Color.LTGRAY).setTextSize(fontSize).commit();
        // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
        float sqrt2 = (float) Math.sqrt(2);
        float n =
                rc.floatExpression(
                        1,
                        0.5f,
                        sqrt2,
                        rad2,
                        rad1,
                        DIV,
                        1,
                        sqrt2,
                        SUB,
                        MUL,
                        ADD,
                        LN,
                        2,
                        LN,
                        DIV,
                        SUB,
                        DIV);
        float n1 = rc.floatExpression(1, n, DIV);
        float pi = (float) Math.PI;
        rc.loop(
                Utils.idFromNan(second),
                0f,
                1,
                60,
                () -> {
                    float ang =
                            rc.floatExpression(
                                    second,
                                    2 * pi / 60,
                                    MUL,
                                    pi / 2,
                                    SUB);
                    float angDeg = rc.floatExpression(second, 6, MUL);
                    float cosAng = rc.floatExpression(ang, COS);
                    float sinAng = rc.floatExpression(ang, SIN);
                    float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
                    float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

                    float polarRadius =
                            rc.floatExpression(
                                    rad1,
                                    cos4,
                                    sin4,
                                    ADD,
                                    ABS,
                                    n1,
                                    POW,
                                    DIV);
                    float offsetX =
                            rc.floatExpression(
                                    polarRadius,
                                    cosAng,
                                    MUL,
                                    centerX,
                                    ADD);
                    float offsetY =
                            rc.floatExpression(
                                    polarRadius,
                                    sinAng,
                                    MUL,
                                    centerY,
                                    ADD);
                    float scale =
                            rc.floatExpression(
                                    1,
                                    sec,
                                    60,
                                    MOD,
                                    second,
                                    SUB,
                                    ABS,
                                    SUB,
                                    0,
                                    MAX,
                                    0.5f,
                                    ADD);
                    rc.save();
                    rc.rotate(angDeg, offsetX, offsetY);
                    float posY = rc.floatExpression(offsetY, 6, ADD);
                    rc.scale(scale, 2, offsetX, offsetY);
                    rc.drawCircle(offsetX, posY, 6);
                    rc.restore();
                });
        rc.getPainter().setColor(Color.WHITE).setTextSize(fontSize).commit();

        rc.loop(
                Utils.idFromNan(second),
                0f,
                5,
                60,
                () -> {
                    float ang =
                            rc.floatExpression(
                                    second,
                                    2 * pi / 60,
                                    MUL,
                                    pi / 2,
                                    SUB);
                    float angDeg = rc.floatExpression(second, 6, MUL);
                    float cosAng = rc.floatExpression(ang, COS);
                    float sinAng = rc.floatExpression(ang, SIN);

                    float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
                    float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

                    float polarRadius =
                            rc.floatExpression(
                                    rad1,
                                    cos4,
                                    sin4,
                                    ADD,
                                    ABS,
                                    n1,
                                    POW,
                                    DIV);
                    float offsetX =
                            rc.floatExpression(
                                    polarRadius,
                                    cosAng,
                                    MUL,
                                    centerX,
                                    ADD);
                    float offsetY =
                            rc.floatExpression(
                                    polarRadius,
                                    sinAng,
                                    MUL,
                                    centerY,
                                    ADD);
                    rc.save();
                    rc.rotate(angDeg, offsetX, offsetY);
                    float posY = rc.floatExpression(offsetY, 6, ADD);
                    rc.scale(0.5f, 3, offsetX, offsetY);
                    rc.drawCircle(offsetX, posY, 6);
                    rc.restore();
                });

        float inset = 70;
        rc.loop(
                Utils.idFromNan(second),
                0f,
                15,
                60,
                () -> {
                    float ang =
                            rc.floatExpression(
                                    second,
                                    2 * pi / 60,
                                    MUL,
                                    pi / 2,
                                    SUB);
                    float cosAng = rc.floatExpression(ang, COS);
                    float sinAng = rc.floatExpression(ang, SIN);
                    float cos4 =
                            rc.floatExpression(
                                    cosAng,
                                    DUP,
                                    MUL,
                                    DUP,
                                    MUL);
                    float sin4 =
                            rc.floatExpression(
                                    sinAng,
                                    DUP,
                                    MUL,
                                    DUP,
                                    MUL);

                    float polarRadius =
                            rc.floatExpression(
                                    rad1,
                                    cos4,
                                    sin4,
                                    ADD,
                                    0.25f,
                                    POW,
                                    DIV,
                                    inset,
                                    SUB);
                    float offsetX =
                            rc.floatExpression(
                                    polarRadius,
                                    cosAng,
                                    MUL,
                                    centerX,
                                    ADD);
                    float offsetY =
                            rc.floatExpression(
                                    polarRadius,
                                    sinAng,
                                    MUL,
                                    centerY,
                                    ADD);

                    float hr =
                            rc.floatExpression(
                                    second,
                                    15,
                                    DIV,
                                    3,
                                    ADD,
                                    4,
                                    MOD,
                                    1,
                                    ADD,
                                    3,
                                    MUL,
                                    ROUND);
                    int tid = rc.createTextFromFloat(hr, 2, 0, 0);

                    rc.drawTextAnchored(tid, offsetX, offsetY, 0, 0, 0);
                });
    }

    static void drawClock(RemoteComposeWriterAndroid rc, float centerX, float centerY) {
        float secondAngle =
                rc.floatExpression(FLOAT_CONTINUOUS_SEC, 60f, MOD, 6f, MUL);
        float minAngle =
                rc.floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL);
        float hrAngle =
                rc.floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL);
        float hourHandLength =
                rc.floatExpression(centerY, centerX, centerY, MIN, 0.3f, MUL, SUB);
        float minHandLength =
                rc.floatExpression(centerY, centerX, centerY, MIN, 0.7f, MUL, SUB);
        float hourWidth = 12f;
        float handWidth = 6f;
        // Hour
        rc.save();
        rc.getPainter()
                .setColor(Color.GRAY)
                .setStrokeWidth(hourWidth)
                .setStrokeCap(Paint.Cap.ROUND)
                .commit();
        rc.drawCircle(centerX, centerY, hourWidth);
        rc.rotate(hrAngle, centerX, centerY);
        rc.drawLine(centerX, centerY, centerX, hourHandLength);
        rc.restore();

        // min
        rc.save();
        rc.getPainter().setColor(Color.WHITE).setStrokeWidth(handWidth).commit();
        rc.drawCircle(centerX, centerY, handWidth);
        rc.rotate(minAngle, centerX, centerY);
        rc.drawLine(centerX, centerY, centerX, minHandLength);
        rc.restore();
        // Center
        rc.getPainter().setColor(Color.WHITE).commit();
        rc.drawCircle(centerX, centerY, 2);
        // sec
        rc.save();
        rc.rotate(secondAngle, centerX, centerY);
        rc.getPainter().setColor(Color.RED).setStrokeWidth(4f).commit();
        rc.drawLine(centerX, centerY, centerX, minHandLength);
        rc.restore();
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
     * Creates a fancy clock demo with sweep gradients and ticks.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter fancyClock2() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        rc.root(
                () -> {
                    rc.startBox(
                            new RecordingModifier().fillMaxSize(),
                            BoxLayout.START,
                            BoxLayout.TOP);
                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 2, DIV);
                    float cy = rc.floatExpression(h, 2, DIV);
                    float rad = rc.floatExpression(w, h, MIN);
                    float clipRad1 = rc.floatExpression(rad, 2f, DIV);
                    float rad1 = rc.floatExpression(rad, 2, DIV);
                    float rad2 = rc.floatExpression(rad, 5, DIV);

                    int pat2 = genPath(rc, cx, cy, clipRad1, rad2);

                    rc.addClipPath(pat2);
                    rc.save();
                    float a2 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 360f, MOD);
                    rc.rotate(a2, cx, cy);
                    rc.getPainter()
                            .setSweepGradient(
                                    cx,
                                    cy,
                                    new int[]{
                                            0xff4e6588,
                                            0xff182947,
                                            0xff182947,
                                            0xff182947,
                                            0xff4e6588,
                                            0xff182947,
                                            0xff4e6588
                                    },
                                    null)
                            .commit();
                    float secondAngle =
                            rc.floatExpression(
                                    FLOAT_CONTINUOUS_SEC,
                                    360f,
                                    MOD,
                                    11f,
                                    MUL);
                    rc.drawCircle(cx, cy, rad);
                    rc.rotate(secondAngle, cx, cy);
                    rc.getPainter()
                            .setSweepGradient(
                                    cx,
                                    cy,
                                    new int[]{
                                            0x804e6588,
                                            0x80182947,
                                            0x80182947,
                                            0x80182947,
                                            0x804e6588,
                                            0x80182947,
                                            0x804e6588
                                    },
                                    null)
                            .commit();
                    rc.drawCircle(cx, cy, rad);
                    rc.restore();
                    rc.getPainter().setShader(0).commit();

                    drawTicksCond(rc, cx, cy, rad1, rad2);

                    drawClock(rc, cx, cy);

                    rc.endCanvas();
                    rc.endBox();
                });
        return rc;
    }

    static void drawTicksCond(
            RemoteComposeWriterAndroid rc, float centerX, float centerY, float rad1, float rad2) {
        float second = rc.createFloatId();
        rc.getPainter().setColor(Color.LTGRAY).setTextSize(80f).commit();
        // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
        float sqrt2 = (float) Math.sqrt(2);
        float n =
                rc.floatExpression(
                        1,
                        0.5f,
                        sqrt2,
                        rad2,
                        rad1,
                        DIV,
                        1,
                        sqrt2,
                        SUB,
                        MUL,
                        ADD,
                        LN,
                        2,
                        LN,
                        DIV,
                        SUB,
                        DIV);
        float n1 = rc.floatExpression(1, n, DIV);
        float pi = (float) Math.PI;
        rc.loop(
                Utils.idFromNan(second),
                0f,
                1,
                60,
                () -> {
                    float ang =
                            rc.floatExpression(
                                    second,
                                    2 * pi / 60,
                                    MUL,
                                    pi / 2,
                                    SUB);
                    float angDeg = rc.floatExpression(second, 6, MUL);
                    float cosAng = rc.floatExpression(ang, COS);
                    float sinAng = rc.floatExpression(ang, SIN);
                    float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
                    float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

                    float polarRadius =
                            rc.floatExpression(
                                    rad1,
                                    cos4,
                                    sin4,
                                    ADD,
                                    ABS,
                                    n1,
                                    POW,
                                    DIV);
                    float offsetX =
                            rc.floatExpression(
                                    polarRadius,
                                    cosAng,
                                    MUL,
                                    centerX,
                                    ADD);
                    float offsetY =
                            rc.floatExpression(
                                    polarRadius,
                                    sinAng,
                                    MUL,
                                    centerY,
                                    ADD);
                    rc.save();
                    rc.rotate(angDeg, offsetX, offsetY);
                    float posY = rc.floatExpression(offsetY, 6, ADD);
                    rc.scale(0.5f, 2, offsetX, offsetY);
                    rc.drawCircle(offsetX, posY, 6);

                    rc.conditionalOperations(
                            ConditionalOperations.TYPE_EQ, 0f, rc.floatExpression(second, 5, MOD));
                    {
                        rc.getPainter().setColor(Color.WHITE).commit();
                        rc.drawCircle(offsetX, posY, 10);
                        rc.getPainter().setColor(Color.LTGRAY).commit();
                    }
                    rc.endConditionalOperations();

                    rc.restore();
                    rc.conditionalOperations(
                            ConditionalOperations.TYPE_EQ, 0f,
                            rc.floatExpression(second, 15, MOD));
                    {
                        float inset = 70;
                        float txtOffsetX =
                                rc.floatExpression(
                                        polarRadius,
                                        inset,
                                        SUB,
                                        cosAng,
                                        MUL,
                                        centerX,
                                        ADD);
                        float txtOffsetY =
                                rc.floatExpression(
                                        polarRadius,
                                        inset,
                                        SUB,
                                        sinAng,
                                        MUL,
                                        centerY,
                                        ADD);
                        float hr =
                                rc.floatExpression(
                                        second,
                                        15,
                                        DIV,
                                        3,
                                        ADD,
                                        4,
                                        MOD,
                                        1,
                                        ADD,
                                        3,
                                        MUL,
                                        ROUND);
                        int tid = rc.createTextFromFloat(hr, 2, 0, 0);
                        rc.drawTextAnchored(tid, txtOffsetX, txtOffsetY, 0, 0, 0);
                    }
                    rc.endConditionalOperations();
                });
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
            sLoc =
                    new Locations[]{
                            loc(-10, "Hawaii", 21.3, -157.9),
                            loc(-9, "Anchorage", 61.2, -149.9),
                            loc(-8, "Los_Angeles", 37.8, -122.4),
                            loc(-7, "Denver", 39.7, -104.9),
                            loc(-6, "Mexico", 19.4, -99.1),
                            loc(-5, "New_York", 40.7, -74.0),
                            loc(-4, "Caracas", 10.5, -66.9),
                            loc(-3, "Buenos_Aires", -34.6, -58.4),
                            loc(-2, "Georgia", -54.3, -36.5),
                            loc(-1, "Azores", 37.7, -25.7),
                            loc(0, "London", 51.5, 0.1),
                            loc(+1, "Berlin", 52.5, 13.4),
                            loc(+2, "Cairo", 30.0, 31.2),
                            loc(+3, "Riyadh", 24.7, 46.7),
                            loc(+4, "Dubai", 25.2, 55.3),
                            loc(+6, "Dhaka", 23.8, 90.4),
                            loc(+7, "Bangkok", 13.8, 100.5),
                            loc(+8, "Shanghai", 31.2304, 121.4737),
                            loc(+9, "Tokyo", 35.7, 139.7),
                            loc(+10, "Sydney", -33.9, 151.2),
                            loc(+12, "Auckland", -36.8, 174.7),
                            loc(+13, "Tonga", -21.1, -175.2),
                            loc(+14, "Line", 1.9, -157.4)
                    };
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

        Locations(int off, String name, float lat, float lon) {
            boolean found = false;
            for (String id : sZoneIds) {
                if (id.contains(name)) {
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
            return new Locations(off, name, (float) lat, (float) lon);
        }
    }

    /**
     * Calculates sunrise and sunset times.
     *
     * @param rc             the writer.
     * @param timezoneOffset the timezone offset.
     * @param latitude       the latitude.
     * @param longitude      the longitude.
     * @return an array containing sunrise and sunset times.
     */
    public static float @NonNull [] sunriseSunset(
            @NonNull RemoteComposeWriter rc,
            float timezoneOffset,
            float latitude, float longitude) {

        float dayOfYear = Rc.Time.DAY_OF_YEAR;
        // Solar declination angle
        // declination = 23.45f *  Math.sin(Math.toRadians(360.0 * (284 + dayOfYear) / 365.0));
        float declination =
                rc.floatExpression(dayOfYear, 284, ADD, (360 / 365f), MUL, RAD, SIN, 23.45f, MUL);
        // Convert latitude to radians
        // latRad = Math.toRadians(latitude);
        // declRad =  Math.toRadians(declination);
        float latRad = rc.floatExpression(latitude, RAD);
        float declRad = rc.floatExpression(declination, RAD);
        // Hour angle calculation
        // hourAngleArg = - Math.tan(latRad) *  Math.tan(declRad);
        float hourAngleArg = rc.floatExpression(0, latRad, TAN, declRad, TAN, MUL, SUB);

        // Check for polar day/night
        //  if (hourAngleArg < -1 || hourAngleArg > 1) {
        //     if (latitude * declination > 0) {
        //       return new float[]{0,0}; // polar day
        //     } else {
        //       return new float[]{24,24}; // polar night
        //     }
        //  }
        // todo add check for polar day/night

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
        float sunriseLocal =
                rc.floatExpression(solarNoon, hourAngleHours, SUB, timezoneOffset, ADD);
        float sunsetLocal = rc.floatExpression(solarNoon, hourAngleHours, ADD, timezoneOffset,
                ADD);
        return new float[]{sunriseLocal, sunsetLocal};
    }
    @SuppressWarnings("unused")
    private static float drawTimeZones(
            RemoteComposeWriterAndroid rc,
            int path,
            float w,
            float h,
            float cx,
            float cy,
            String[] list,
            float utcOff,
            float lat,
            float lon) {
        float rad = rc.floatExpression(w, h, MIN, 2, DIV);

        float border = rc.floatExpression(rad, 0.61f, MUL);

        float strList = rc.addStringList(list);
        float step = 400f;

        rc.addTouch(
                0,
                Float.NaN,
                list.length * step,
                RemoteComposeWriter.STOP_NOTCHES_EVEN,
                0,
                0,
                new float[]{list.length},
                rc.easing(3f, 5f, 60f),
                RemoteContext.FLOAT_TOUCH_POS_Y);

        // ============= LOOP ===============

        float gmtRad = rc.floatExpression(rad, 0.23f, MUL);
        rc.save();
//        float show = rc.floatExpression(touch, step, DIV, list.length,
//                MOD);

        int[] colors =
                new int[]{0xFF9999FF, 0xFF9999FF, 0xFF993300, 0xFF000044, 0xFF000044, 0xFF993300};
        float[] sunPos = new float[]{0, 0.20f, 0.40f, 0.60f, 0.80f, 1.0f};
        rc.getPainter().setTextSize(45f).commit();
        int showNgmts = 8;
        for (int i = 0; i < showNgmts; i++) {
            float x =
                    rc.floatExpression(
                            cx, (float) (2 * Math.PI * i / showNgmts), SIN, border, MUL, ADD);
            float y =
                    rc.floatExpression(
                            cy, (float) (2 * Math.PI * i / showNgmts), COS, border, MUL, ADD);
            float count = i;

            float offsetCalc = rc.floatExpression(utcOff, count, Rc.FloatExpression.A_DEREF);
            float[] day0 =
                    sunriseSunset(
                            rc,
                            offsetCalc,
                            rc.floatExpression(lat, count, Rc.FloatExpression.A_DEREF),
                            rc.floatExpression(lon, count, Rc.FloatExpression.A_DEREF));

            rc.getPainter().setSweepGradient(x, y, colors, sunPos).commit();

            int cityName = rc.textLookup(strList, count);
            drawTimeZone(rc, cityName, offsetCalc, x, y, gmtRad, day0[0], day0[1]);
        }

        rc.restore();

        rc.getPainter().setShader(0).commit();

        return 0;
    }

    private static void drawTimeZone(
            RemoteComposeWriterAndroid rc,
            int city,
            float utcOffset,
            float cx,
            float py,
            float rad,
            float sunrise,
            float sunset) {

        float outerEdge = 0.2f;
        float textSize = rc.floatExpression(outerEdge, rad, MUL);
        rc.getPainter().setTextSize(textSize).commit();
        rc.save();

        float hr =
                rc.floatExpression(
                        utcOffset,
                        Rc.Time.TIME_IN_HR,
                        Rc.Time.OFFSET_TO_UTC,
                        3600,
                        DIV,
                        SUB,
                        ADD,
                        12,
                        MOD);
        int hrId = rc.createTextFromFloat(hr, 3, 0, 0);
        float min = rc.floatExpression(Rc.Time.TIME_IN_MIN, 60, MOD);
        int minId = rc.createTextFromFloat(min, 2, 0, Rc.TextFromFloat.PAD_PRE_ZERO);
        int clock = rc.textMerge(hrId, rc.addText(":"));
        clock = rc.textMerge(clock, minId);

        float sunsetPos = rc.floatExpression(sunset, 24, DIV);
        float sunrisePos = rc.floatExpression(sunrise, 24, DIV);

        rc.save();
        rc.rotate(-90, cx, py);
        rc.getPainter()
                .setSweepGradient(
                        cx,
                        py,
                        new int[]{
                                0xFF000055, 0xFF000055, 0xFF7799EE, 0xFF7799EE, 0xFF000055,
                                0xFF000055
                        },
                        new float[]{0f, sunrisePos, sunrisePos, sunsetPos, sunsetPos, 1f})
                .commit();
        rc.drawCircle(cx, py, rad);
        rc.restore();
        rc.getPainter().setShader(0).setColor(Color.DKGRAY).commit();
        rc.drawCircle(cx, py, rc.floatExpression(rad, (1 - outerEdge), MUL));
        rc.getPainter().setShader(0).setColor(Color.LTGRAY).commit();
        rc.drawTextAnchored(clock, cx, py, 0, -3f, 0);
        rc.drawTextAnchored(city, cx, py, 0, 0, Rc.TextAnchorMask.MEASURE_EVERY_TIME);
        int hrSunrise = rc.createTextFromFloat(sunrise, 2, 0, 0);
        int minSunrise =
                rc.createTextFromFloat(rc.floatExpression(sunrise, 1, MOD, 60, MUL), 2, 0, 0);
        int hrSunset = rc.createTextFromFloat(sunset, 2, 0, 0);
        int minSunset =
                rc.createTextFromFloat(rc.floatExpression(sunset, 1, MOD, 60, MUL), 2, 0, 0);

        int sunriseText = rc.textMerge(rc.textMerge(hrSunrise, rc.addText(":")), minSunrise);
        int sunsetText = rc.textMerge(rc.textMerge(hrSunset, rc.addText(":")), minSunset);
        rc.drawTextAnchored(sunriseText, cx, py, 0, +3f, 0);
        rc.drawTextAnchored(sunsetText, cx, py, 0, +6, 0);
        rc.restore();
        float top = rc.floatExpression(py, rad, SUB);
        rc.save();
        for (int i = 0; i < 24; i += 2) {
            int timeHr = i == 0 ? 24 : i;
            rc.getPainter().setColor(0xFFAABB33).commit();

            rc.conditionalOperations(Rc.Condition.GT, (float) i, sunrise);
            rc.getPainter().setColor(0xFF775533).commit();
            rc.endConditionalOperations();
            rc.conditionalOperations(Rc.Condition.GT, (float) i, sunset);
            rc.getPainter().setColor(0xFFAABB33).commit();
            rc.endConditionalOperations();
            rc.drawTextAnchored(timeHr + "", cx, top, 0, +1.3f, 0);
            rc.rotate(30, cx, py);
        }
        rc.restore();
        rc.save();
        rc.getPainter().setColor(0x77FF4500).commit();
        float angle = rc.floatExpression(hr, min, 1 / 60f, MUL, ADD, 360 / 24f, MUL);
        rc.rotate(angle, cx, py);
        rc.translate(0, textSize);
        rc.scale(0.5f, 1, cx, top);

        rc.drawCircle(cx, top, rc.floatExpression(textSize, 2, DIV));
        rc.restore();
        rc.getPainter().setColor(0xFF000000).commit();
    }
}
