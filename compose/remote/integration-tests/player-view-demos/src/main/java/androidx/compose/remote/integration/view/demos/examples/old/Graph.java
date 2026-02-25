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

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_DEREF;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_LEN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SPLINE;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for procedural graph drawing.
 */
@SuppressLint("RestrictedApiAndroidX")
public class Graph {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private Graph() {

    }

    /**
     * Demo showing a bar graph and a spline graph.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter graph1() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, "Graph", 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float margin = rc.floatExpression(w, h, MIN, 0.1f, MUL);
            float lineBottom = rc.floatExpression(h, margin, SUB);
            float lineRight = rc.floatExpression(w, margin, SUB);
            rc.getPainter().setColor(Color.GRAY).setStyle(Paint.Style.FILL).commit();
            rc.drawRect(0, 0, w, h);
            float[] data = {2, 3, 5, 12, 3, 1, 8, 4, 2};
            rc.getPainter().setStrokeWidth(10).setColor(Color.DKGRAY).setStyle(
                    Paint.Style.STROKE).commit();
            rc.drawLine(margin, margin, margin, lineBottom);
            rc.drawLine(margin, lineBottom, lineRight, lineBottom);
            float array = rc.addFloatArray(data);
            float max = rc.floatExpression(array, A_MAX);
            float min = rc.floatExpression(array, A_MIN, 1, SUB);
            float len = rc.floatExpression(array, A_LEN);
            float grHeight = rc.floatExpression(h, margin, 2, MUL, SUB);
            float grWidth = rc.floatExpression(w, margin, 2, MUL, SUB);
            float scale = rc.floatExpression(grHeight, max, min, SUB, DIV);
            rc.getPainter().setTextSize(60).commit();

            {
                float xstep = rc.floatExpression(grWidth, len, DIV);
                float index = rc.startLoopVar(0, 1f, len);
                float xPos1 = rc.floatExpression(xstep, index, MUL, margin, ADD);
                float xPos2 = rc.floatExpression(xPos1, xstep, ADD);
                float yVal = rc.floatExpression(array, index, A_DEREF);
                float lineTop = rc.floatExpression(grHeight, scale, yVal, min, SUB, MUL, SUB,
                        margin, ADD);
                rc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).setStrokeWidth(
                        1).commit();
                rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
                int textId = rc.createTextFromFloat(yVal, 2, 0, 0);
                rc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.STROKE).setStrokeWidth(
                        2).commit();
                rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
                float xPosCenter = rc.floatExpression(xPos1, xPos2, ADD, 2, DIV);
                rc.getPainter().setStyle(Paint.Style.FILL).commit();

                rc.drawTextAnchored(textId, xPosCenter, lineTop, 0, 2, 0);
                rc.endLoop();
            }
            {
                rc.getPainter().setStrokeWidth(10).setColor(Color.GREEN).setStyle(
                        Paint.Style.STROKE).setStrokeWidth(4).commit();
                float step = 10;
                float xOff = rc.floatExpression(margin, grWidth, len, DIV, 2, DIV, ADD);
                float xScale = rc.floatExpression(1, grWidth, grWidth, len, DIV, SUB, DIV);
                float yOff = rc.floatExpression(grHeight, scale, min, MUL, SUB, margin, ADD);
                float sx1 = rc.startLoopVar(margin, step, rc.floatExpression(w, margin, SUB));
                float sx2 = rc.floatExpression(sx1, step, ADD);
                float x1 = rc.floatExpression(sx1, xOff, SUB, xScale, MUL);
                float x2 = rc.floatExpression(sx2, xOff, SUB, xScale, MUL);
                float y1 = rc.floatExpression(yOff, scale, array, x1, A_SPLINE, MUL, SUB);
                float y2 = rc.floatExpression(yOff, scale, array, x2, A_SPLINE, MUL, SUB);
                rc.drawLine(sx1, y1, sx2, y2);
                rc.endLoop();
            }

            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }

    /**
     * Demo showing a spline graph with linear gradient fill.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter graph2() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, "Graph2", 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float margin = rc.floatExpression(w, h, MIN, 0.1f, MUL);
            float lineBottom = rc.floatExpression(h, margin, SUB);
            float lineRight = rc.floatExpression(w, margin, SUB);
            rc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.FILL).commit();
            rc.drawRect(0, 0, w, h);
            float[] data = {2, 3, 5, 12, 3, 1, 8, 4, 2};

            float array = rc.addFloatArray(data);
            float max = rc.floatExpression(array, A_MAX);
            float min = rc.floatExpression(array, A_MIN, 1, SUB);
            float len = rc.floatExpression(array, A_LEN);
            float grHeight = rc.floatExpression(h, margin, 2, MUL, SUB);
            float grWidth = rc.floatExpression(w, margin, 2, MUL, SUB);
            float scale = rc.floatExpression(grHeight, max, min, SUB, DIV);
            rc.getPainter().setTextSize(60).commit();

            float xstep = rc.floatExpression(grWidth, len, DIV);
            float index = rc.startLoopVar(0, 1f, len);
            {
                float xPos1 = rc.floatExpression(xstep, index, MUL, margin, ADD);
                float xPos2 = rc.floatExpression(xPos1, xstep, ADD);
                float yVal = rc.floatExpression(array, index, A_DEREF);
                float lineTop = rc.floatExpression(grHeight, scale, yVal, min, SUB, MUL, SUB,
                        margin, ADD);
                rc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).setStrokeWidth(
                        1).commit();
                rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
                int textId = rc.createTextFromFloat(yVal, 2, 0, 0);
                rc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.STROKE).setStrokeWidth(
                        2).commit();
                rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
                float xPosCenter = rc.floatExpression(xPos1, xPos2, ADD, 2, DIV);
                rc.getPainter().setStyle(Paint.Style.FILL).commit();

                rc.drawTextAnchored(textId, xPosCenter, lineTop, 0, 2, 0);
            }
            rc.endLoop();

            {
                rc.getPainter().setStrokeWidth(10).setColor(Color.GREEN).setStyle(
                        Paint.Style.STROKE).commit();

                float step = 10;
                float xOff = rc.floatExpression(margin, grWidth, len, DIV, 2, DIV, ADD);
                float xScale = rc.floatExpression(1, grWidth, grWidth, len, DIV, SUB, DIV);
                float yOff = rc.floatExpression(grHeight, scale, min, MUL, SUB, margin, ADD);
                float end = rc.floatExpression(w, margin, SUB);
                int path = rc.pathCreate(margin, lineBottom);
                float sx1 = rc.startLoopVar(margin, step, end);
                {
                    float x1 = rc.floatExpression(sx1, xOff, SUB, xScale, MUL);
                    float y1 = rc.floatExpression(yOff, scale, array, x1, A_SPLINE, MUL, SUB);
                    rc.pathAppendLineTo(path, sx1, y1);
                }
                rc.endLoop();
                rc.pathAppendLineTo(path, end, lineBottom);
                rc.pathAppendClose(path);
                rc.getPainter().setStyle(Paint.Style.FILL).setLinearGradient(0, 0, 0, h,
                        new int[]{0xAAFF0000, 0x44FF0000}, null, Shader.TileMode.CLAMP).commit();
                float top = rc.floatExpression(margin, 1, ADD);
                float left = rc.floatExpression(margin, 10, ADD);
                float bottom = rc.floatExpression(lineBottom, 10, SUB);
                float right = rc.floatExpression(end, 20, SUB);
                rc.save();
                rc.clipRect(left, top, right, bottom);
                rc.drawPath(path);
                rc.getPainter().setStrokeWidth(10).setShader(0).setStyle(
                        Paint.Style.STROKE).setColor(Color.RED).commit();
                rc.drawPath(path);
                rc.restore();
            }
            // ======= axis
            rc.getPainter().setStrokeWidth(10).setColor(Color.BLACK).setStyle(
                    Paint.Style.STROKE).commit();
            rc.drawLine(margin, margin, margin, lineBottom);
            rc.drawLine(margin, lineBottom, lineRight, lineBottom);
            // ====== wnd axis
            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }
}
