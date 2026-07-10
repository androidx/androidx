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

import static androidx.compose.remote.core.RemoteContext.FLOAT_ACCELERATION_X;
import static androidx.compose.remote.core.RemoteContext.FLOAT_ACCELERATION_Y;
import static androidx.compose.remote.core.RemoteContext.FLOAT_ACCELERATION_Z;
import static androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC;
import static androidx.compose.remote.core.RemoteContext.FLOAT_GYRO_ROT_X;
import static androidx.compose.remote.core.RemoteContext.FLOAT_GYRO_ROT_Y;
import static androidx.compose.remote.core.RemoteContext.FLOAT_GYRO_ROT_Z;
import static androidx.compose.remote.core.RemoteContext.FLOAT_LIGHT;
import static androidx.compose.remote.core.RemoteContext.FLOAT_MAGNETIC_X;
import static androidx.compose.remote.core.RemoteContext.FLOAT_MAGNETIC_Y;
import static androidx.compose.remote.core.RemoteContext.FLOAT_MAGNETIC_Z;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.DEG;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.ATAN2;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for device sensors (accelerometer, gyroscope, magnetometer, light).
 */
@SuppressLint("RestrictedApiAndroidX")
public class SensorDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private SensorDemo() {
    }

    /**
     * Demo showing accelerometer sensor data.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter accSensor1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            rcDoc.getPainter().setColor(Color.BLUE).commit();
            float w = rcDoc.addComponentWidthValue();
            float h = rcDoc.addComponentHeightValue();
            float cx = rcDoc.floatExpression(w, 0.5f, MUL);
            float cy = rcDoc.floatExpression(h, 0.5f, MUL);
            float accX = rcDoc.floatExpression(FLOAT_ACCELERATION_X, -9.8f * 2, DIV, w, MUL, w,
                    0.5f, MUL, ADD);
            float accY = rcDoc.floatExpression(FLOAT_ACCELERATION_Y, 9.8f * 2, DIV, w, MUL, w,
                    0.5f, MUL, ADD);
            float accZ = rcDoc.floatExpression(FLOAT_ACCELERATION_Z, 9.8f * 2, DIV, w, MUL, w,
                    0.5f, MUL, ADD);

            rcDoc.drawRect(0, 0, w, h);

            rcDoc.getPainter().setColor(Color.CYAN).setStrokeWidth(40).setStrokeCap(
                    Paint.Cap.ROUND).commit();
            rcDoc.drawLine(cx, cy, accX, accY);
            rcDoc.getPainter().setColor(Color.WHITE).commit();
            rcDoc.drawCircle(cx, cy, 20);
            rcDoc.getPainter().setColor(Color.RED).commit();
            rcDoc.drawCircle(accX, cy, 20);
            rcDoc.getPainter().setColor(Color.GREEN).commit();
            rcDoc.drawCircle(cx, accY, 20);
            rcDoc.getPainter().setColor(Color.YELLOW).commit();
            rcDoc.drawCircle(accZ, accZ, 20);

            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    /**
     * Demo showing gyroscope sensor data.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter gyroSensor1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            rcDoc.getPainter().setColor(Color.BLUE).commit();
            float w = rcDoc.addComponentWidthValue();
            float h = rcDoc.addComponentHeightValue();
            float cx = rcDoc.floatExpression(w, 0.5f, MUL);
            float cy = rcDoc.floatExpression(h, 0.5f, MUL);
            float gyroX = rcDoc.floatExpression(FLOAT_GYRO_ROT_X, 100, MUL);
            float gyroY = rcDoc.floatExpression(FLOAT_GYRO_ROT_Y, 100, MUL);
            float gyroZ = rcDoc.floatExpression(FLOAT_GYRO_ROT_Z, 100, MUL);

            rcDoc.drawRect(0, 0, w, h);

            rcDoc.getPainter().setStrokeWidth(40).setStrokeCap(Paint.Cap.ROUND).commit();
            rcDoc.save();
            rcDoc.getPainter().setColor(Color.WHITE).commit();
            rcDoc.rotate(gyroZ, cx, cy);
            rcDoc.drawLine(cx, cy, cx, h);
            rcDoc.restore();
            rcDoc.save();
            rcDoc.getPainter().setColor(Color.GREEN).commit();
            rcDoc.rotate(gyroY, cx, cy);
            rcDoc.drawLine(cx, cy, cx, h);
            rcDoc.restore();

            rcDoc.save();
            rcDoc.getPainter().setColor(Color.RED).commit();
            rcDoc.rotate(gyroX, cx, cy);
            rcDoc.drawLine(cx, cy, cx, h);
            rcDoc.restore();

            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    /**
     * Demo showing magnetometer sensor data.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter magSensor1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            rcDoc.getPainter().setColor(Color.BLUE).commit();
            float w = rcDoc.addComponentWidthValue();
            float h = rcDoc.addComponentHeightValue();
            float cx = rcDoc.floatExpression(w, 0.5f, MUL);
            float cy = rcDoc.floatExpression(h, 0.5f, MUL);
            float accX = rcDoc.floatExpression(FLOAT_MAGNETIC_X, -0.02f, MUL, w, MUL, w, 0.5f, MUL,
                    ADD);
            float accY = rcDoc.floatExpression(FLOAT_MAGNETIC_Y, 0.02f, MUL, w, MUL, w, 0.5f, MUL,
                    ADD);
            float accZ = rcDoc.floatExpression(FLOAT_MAGNETIC_Z, 0.02f, MUL, w, MUL, w, 0.5f, MUL,
                    ADD);

            rcDoc.drawRect(0, 0, w, h);

            rcDoc.getPainter().setColor(Color.CYAN).setStrokeWidth(40).setStrokeCap(
                    Paint.Cap.ROUND).commit();
            rcDoc.drawLine(cx, cy, accX, accY);
            rcDoc.getPainter().setColor(Color.WHITE).commit();
            rcDoc.drawCircle(cx, cy, 20);
            rcDoc.getPainter().setColor(Color.RED).commit();
            rcDoc.drawCircle(accX, cy, 20);
            rcDoc.getPainter().setColor(Color.GREEN).commit();
            rcDoc.drawCircle(cx, accY, 20);
            rcDoc.getPainter().setColor(Color.YELLOW).commit();
            rcDoc.drawCircle(accZ, accZ, 20);

            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    /**
     * Demo showing light sensor data with a spring animation.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter lightSensor1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock",
                sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            rcDoc.getPainter().setColor(Color.BLUE).commit();
            float w = rcDoc.addComponentWidthValue();
            float h = rcDoc.addComponentHeightValue();
            float cx = rcDoc.floatExpression(w, 0.5f, MUL);
            float cy = rcDoc.floatExpression(h, 0.5f, MUL);
            float lightBar = rcDoc.floatExpression(new float[]{FLOAT_LIGHT},
                    rcDoc.spring(3f, 1f, 0.01f, 0));

            rcDoc.drawRoundRect(0, 0, w, h, 100, 100);
            rcDoc.getPainter().setColor(Color.GRAY).commit();
            float arc = rcDoc.floatExpression(lightBar, 6, MUL);
            rcDoc.drawSector(0, 0, w, h, -180, arc);

            rcDoc.getPainter().setColor(Color.BLACK).setStrokeWidth(40).setStyle(
                    Paint.Style.FILL).setStrokeCap(Paint.Cap.ROUND).commit();
            drawLineAt(rcDoc, 0, cx, cy, 40);
            rcDoc.getPainter().setColor(Color.WHITE).commit();

            drawLineAt(rcDoc, 30, cx, cy, 40);

            rcDoc.getPainter().setColor(Color.RED).commit();

            drawLineAt(rcDoc, lightBar, cx, cy, 50);

            rcDoc.getPainter().setColor(Color.BLUE).commit();
            rcDoc.drawCircle(cx, cy, 100f);

            rcDoc.getPainter().setColor(Color.CYAN).setStyle(Paint.Style.FILL).setTextSize(
                    64).commit();
            int lightNumber = rcDoc.createTextFromFloat(lightBar, 4, 2, 0);
            rcDoc.drawTextAnchored(lightNumber, cx, cy, 0, 0, 0);

            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    private static void drawLineAt(RemoteComposeWriter rcDoc, float lightBar, float cx, float cy,
            float rad) {

        rcDoc.save();
        float sub = rcDoc.floatExpression(lightBar, 6, MUL, 90, SUB);
        rcDoc.rotate(sub, cx, cy);
        rcDoc.drawLine(cx, cy, cx, rad);
        rcDoc.restore();
    }

    /**
     * Demo showing a compass using magnetometer data.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter compass() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            rcDoc.floatExpression(
                    FLOAT_CONTINUOUS_SEC); // TODO fix so this can be removed
            float angle = rcDoc.floatExpression(
                    exp(FLOAT_MAGNETIC_X, FLOAT_MAGNETIC_Y, ATAN2, DEG),
                    rcDoc.spring(3f, 3f, 0.01f, 0));
            float w = rcDoc.addComponentWidthValue();
            float h = rcDoc.addComponentHeightValue();
            float cx = rcDoc.floatExpression(w, 0.5f, MUL);
            float cy = rcDoc.floatExpression(h, 0.5f, MUL);
            rcDoc.getPainter().setColor(Color.BLUE).setStrokeWidth(40).setStyle(
                    Paint.Style.FILL).setStrokeCap(Paint.Cap.ROUND).commit();
            rcDoc.drawCircle(cx, cy, 200);
            rcDoc.getPainter().setColor(Color.BLACK).setStrokeWidth(40).setStyle(
                    Paint.Style.FILL).setStrokeCap(Paint.Cap.ROUND).commit();

            rcDoc.save();
            rcDoc.rotate(angle, cx, cy);

            rcDoc.drawLine(cx, cy, cx, 100);

            rcDoc.restore();
            rcDoc.endCanvas();
            rcDoc.endBox();
            rcDoc.getPainter().setColor(Color.CYAN).setStyle(Paint.Style.FILL).setTextSize(
                    64).commit();
            int lightNumber = rcDoc.createTextFromFloat(angle, 4, 2, 0);
            rcDoc.drawTextAnchored(lightNumber, cx, cy, 0, 0, 0);
        });
        return rcDoc;
    }

    static float[] exp(float... exp) {
        return exp;
    }
}
