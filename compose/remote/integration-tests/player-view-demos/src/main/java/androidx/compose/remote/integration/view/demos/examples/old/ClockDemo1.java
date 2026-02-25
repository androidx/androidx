/*
 * Copyright (C) 2024 The Android Open Source Project
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
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DUP;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.LN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MAX;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.POW;
import static androidx.compose.remote.creation.Rc.FloatExpression.ROUND;
import static androidx.compose.remote.creation.Rc.FloatExpression.SIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

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
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * A collection of clock demos using procedural animations.
 */
@SuppressLint("RestrictedApiAndroidX")
public class ClockDemo1 {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private ClockDemo1() {
    }
    /**
     * Creates a simple clock demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter clock1() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START);
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

            rc.getPainter().setRadialGradient(cx, cy, rad, new int[]{0xff000000, 0x0}, null,
                    Shader.TileMode.CLAMP).commit();

            rc.drawCircle(cx, cy, rad);
            rc.getPainter().setShader(0).setColor(Color.BLACK).setStyle(
                    Paint.Style.STROKE).setStrokeWidth(62f).setStrokeCap(Paint.Cap.ROUND).commit();
            rc.drawPath(pat2);
            rc.getPainter().setColor(Color.GRAY).commit();
            float sub = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 60f, MOD, 60f, DIV);
            rc.drawTweenPath(pat2, pat2, 0, 0, sub);
            rc.getPainter().setStyle(Paint.Style.FILL).commit();
            rc.restore();
            rc.getPainter().setShader(0).commit();

            drawTicks(rc, cx, cy, rad1, rad2, a2);

            drawClock(rc, cx, cy);

            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }

    static int genPath(RemoteComposeWriter rc, float centerX, float centerY, float rad1,
            float rad2) {
        float second = rc.createFloatId();

        // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
        float sqrt2 = (float) Math.sqrt(2);
        float n = rc.floatExpression(1, 0.5f, sqrt2, rad2, rad1, DIV, 1, sqrt2, SUB, MUL, ADD, LN,
                2, LN, DIV, SUB, DIV);

        float n1 = rc.floatExpression(1, n, DIV);
        float pi = (float) Math.PI;
        int pid = rc.pathCreate(centerX, rc.floatExpression(centerY, rad1, SUB));
        rc.loop(Utils.idFromNan(second), 0f, 0.2f, 60, () -> {
            float ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB);
            float cosAng = rc.floatExpression(ang, COS);
            float sinAng = rc.floatExpression(ang, SIN);
            float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
            float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

            float polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n1, POW, DIV);
            float offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD);
            float offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD);
            rc.pathAppendLineTo(pid, offsetX, offsetY);
        });

        rc.pathAppendClose(pid);
        return pid;
    }

    static void drawTicks(RemoteComposeWriterAndroid rc, float centerX, float centerY, float rad1,
            float rad2, float sec) {
        float second = rc.createFloatId();

        float fontSize = rc.floatExpression(FLOAT_FONT_SIZE, 2, MUL);
        // rc.addDebugMessage("default font size ", fontSize);
        rc.getPainter().setColor(Color.LTGRAY).setTextSize(fontSize).commit();
        // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
        float sqrt2 = (float) Math.sqrt(2);
        float n = rc.floatExpression(1, 0.5f, sqrt2, rad2, rad1, DIV, 1, sqrt2, SUB, MUL, ADD, LN,
                2, LN, DIV, SUB, DIV);
        float n1 = rc.floatExpression(1, n, DIV);
        float pi = (float) Math.PI;
        rc.loop(Utils.idFromNan(second), 0f, 1, 60, () -> {
            float ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB);
            float angDeg = rc.floatExpression(second, 6, MUL);
            float cosAng = rc.floatExpression(ang, COS);
            float sinAng = rc.floatExpression(ang, SIN);
            float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
            float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

            float polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n1, POW, DIV);
            float offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD);
            float offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD);
            float scale = rc.floatExpression(1, sec, 60, MOD, second, SUB, ABS, SUB, 0, MAX, 0.5f,
                    ADD);
            rc.save();
            rc.rotate(angDeg, offsetX, offsetY);
            float posY = rc.floatExpression(offsetY, 6, ADD);
            rc.scale(scale, 2, offsetX, offsetY);
            rc.drawCircle(offsetX, posY, 6);
            rc.restore();
        });
        rc.getPainter().setColor(Color.WHITE).setTextSize(fontSize).commit();

        rc.loop(Utils.idFromNan(second), 0f, 5, 60, () -> {
            float ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB);
            float angDeg = rc.floatExpression(second, 6, MUL);
            float cosAng = rc.floatExpression(ang, COS);
            float sinAng = rc.floatExpression(ang, SIN);

            float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
            float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

            float polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n1, POW, DIV);
            float offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD);
            float offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD);
            rc.save();
            rc.rotate(angDeg, offsetX, offsetY);
            float posY = rc.floatExpression(offsetY, 6, ADD);
            rc.scale(0.5f, 3, offsetX, offsetY);
            rc.drawCircle(offsetX, posY, 6);
            rc.restore();
        });

        float inset = 70;
        rc.loop(Utils.idFromNan(second), 0f, 15, 60, () -> {
            float ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB);
            float cosAng = rc.floatExpression(ang, COS);
            float sinAng = rc.floatExpression(ang, SIN);
            float cos4 = rc.floatExpression(cosAng, DUP, MUL, DUP, MUL);
            float sin4 = rc.floatExpression(sinAng, DUP, MUL, DUP, MUL);

            float polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, 0.25f, POW, DIV, inset,
                    SUB);
            float offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD);
            float offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD);

            float hr = rc.floatExpression(second, 15, DIV, 3, ADD, 4, MOD, 1, ADD, 3, MUL, ROUND);
            int tid = rc.createTextFromFloat(hr, 2, 0, 0);

            rc.drawTextAnchored(tid, offsetX, offsetY, 0, 0, 0);
        });
    }

    static void drawClock(RemoteComposeWriterAndroid rc, float centerX, float centerY) {
        float minAngle = rc.floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL);
        float hrAngle = rc.floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL);
        float hourHandLength = rc.floatExpression(centerY, centerX, centerY, MIN, 0.3f, MUL, SUB);
        float minHandLength = rc.floatExpression(centerY, centerX, centerY, MIN, 0.7f, MUL, SUB);
        float hourWidth = 12f;
        float handWidth = 6f;
        // Hour
        rc.save();
        rc.getPainter().setColor(Color.GRAY).setStrokeWidth(hourWidth).setStrokeCap(
                Paint.Cap.ROUND).commit();
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
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter fancyClock2() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START);
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
            rc.getPainter().setSweepGradient(cx, cy,
                    new int[]{0xff4e6588, 0xff182947, 0xff182947, 0xff182947, 0xff4e6588,
                            0xff182947, 0xff4e6588}, null).commit();
            float secondAngle = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 360f, MOD, 11f, MUL);
            rc.drawCircle(cx, cy, rad);
            rc.rotate(secondAngle, cx, cy);
            rc.getPainter().setSweepGradient(cx, cy,
                    new int[]{0x804e6588, 0x80182947, 0x80182947, 0x80182947, 0x804e6588,
                            0x80182947, 0x804e6588}, null).commit();
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

    static void drawTicksCond(RemoteComposeWriterAndroid rc, float centerX, float centerY,
            float rad1, float rad2) {
        float second = rc.createFloatId();
        rc.getPainter().setColor(Color.LTGRAY).setTextSize(80f).commit();
        // float  n = 1 / ( 0.5 -   Log2( SQRT2 + (rad2/rad1) * (1 - SQRT2) )
        float sqrt2 = (float) Math.sqrt(2);
        float n = rc.floatExpression(1, 0.5f, sqrt2, rad2, rad1, DIV, 1, sqrt2, SUB, MUL, ADD, LN,
                2, LN, DIV, SUB, DIV);
        float n1 = rc.floatExpression(1, n, DIV);
        float pi = (float) Math.PI;
        rc.loop(Utils.idFromNan(second), 0f, 1, 60, () -> {
            float ang = rc.floatExpression(second, 2 * pi / 60, MUL, pi / 2, SUB);
            float angDeg = rc.floatExpression(second, 6, MUL);
            float cosAng = rc.floatExpression(ang, COS);
            float sinAng = rc.floatExpression(ang, SIN);
            float cos4 = rc.floatExpression(cosAng, ABS, n, POW);
            float sin4 = rc.floatExpression(sinAng, ABS, n, POW);

            float polarRadius = rc.floatExpression(rad1, cos4, sin4, ADD, ABS, n1, POW, DIV);
            float offsetX = rc.floatExpression(polarRadius, cosAng, MUL, centerX, ADD);
            float offsetY = rc.floatExpression(polarRadius, sinAng, MUL, centerY, ADD);
            rc.save();
            rc.rotate(angDeg, offsetX, offsetY);
            float posY = rc.floatExpression(offsetY, 6, ADD);
            rc.scale(0.5f, 2, offsetX, offsetY);
            rc.drawCircle(offsetX, posY, 6);

            rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f,
                    rc.floatExpression(second, 5, MOD));
            {
                rc.getPainter().setColor(Color.WHITE).commit();
                rc.drawCircle(offsetX, posY, 10);
                rc.getPainter().setColor(Color.LTGRAY).commit();
            }
            rc.endConditionalOperations();

            rc.restore();
            rc.conditionalOperations(ConditionalOperations.TYPE_EQ, 0f,
                    rc.floatExpression(second, 15, MOD));
            {
                float inset = 70;
                float txtOffsetX = rc.floatExpression(polarRadius, inset, SUB, cosAng, MUL,
                        centerX, ADD);
                float txtOffsetY = rc.floatExpression(polarRadius, inset, SUB, sinAng, MUL,
                        centerY, ADD);
                float hr = rc.floatExpression(second, 15, DIV, 3, ADD, 4, MOD, 1, ADD, 3, MUL,
                        ROUND);
                int tid = rc.createTextFromFloat(hr, 2, 0, 0);
                rc.drawTextAnchored(tid, txtOffsetX, txtOffsetY, 0, 0, 0);
            }
            rc.endConditionalOperations();
        });
    }
}
