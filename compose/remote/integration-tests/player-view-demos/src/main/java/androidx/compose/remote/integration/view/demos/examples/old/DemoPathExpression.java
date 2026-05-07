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

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1;
import static androidx.compose.remote.creation.Rc.FloatExpression.ABS;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.COS;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MAX;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.SMOOTH_STEP;
import static androidx.compose.remote.creation.Rc.FloatExpression.SQRT;
import static androidx.compose.remote.creation.Rc.FloatExpression.SQUARE;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for path expression capabilities in RemoteCompose.
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoPathExpression {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private DemoPathExpression() {
    }

    /**
     * Demo showing basic path expressions and polar path expressions.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter pathTest1() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                RcProfiles.PROFILE_ANDROIDX, sPlatform);

        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            rc.getPainter().setColor(Color.GRAY).commit();

            rc.drawRect(0, 0, w, h);

            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            float rad = rc.floatExpression(cx, cy, MIN);
            float rad2 = rc.floatExpression(rad, 2, DIV);
            float rad3 = rc.floatExpression(rad, 0.7f, MUL);
            float pi2 = (float) (Math.PI * 2);

            float bump = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, pi2, MUL,
                    SIN, 70, MUL);
            float rot = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, pi2,
                    MOD);
            rc.getPainter().setColor(0xFF7777ff).commit();
            rc.drawCircle(cx, cy, rad);
            rc.getPainter().setColor(0xFF77aaff).commit();
            {
                rc.getPainter().setColor(0xFFAA8844).setStrokeCap(Paint.Cap.ROUND).commit();
                int pathId = rc.addPolarPathExpression(
                        new float[]{rad3, 1, VAR1, (float) Math.PI, ADD, SIN, SUB, MUL}, 0, pi2,
                        60, cx, rc.floatExpression(cy, 0.5f, MUL), 0);
                rc.drawPath(pathId);
            }

            {
                float start = rc.floatExpression((float) Math.PI,
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, ADD);
                float end = rc.floatExpression(start, (float) Math.PI, ADD);
                rc.getPainter().setColor(Color.BLACK).setStrokeWidth(16f).setStyle(
                        Paint.Style.STROKE).commit();
                int pathId = rc.addPathExpression(new float[]{VAR1, SIN, rad2, MUL, cx, ADD},
                        new float[]{VAR1, COS, rad2, MUL, VAR1, 6, MUL, rot, ADD, COS, bump, MUL,
                                cy, ADD, ADD}, start, end, 60, Rc.PathExpression.LOOP_PATH);
                rc.drawPath(pathId);
            }
            {
                rc.getPainter().setColor(Color.BLUE).commit();
                int pathId = rc.addPathExpression(new float[]{VAR1, SIN, rad2, MUL, cx, ADD},
                        new float[]{VAR1, COS, rad2, MUL, VAR1, 6, MUL, rot, ADD, COS, bump, MUL,
                                cy, ADD, ADD}, 0, pi2, 60, Rc.PathExpression.LOOP_PATH);
                rc.drawPath(pathId);
            }
            {
                rc.getPainter().setColor(Color.GREEN).commit();
                int pathId = rc.addPathExpression(
                        new float[]{VAR1, (float) Math.PI, DIV, 1, SUB, rad2, MUL, cx, ADD},
                        new float[]{(float) Math.PI, VAR1, SUB, SQUARE, 0.04f, MUL, rad2, MUL,
                                VAR1, 6, MUL, rot, ADD, COS, bump, MUL, cy, ADD, ADD}, 0, pi2, 60,
                        0);
                rc.drawPath(pathId);
            }
            {
                rc.getPainter().setColor(Color.RED).commit();
                int pathId = rc.addPolarPathExpression(
                        new float[]{rad2, VAR1, 10, MUL, rot, ADD, COS, 20, MUL, ADD}, 0, pi2, 60,
                        cx, cy, Rc.PathExpression.LOOP_PATH);
                rc.drawPath(pathId);
            }
            {
                float end = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                        (float) Math.PI, ADD);
                rc.getPainter().setColor(Color.YELLOW).setStrokeCap(Paint.Cap.ROUND).commit();
                int pathId = rc.addPolarPathExpression(
                        new float[]{rad2, VAR1, 10, MUL, rot, ADD, COS, 20, MUL, ADD, 20, ADD},
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, end, 60, cx, cy, 0);
                rc.drawPath(pathId);
            }
            {
                float root2 = (float) Math.sqrt(2);
                float s = 0.7f;
                rc.getPainter().setColor(Color.WHITE).setStrokeCap(Paint.Cap.ROUND).commit();
                int pathId = rc.addPolarPathExpression(
                        new float[]{rad3, root2, MUL, VAR1, 2, MUL, SIN, ABS, 0.0001f, ADD, s, MUL,
                                DIV, 1, 1, s * s, 2, VAR1, MUL, SIN, SQUARE, 0.0001f, ADD, MUL,
                                SUB, SQRT, SUB, SQRT, MUL}, 1, (float) Math.PI * 2 + 1, 60, cx, cy,
                        Rc.PathExpression.LOOP_PATH);
                rc.drawPath(pathId);
            }

            rc.endCanvas();
        });
        return rc;
    }

    /**
     * Demo showing path morphing (tweening) between two expressions.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter pathTest2() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                RcProfiles.PROFILE_ANDROIDX, sPlatform);

        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            rc.getPainter().setColor(Color.GRAY).commit();

            rc.drawRect(0, 0, w, h);

            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            float rad = rc.floatExpression(cx, cy, MIN);
            float rad2 = rc.floatExpression(rad, 2, DIV);
            float pi2 = (float) (Math.PI * 2);

            float rot = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, pi2,
                    MOD);
            float rot2 = rc.floatExpression(rot, Rc.Time.ANIMATION_TIME, 10, 15, SMOOTH_STEP, MUL);
            rc.getPainter().setColor(0xFF7777ff).commit();

            rc.drawCircle(cx, cy, rad);
            rc.getPainter().setColor(0xFF77aaff).commit();

            rc.drawCircle(cx, cy, 20);

            rc.getPainter().setStyle(Paint.Style.STROKE).setColor(Color.RED).commit();
            int pathId1 = rc.addPolarPathExpression(
                    new float[]{rad2, VAR1, 10, MUL, rot2, ADD, COS, 50, MUL, ADD}, 0.07f,
                    pi2 + 0.07f, 120, cx, cy, Rc.PathExpression.LOOP_PATH);
            rc.drawPath(pathId1);

            float root2 = (float) Math.sqrt(2);
            float s = 0.7f;

            rc.getPainter().setColor(Color.GREEN).setStrokeCap(Paint.Cap.ROUND).commit();
            int pathId3 = rc.addPolarPathExpression(
                    new float[]{rad2, root2, MUL, VAR1, 2, MUL, SIN, ABS, 0.00001f, ADD, s, MUL,
                            DIV, 1, 1, s * s, 2, VAR1, MUL, SIN, SQUARE, 0.00001f, ADD, MUL, SUB,
                            SQRT, SUB, SQRT, MUL}, .07f, (float) Math.PI * 2 + .07f, 120, cx, cy,
                    Rc.PathExpression.LOOP_PATH);
            rc.drawPath(pathId3);

            float rock = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MOD, 2,
                    SUB, ABS, 2, DIV);

            rc.getPainter().setColor(Color.MAGENTA).setStrokeWidth(14f).setStyle(
                    Paint.Style.FILL).commit();

            rc.drawTweenPath(pathId1, pathId3, rock, 0, 1);
            rc.getPainter().setColor(Color.WHITE).setStrokeWidth(14f).setStyle(
                    Paint.Style.STROKE).commit();

            rc.drawTweenPath(pathId1, pathId3, rock, 0, 1);

            rc.endCanvas();
        });
        return rc;
    }

    /**
     * Demo showing linear vs cubic path expression interpolation.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter pathTest3() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                RcProfiles.PROFILE_ANDROIDX, sPlatform);

        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            rc.getPainter().setColor(Color.GRAY).commit();

            rc.drawRect(0, 0, w, h);

            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            float rad = rc.floatExpression(cx, cy, MIN);
            float rad2 = rc.floatExpression(rad, 2, DIV);
            float pi2 = (float) (Math.PI * 2);

            float rot = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, pi2,
                    MOD);
            float rot2 = rc.floatExpression(rot, Rc.Time.ANIMATION_TIME, 10, SUB, 0, MAX, 1,
                    Rc.FloatExpression.MIN, MUL);
            rc.getPainter().setColor(0xFF7777ff).commit();

            rc.drawCircle(cx, cy, rad);
            rc.getPainter().setColor(0xFF77aaff).commit();

            rc.drawCircle(cx, cy, 20);

            rc.getPainter().setStyle(Paint.Style.STROKE).setColor(Color.RED).commit();
            int pathId1 = rc.addPolarPathExpression(
                    new float[]{rad2, VAR1, 10, MUL, rot2, ADD, COS, 50, MUL, ADD}, 0.07f,
                    pi2 + 0.07f, 20, cx, cy, Rc.PathExpression.LOOP_PATH);
            int pathId2 = rc.addPolarPathExpression(
                    new float[]{rad2, VAR1, 10, MUL, rot2, ADD, COS, 50, MUL, ADD}, 0.07f,
                    pi2 + 0.07f, 20, cx, cy,
                    Rc.PathExpression.LOOP_PATH | Rc.PathExpression.LINEAR_PATH);
            rc.drawPath(pathId1);

            float root2 = (float) Math.sqrt(2);
            float s = 0.7f;

            rc.getPainter().setColor(Color.GREEN).setStrokeCap(Paint.Cap.ROUND).commit();
            int pathId3 = rc.addPolarPathExpression(
                    new float[]{rad2, root2, MUL, VAR1, 2, MUL, SIN, ABS, 0.00001f, ADD, s, MUL,
                            DIV, 1, 1, s * s, 2, VAR1, MUL, SIN, SQUARE, 0.00001f, ADD, MUL, SUB,
                            SQRT, SUB, SQRT, MUL}, .07f, (float) Math.PI * 2 + .07f, 20, cx, cy,
                    Rc.PathExpression.LOOP_PATH | Rc.PathExpression.LINEAR_PATH);
            rc.drawPath(pathId3);

            float rock = rc.floatExpression(RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MOD, 2,
                    SUB, ABS, 2, DIV);

            rc.getPainter().setColor(Color.MAGENTA).setStrokeWidth(14f).setStyle(
                    Paint.Style.FILL).commit();

            rc.drawTweenPath(pathId1, pathId2, rock, 0, 1);
            rc.getPainter().setColor(Color.WHITE).setStrokeWidth(14f).setStyle(
                    Paint.Style.STROKE).commit();

            rc.drawTweenPath(pathId1, pathId2, rock, 0, 1);

            rc.endCanvas();
        });
        return rc;
    }
}
