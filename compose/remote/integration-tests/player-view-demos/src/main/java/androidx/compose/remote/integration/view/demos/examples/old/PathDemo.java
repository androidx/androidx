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
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAD;
import static androidx.compose.remote.creation.Rc.FloatExpression.ABS;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.COS;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemotePath;

import org.jspecify.annotations.NonNull;

/**
 * Demos for path tweening and remote construction.
 */
@SuppressLint("RestrictedApiAndroidX")
public class PathDemo extends RCDemo {
    private PathDemo() {
        mPrivateConstructorForUtilityClassWorkAround = false;
    }

    /**
     * Demo showing path tweening between different shapes.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter pathTweenDemo() {
        return demoCanvas("graph", (rc) -> {
            float tween1 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB, ABS);
            float tween2 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 8, MOD, 4, DIV, 1, SUB, ABS);
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            RemotePath path1 = createCircle(60, 3, 150);
            RemotePath path2 = createCircle(60, 4, 150);
            RemotePath path3 = createCircle(60, 60, 150);
            int pid1 = rc.addPathData(path1);
            int pid2 = rc.addPathData(path2);
            int pid3 = rc.addPathData(path3);
            int pid12 = rc.pathTween(pid1, pid2, tween1);
            int pid123 = rc.pathTween(pid12, pid3, tween2);
            float delta = 300;
            rc.translate(cx, cy);
            rc.translate(-delta, -delta);
            rc.getPainter().setColor(Color.RED).setStrokeWidth(3).commit();
            rc.drawPath(pid1);
            short color1 = rc.addColorExpression(Color.RED, Color.GREEN, tween1);
            rc.getPainter().setColorId(color1).setStrokeWidth(3).commit();
            rc.translate(delta, 0);
            rc.drawPath(pid12);

            rc.getPainter().setColor(Color.GREEN).setStrokeWidth(3).commit();
            rc.translate(delta, 0);
            rc.drawPath(pid2);
            short color2 = rc.addColorExpression(color1, Color.BLUE, tween2);
            rc.getPainter().setColorId(color2).setStrokeWidth(3).commit();
            rc.translate(-delta, delta);
            rc.drawPath(pid123);
            rc.getPainter().setColor(Color.BLUE).setStrokeWidth(3).commit();
            rc.translate(0, delta);
            rc.drawPath(pid3);
        });
    }

    /**
     * Another demo showing path tweening and layouts.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter path2() {
        return demoCanvas("graph", (rc) -> {
            float tween1 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB, ABS);
            float tween2 = rc.floatExpression(FLOAT_CONTINUOUS_SEC, 8, MOD, 4, DIV, 1, SUB, ABS);
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            float margin = rc.floatExpression(w, h, MIN, 0.1f, MUL);

            float lineBottom = rc.floatExpression(h, margin, SUB);
            float lineRight = rc.floatExpression(w, margin, SUB);
            float[] data = {2, 3, 5, 12, 3, 1};
            rc.getPainter().setStrokeWidth(10).setColor(Color.DKGRAY).setStyle(
                    Paint.Style.STROKE).commit();
            rc.drawLine(margin, margin, margin, lineBottom);
            rc.drawLine(margin, lineBottom, lineRight, lineBottom);
            float array = rc.addFloatArray(data);
            rc.floatExpression(array, A_MAX);
            rc.floatExpression(array, A_MIN);
            float len = data.length; // rc.floatExpression(array, A_LEN);
            rc.startLoopVar(len, 0, 1f);

            RemotePath path1 = createCircle(60, 3, 150);
            RemotePath path2 = createCircle(60, 4, 150);
            RemotePath path3 = createCircle(60, 60, 150);
            int pid1 = rc.addPathData(path1);
            int pid2 = rc.addPathData(path2);
            int pid3 = rc.addPathData(path3);
            int pid12 = rc.pathTween(pid1, pid2, tween1);
            int pid123 = rc.pathTween(pid12, pid3, tween2);
            float delta = 300;
            rc.translate(cx, cy);
            rc.translate(-delta, -delta);
            rc.getPainter().setColor(Color.RED).setStrokeWidth(3).commit();
            rc.drawPath(pid1);
            short color1 = rc.addColorExpression(Color.RED, Color.GREEN, tween1);
            rc.getPainter().setColorId(color1).setStrokeWidth(3).commit();
            rc.translate(delta, 0);
            rc.drawPath(pid12);

            rc.getPainter().setColor(Color.GREEN).setStrokeWidth(3).commit();
            rc.translate(delta, 0);
            rc.drawPath(pid2);
            short color2 = rc.addColorExpression(color1, Color.BLUE, tween2);
            rc.getPainter().setColorId(color2).setStrokeWidth(3).commit();
            rc.translate(-delta, delta);
            rc.drawPath(pid123);
            rc.getPainter().setColor(Color.BLUE).setStrokeWidth(3).commit();
            rc.translate(0, delta);
            rc.drawPath(pid3);
        });
    }

    /**
     * Demo showing remote construction of a path using a loop.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter remoteConstruction() {
        return demoCanvas("graph", (rc) -> {
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            float radius = rc.floatExpression(w, h, MIN, 3, DIV);
            float top = rc.floatExpression(cy, radius, SUB);

            rc.getPainter().setStrokeWidth(10).setColor(Color.RED).setStyle(
                    Paint.Style.STROKE).commit();
            rc.drawCircle(cx, cy, radius);

            rc.getPainter().setStrokeWidth(10).setColor(Color.GREEN).setAlpha(0.5f).setStyle(
                    Paint.Style.FILL_AND_STROKE).commit();

            int path = rc.pathCreate(cx, top);
            float step = rc.startLoopVar(0, 10, 360);
            {
                float x = rc.floatExpression(cx, step, RAD, SIN, radius, MUL, ADD);
                float y = rc.floatExpression(cy, step, RAD, COS, radius, MUL, SUB);
                rc.pathAppendLineTo(path, x, y);
            }
            rc.endLoop();
            rc.pathAppendClose(path);
            rc.drawPath(path);
        });
    }

    static RemotePath createCircle(int points, int sides, float radius) {
        int side = points / sides;
        double angleStep = Math.toRadians(360. / sides);
        double angle = 0;

        RemotePath path = null;

        for (int s = 0; s < sides; s++) {
            double x1 = radius * Math.sin(angle);
            double y1 = -radius * Math.cos(angle);
            angle += angleStep;
            double x2 = radius * Math.sin(angle);
            double y2 = -radius * Math.cos(angle);
            for (int i = 0; i < side; i++) {
                float x = (float) (x1 + (x2 - x1) * (i / (float) side));
                float y = (float) (y1 + (y2 - y1) * (i / (float) side));
                if (path == null) {
                    path = new RemotePath();
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
        }
        if (path != null) {
            path.close();
        }
        return path;
    }
}
