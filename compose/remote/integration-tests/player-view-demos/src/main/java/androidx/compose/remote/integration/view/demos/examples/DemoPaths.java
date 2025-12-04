/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples;

import static androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.ADD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_DEREF;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_LEN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SPLINE;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.COS;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MOD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SIN;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1;
import static androidx.compose.remote.creation.Rc.FloatExpression.ABS;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MAX;
import static androidx.compose.remote.creation.Rc.FloatExpression.PINGPONG;
import static androidx.compose.remote.creation.Rc.FloatExpression.SMOOTH_STEP;
import static androidx.compose.remote.creation.Rc.FloatExpression.SQRT;
import static androidx.compose.remote.creation.Rc.FloatExpression.SQUARE;
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
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout;
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintPathEffects;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.RemotePath;
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.modifiers.ScrollModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

import java.util.Random;

/**
 * This is a demo of many of the path APIs
 * paint.setPathEffect
 * RemotePath object
 * rc.addPathData
 * rc.addPolarPathExpression
 * rc.addPathExpression(
 * rc.drawPath(pathId1);
 * rc.drawTweenPath
 * rc.pathCreate(); rc.pathAppendLineTo() , rc.pathAppendClose()
 * Building a path from Text & a Paint
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoPaths {
    @SuppressLint("RestrictedApiAndroidX")
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private DemoPaths() {
    }

    /**
     * Test of various computed paths
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RemoteComposeWriter pathTest() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                PROFILE_ANDROIDX, sPlatform);
        float touchPosition = rc.addFloatConstant(0f);
        float computedHeight = rc.addFloatConstant(1080);
        float scrollSize = rc.floatExpression(computedHeight, 6, MUL);
        float visFloat = rc.addFloatConstant(1f);
        int vis = Utils.idFromNan(visFloat);
        float notVisFloat = rc.addFloatConstant(0f);
        int notVis = Utils.idFromNan(notVisFloat);
        float scrollPosition = rc.floatExpression(touchPosition, computedHeight, 20, ADD, MUL);

        rc.root(
                () -> {
                    rc.box(new RecordingModifier().fillMaxSize().background(Color.DKGRAY).padding(
                            4));
                    rc.column(new RecordingModifier().fillMaxSize().then(
                            new CustomScroller(
                                    0,
                                    ScrollModifier.VERTICAL,
                                    touchPosition,
                                    scrollPosition,
                                    6,
                                    scrollSize
                            )

                    ), ColumnLayout.START, ColumnLayout.TOP, () -> {
                        rc.startCanvasOperations();
                        float scrollHeight = rc.floatExpression(rc.addComponentHeightValue());

                        // -------------force refresh when height changes ---------------
                        rc.conditionalOperations(Rc.Condition.NEQ, scrollHeight, computedHeight);
                        toggle(rc, visFloat, notVisFloat);
                        rc.endConditionalOperations();
                        rc.startRunActions();
                        ValueFloatExpressionChange action =
                                new ValueFloatExpressionChange(
                                        Utils.idFromNan(computedHeight),
                                        Utils.idFromNan(scrollHeight)
                                );
                        rc.addAction(action);
                        rc.endRunActions();
                        // -------------force refresh when height changes ---------------


                        rc.drawComponentContent();
                        rc.endCanvasOperations();
                        graph2(rc, computedHeight);

                        pathEffects1(rc, computedHeight);
                        pathEffects2(rc, computedHeight);
                        effect1(rc, computedHeight);
                        effect2(rc, computedHeight);
                        effect3(rc, computedHeight);
                        graph1(rc, computedHeight);
                    });
                    rc.endCanvas();
                    rc.startCanvas(new RecordingModifier().visibility(vis).width(12).height(0));
                    rc.endCanvas();
                    rc.startCanvas(new RecordingModifier().visibility(notVis).width(12).height(0));
                    rc.endCanvas();
                    rc.endBox();


                });
        return rc;
    }

    @SuppressLint("RestrictedApiAndroidX")
    static void pathEffects1(RemoteComposeWriterAndroid rc, float height) {
        rc.startCanvas(new RecordingModifier().fillMaxWidth().height(height));
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();

        float gap = 22f;
        float cx = rc.floatExpression(w, 0.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        float rad1 = rc.floatExpression(cx, cy, MIN);
        float rad2 = rc.floatExpression(rad1, gap * 2, SUB);
        float rad3 = rc.floatExpression(rad1, gap * 4, SUB);
        float rad4 = rc.floatExpression(rad1, gap * 6, SUB);
        float rad5 = rc.floatExpression(rad1, gap * 8, SUB);
        float rad6 = rc.floatExpression(rad1, gap * 10, SUB);
        float rad7 = rc.floatExpression(rad1, gap * 12, SUB);
        float rad8 = rc.floatExpression(rad1, gap * 14, SUB);
        float d1 = rc.floatExpression(rad1, (float) (Math.PI * 2), MUL);
        float d2 = rc.floatExpression(rad2, (float) (Math.PI * 2), MUL);
        float ringTop = rc.floatExpression(cx, rad1, 0.5f, MUL, SUB);
        float ringBottom = rc.floatExpression(cx, rad1, 0.5f, MUL, ADD);
        float edge = 10;
        rc.getPainter()
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(gap)
                .commit();
        //  background ring

        ring(rc, cx, cy, w, h, gap, ringTop, ringBottom);

        rc.getPainter()
                .setStrokeWidth(gap).commit();

        //  ====== start mask circle ======
        rc.getPainter()
                .setColor(Color.DKGRAY)
                .setStyle(Paint.Style.FILL)
                .setLinearGradient(0, 0, 0, h, new int[]{Color.LTGRAY, 0xFF202050}, null,
                        Shader.TileMode.CLAMP)
                .setPathEffect(null)
                .commit();

        rc.drawCircle(cx, cy, rad2);

        rc.getPainter()
                .setColor(Color.GRAY)
                .setPathEffect(null)
                .setShader(0)
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(edge)
                .commit();

        rc.drawCircle(cx, cy, rad2);

        //  ====== end mask circle ======
        {
            // Filling out a path
            float rock =
                    rc.floatExpression(Rc.Time.ANIMATION_TIME, 1, PINGPONG, d1, MUL);

            float rock1 =
                    rc.floatExpression(d1, rock, SUB);
            rc.getPainter()
                    .setColor(Color.MAGENTA)

                    .setPathEffect(PaintPathEffects.dash(0, new float[]{rock, rock1}))
                    .commit();
            rc.drawCircle(cx, cy, rad3);
        }

        // ======= Traveling along a path =======
        {
            float gap1 =
                    rc.floatExpression(d2, 0.1f, MUL);
            float rock2 =
                    rc.floatExpression(Rc.Time.ANIMATION_TIME, 1, PINGPONG, d2, gap1, SUB, MUL);

            float rock3 =
                    rc.floatExpression(d2, rock2, SUB, gap1, SUB);
            rc.getPainter()
                    .setColor(Color.BLUE)
                    .setPathEffect(PaintPathEffects.dash(0, new float[]{0, rock2, gap1, rock3}))
                    .commit();
            rc.drawCircle(cx, cy, rad4);
        }
        // ======= Chain? =======
        {
            float stretch1 =
                    rc.floatExpression(Rc.Time.ANIMATION_TIME, 1, PINGPONG, 70, MUL);
            float stretch2 =
                    rc.floatExpression(stretch1, -1, MUL);

            RemotePath path = new RemotePath();

            path.cubicTo(25, stretch1, 75, stretch2, 100, 0);
            int pathId = rc.addPathData(path);

            rc.getPainter()
                    .setColor(Color.YELLOW)
                    .setStrokeWidth(5)
                    .setPathEffect(PaintPathEffects.pathDash(pathId, 100, stretch1,
                            Rc.PathEffect.PATH_DASH_MORPH))
                    .commit();

            rc.drawCircle(cx, cy, rad5);

            int pathId2 = rc.addPathData(
                    buildPathFromText("Path effects unlimited", new Paint(), 3));
            float advance = rc.floatExpression(rad6, (float) Math.PI, MUL);
            rc.getPainter()
                    .setColor(Color.GREEN)
                    .setStrokeWidth(5)
                    .setPathEffect(PaintPathEffects.pathDash(pathId2, advance, 0,
                            Rc.PathEffect.PATH_DASH_MORPH))
                    .commit();

            rc.drawCircle(cx, cy, rad6);
            // ======= 2 paths in parallel =======

            float seglen = rc.floatExpression(rad6, (float) (Math.PI * 2), MUL, 8, DIV);

            rc.getPainter()
                    .setColor(Color.RED)
                    .setStrokeWidth(5)
                    .setPathEffect(
                            PaintPathEffects.sum(
                                    PaintPathEffects.dash(0, new float[]{10, 10}),
                                    PaintPathEffects.discrete(seglen, 0)
                            )
                    )
                    .commit();


            rc.drawCircle(cx, cy, rad7);
            // ======= two paths sum =======

            rc.getPainter()
                    .setColor(0x99FF00FF)
                    .setStrokeWidth(10)
                    .setPathEffect(
                            PaintPathEffects.sum(
                                    PaintPathEffects.dash(0, new float[]{7, 13}),
                                    PaintPathEffects.dash(32, 100, 100)
                            )
                    )
                    .commit();


            rc.drawCircle(cx, cy, rad8);

            rc.getPainter()
                    .setStrokeWidth(10).commit();

        }


        // ====== start overlay ring ======
        rc.save();
        rc.clipRect(0, cx, w, h);
        ring(rc, cx, cy, w, h, gap, ringTop, ringBottom);
        rc.restore();
        rc.getPainter()
                .setPathEffect(null)
                .commit();
        rc.endCanvas();
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static void ring(RemoteComposeWriterAndroid rc, float cx, float cy, float w, float h,
            float gap, float ringTop, float ringBottom) {
        rc.save();
        int ringColor2 = 0xFFFF9900;
        int ringColor1 = 0xFF775500;
        int ringColor0 = 0xFF442200;
        System.out.println(cx == w ? h + gap + ringBottom + ringBottom + ringTop + "" : "");
        float ramp4 =
                rc.floatExpression(Rc.Time.ANIMATION_TIME, 1, MOD, 100, MUL);

        RemotePath path = new RemotePath();
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            float y = r.nextFloat() * 50 - 25;
            float x = r.nextFloat() * 100;
            float len = r.nextFloat() * 50 + 10;
            path.moveTo(x, y);
            path.lineTo(x + len, y);
            path.lineTo(x + len, y + 1);
            path.lineTo(x, y + 1);
            path.close();
        }


        int pathId = rc.addPathData(path);

        rc.getPainter()
                .setColor(ringColor2)
//                .setPathEffect(RcPathEffects.dash(ramp4, new float[]{50, 10, 50, 0.0001f}))
                .setPathEffect(
                        PaintPathEffects.dash(ramp4, new float[]{10, 90})

//                                RcPathEffects.pathDash(pathId, 100, 0, Rc.PathEffect
//                                .PATH_DASH_MORPH)

                )
                .commit();

//        rc.drawOval(0, ringTop, w, ringBottom);


        rc.scale(1, 0.5f, 0, cy);
        // rc.rotate(90,cx,cy);
        rc.getPainter()
                .setPathEffect(null)
                .setStrokeWidth(50)
                .setColor(ringColor1)
                .commit();
        rc.drawCircle(cx, cy, cx);
        float outer = rc.floatExpression(cx, (25 + 50) / 2f + 2, ADD);
        float gapLine = rc.floatExpression(cx, 15, ADD);
        rc.getPainter()
                .setColor(ringColor0)
                .commit();
        rc.drawCircle(cx, cy, outer);
        rc.getPainter()
                .setColor(ringColor2)
                .setStrokeWidth(30)
//                .setPathEffect(RcPathEffects.dash(ramp4, new float[]{10, 90}))
                .setPathEffect(PaintPathEffects.dash(ramp4, new float[]{5, 95}))
                .commit();


        rc.drawCircle(cx, cy, gapLine);

        rc.getPainter()
                .setColor(ringColor2)
                .setStrokeWidth(50)
//                .setPathEffect(RcPathEffects.dash(ramp4, new float[]{10, 90}))
                .setPathEffect(
                        PaintPathEffects.pathDash(pathId, 100, ramp4,
                                Rc.PathEffect.PATH_DASH_MORPH))
                .commit();


        rc.drawCircle(cx, cy, gapLine);
        rc.getPainter()
                .setPathEffect(null)
                .setStrokeWidth(3)
                .setColor(ringColor2)
                .commit();
        rc.drawCircle(cx, cy, cx);

        rc.getPainter()
                .setPathEffect(null)
                .setStrokeWidth(3)
                .setColor(Color.DKGRAY)
                .commit();
        rc.drawCircle(cx, cy, cx);


        rc.restore();


    }

    @SuppressLint("RestrictedApiAndroidX")
    static void pathEffects2(RemoteComposeWriterAndroid rc, float height) {
        rc.startCanvas(new RecordingModifier().fillMaxWidth().height(height));
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();
//        rc.getPainter().setColor(Color.GRAY).commit();

        rc.drawRect(0, 0, w, h);

        float cx = rc.floatExpression(w, 0.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        float rad = rc.floatExpression(cx, cy, MIN);
        float rad2 = rc.floatExpression(rad, 22, SUB);
        float rad3 = rc.floatExpression(rad, 0.6f, MUL);
        float rad4 = rc.floatExpression(rad3, 100, SUB);
        float camelPi2 = (float) (Math.PI * 2);

        float rot =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, camelPi2, MOD);
        float rock =
                rc.floatExpression(
                        rot, Rc.Time.ANIMATION_TIME, 1, PINGPONG, 100, MUL);

        float rock1 =
                rc.floatExpression(100, rock, SUB);
        rc.getPainter()
                .setColor(0xFF7777ff)
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(22f)
                .setPathEffect(PaintPathEffects.dash(0, new float[]{rock, rock1}))
                .commit();

        rc.drawCircle(cx, cy, rad);
        rc.getPainter()
                .setColor(0xFF77ff77)
                .setPathEffect(PaintPathEffects.dash(0, new float[]{rock1, rock}))
                .commit();

        rc.drawRoundRect(
                rc.floatExpression(cx, rad, SUB, 120, ADD),
                rc.floatExpression(cy, rad, SUB, 120, ADD),
                rc.floatExpression(cx, rad, ADD, 120, SUB),
                rc.floatExpression(cy, rad, ADD, 120, SUB),
                80, 80);

        rc.getPainter()
                .setColor(0xFFff7777)
                .setPathEffect(null)
                .commit();
        rc.drawCircle(cx, cy, rad2);
        rc.getPainter()
                .setColor(0xFFffff77)
                .setStrokeWidth(10f)
                .setPathEffect(PaintPathEffects.discrete(20, rock))
                .commit();
        rc.drawCircle(cx, cy, rad3);

        int pathId = rc.addPathData(buildPathFromText("R", new Paint(), 10));
        rc.getPainter()
                .setColor(0xFFff00ff)
                .setStrokeWidth(10f)
                .setPathEffect(PaintPathEffects.pathDash(pathId, 100, 0, 2))
                .commit();
        rc.drawCircle(cx, cy, rad4);


        rc.getPainter()
                .setPathEffect(null)
                .commit();
        rc.endCanvas();
    }


    @SuppressLint("RestrictedApiAndroidX")
    static void effect1(RemoteComposeWriterAndroid rc, float height) {
        rc.startCanvas(new RecordingModifier().fillMaxWidth().height(height));
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();
        rc.getPainter().setColor(Color.GRAY).commit();

        rc.drawRect(0, 0, w, h);

        float cx = rc.floatExpression(w, 0.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        float rad = rc.floatExpression(cx, cy, MIN);
        float rad2 = rc.floatExpression(rad, 2, DIV);
        float camelPi2 = (float) (Math.PI * 2);

        float rot =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, camelPi2, MOD);
        float rot2 =
                rc.floatExpression(
                        rot, Rc.Time.ANIMATION_TIME, 10, 15, SMOOTH_STEP, MUL);
        rc.getPainter().setColor(0xFF7777ff).commit();

        rc.drawCircle(cx, cy, rad);
        rc.getPainter().setColor(0xFF77aaff).commit();

        rc.drawCircle(cx, cy, 20);

        rc.getPainter().setStyle(Paint.Style.STROKE).setColor(Color.RED).commit();
        int pathId1 =
                rc.addPolarPathExpression(
                        new float[]{rad2, VAR1, 10, MUL, rot2, ADD, COS, 50, MUL, ADD},
                        0.07f,
                        camelPi2 + 0.07f,
                        120,
                        cx,
                        cy,
                        Rc.PathExpression.LOOP_PATH);
        rc.drawPath(pathId1);

//        float end =
        rc.floatExpression(
                RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                (float) Math.PI,
                ADD);


        float root2 = (float) Math.sqrt(2);
        float s = 0.7f;

        rc.getPainter().setColor(Color.GREEN).setStrokeCap(Paint.Cap.ROUND).commit();
        int pathId3 =
                rc.addPolarPathExpression(
                        new float[]{
                                rad2, root2, MUL, VAR1, 2, MUL, SIN, ABS, 0.00001f, ADD,
                                s,
                                MUL, DIV, 1, 1, s * s, 2, VAR1, MUL, SIN, SQUARE,
                                0.00001f,
                                ADD, MUL, SUB, SQRT, SUB, SQRT, MUL
                        },
                        .07f,
                        (float) Math.PI * 2 + .07f,
                        120,
                        cx,
                        cy,
                        Rc.PathExpression.LOOP_PATH);
        rc.drawPath(pathId3);

        float rock =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                        4,
                        MOD,
                        2,
                        SUB,
                        ABS,
                        2,
                        DIV);

        rc.getPainter()
                .setColor(Color.MAGENTA)
                .setStrokeWidth(14f)
                .setStyle(Paint.Style.FILL)
                .commit();

        rc.drawTweenPath(pathId1, pathId3, rock, 0, 1);
        rc.getPainter()
                .setColor(Color.WHITE)
                .setStrokeWidth(14f)
                .setStyle(Paint.Style.STROKE)
                .commit();

        rc.drawTweenPath(pathId1, pathId3, rock, 0, 1);
        rc.endCanvas();
    }

    @SuppressLint("RestrictedApiAndroidX")
    static void effect2(RemoteComposeWriterAndroid rc, float height) {
        rc.startCanvas(new RecordingModifier().fillMaxWidth().height(height));
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();
        rc.getPainter().setColor(Color.GRAY).commit();

        rc.drawRect(0, 0, w, h);

        float cx = rc.floatExpression(w, 0.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        float rad = rc.floatExpression(cx, cy, MIN);
        float rad2 = rc.floatExpression(rad, 2, DIV);
        float camelPi2 = (float) (Math.PI * 2);

//        float bump =
        rc.floatExpression(
                RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                camelPi2,
                MUL,
                SIN,
                70,
                MUL);
        float rot =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, camelPi2, MOD);
        float rot2 =
                rc.floatExpression(
                        rot,
                        Rc.Time.ANIMATION_TIME,
                        10,
                        SUB,
                        0,
                        MAX,
                        1,
                        Rc.FloatExpression.MIN,
                        MUL);
        rc.getPainter().setColor(0xFF7777ff).commit();

        rc.drawCircle(cx, cy, rad);
        rc.getPainter().setColor(0xFF77aaff).commit();

        rc.drawCircle(cx, cy, 20);

        rc.getPainter().setStyle(Paint.Style.STROKE).setColor(Color.RED).commit();
        int pathId1 =
                rc.addPolarPathExpression(
                        new float[]{rad2, VAR1, 10, MUL, rot2, ADD, COS, 50, MUL, ADD},
                        0.07f,
                        camelPi2 + 0.07f,
                        20,
                        cx,
                        cy,
                        Rc.PathExpression.LOOP_PATH);
        int pathId2 =
                rc.addPolarPathExpression(
                        new float[]{rad2, VAR1, 10, MUL, rot2, ADD, COS, 50, MUL, ADD},
                        0.07f,
                        camelPi2 + 0.07f,
                        20,
                        cx,
                        cy,
                        Rc.PathExpression.LOOP_PATH | Rc.PathExpression.LINEAR_PATH);
        rc.drawPath(pathId1);

//        float end =
        rc.floatExpression(
                RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                (float) Math.PI,
                ADD);

        float root2 = (float) Math.sqrt(2);
        float s = 0.7f;

        rc.getPainter().setColor(Color.GREEN).setStrokeCap(Paint.Cap.ROUND).commit();
        int pathId3 =
                rc.addPolarPathExpression(
                        new float[]{
                                rad2, root2, MUL, VAR1, 2, MUL, SIN, ABS, 0.00001f, ADD,
                                s,
                                MUL, DIV, 1, 1, s * s, 2, VAR1, MUL, SIN, SQUARE,
                                0.00001f,
                                ADD, MUL, SUB, SQRT, SUB, SQRT, MUL
                        },
                        .07f,
                        (float) Math.PI * 2 + .07f,
                        20,
                        cx,
                        cy,
                        Rc.PathExpression.LOOP_PATH | Rc.PathExpression.LINEAR_PATH);
        rc.drawPath(pathId3);

        float rock =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                        4,
                        MOD,
                        2,
                        SUB,
                        ABS,
                        2,
                        DIV);

        rc.getPainter()
                .setColor(Color.MAGENTA)
                .setStrokeWidth(14f)
                .setStyle(Paint.Style.FILL)
                .commit();

        rc.drawTweenPath(pathId1, pathId2, rock, 0, 1);
        rc.getPainter()
                .setColor(Color.WHITE)
                .setStrokeWidth(14f)
                .setStyle(Paint.Style.STROKE)
                .commit();

        rc.drawTweenPath(pathId1, pathId2, rock, 0, 1);

        rc.endCanvas();

    }

    @SuppressLint("RestrictedApiAndroidX")
    static void effect3(RemoteComposeWriterAndroid rc, float height) {
        rc.startCanvas(new RecordingModifier().fillMaxWidth().height(height));
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();
        rc.getPainter().setColor(Color.GRAY).commit();

        rc.drawRect(0, 0, w, h);

        float cx = rc.floatExpression(w, 0.5f, MUL);
        float cy = rc.floatExpression(h, 0.5f, MUL);
        float rad = rc.floatExpression(cx, cy, MIN);
        float rad2 = rc.floatExpression(rad, 2, DIV);
        float rad3 = rc.floatExpression(rad, 0.7f, MUL);
        float camelPi2 = (float) (Math.PI * 2);

        float bump =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                        camelPi2,
                        MUL,
                        SIN,
                        70,
                        MUL);
        float rot =
                rc.floatExpression(
                        RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC, 4, MUL, camelPi2, MOD);
        rc.getPainter().setColor(0xFF7777ff).commit();
        rc.drawCircle(cx, cy, rad);
        rc.getPainter().setColor(0xFF77aaff).commit();
        {
            // float end =
            rc.floatExpression(
                    RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                    (float) Math.PI,
                    ADD);
            rc.getPainter().setColor(0xFFAA8844).setStrokeCap(Paint.Cap.ROUND).commit();
            int pathId =
                    rc.addPolarPathExpression(
                            new float[]{
                                    rad3, 1, VAR1, (float) Math.PI, ADD, SIN, SUB, MUL
                            },
                            0,
                            camelPi2,
                            60,
                            cx,
                            rc.floatExpression(cy, 0.5f, MUL),
                            0);
            rc.drawPath(pathId);
        }

        {
            float start =
                    rc.floatExpression(
                            (float) Math.PI,
                            RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                            ADD);
            float end = rc.floatExpression(start, (float) Math.PI, ADD);
            rc.getPainter()
                    .setColor(Color.BLACK)
                    .setStrokeWidth(16f)
                    .setStyle(Paint.Style.STROKE)
                    .commit();
            int pathId =
                    rc.addPathExpression(
                            new float[]{VAR1, SIN, rad2, MUL, cx, ADD},
                            new float[]{
                                    VAR1, COS, rad2, MUL, VAR1, 6, MUL, rot, ADD, COS,
                                    bump,
                                    MUL, cy, ADD, ADD
                            },
                            start,
                            end,
                            60,
                            Rc.PathExpression.LOOP_PATH);
            rc.drawPath(pathId);
        }
        {
            rc.getPainter().setColor(Color.BLUE).commit();
            int pathId =
                    rc.addPathExpression(
                            new float[]{VAR1, SIN, rad2, MUL, cx, ADD},
                            new float[]{
                                    VAR1, COS, rad2, MUL, VAR1, 6, MUL, rot, ADD, COS,
                                    bump,
                                    MUL, cy, ADD, ADD
                            },
                            0,
                            camelPi2,
                            60,
                            Rc.PathExpression.LOOP_PATH);
            rc.drawPath(pathId);
        }
        {
            rc.getPainter().setColor(Color.GREEN).commit();
            int pathId =
                    rc.addPathExpression(
                            new float[]{
                                    VAR1, (float) Math.PI, DIV, 1, SUB, rad2, MUL, cx,
                                    ADD
                            },
                            new float[]{
                                    (float) Math.PI,
                                    VAR1,
                                    SUB,
                                    SQUARE,
                                    0.04f,
                                    MUL,
                                    rad2,
                                    MUL,
                                    VAR1,
                                    6,
                                    MUL,
                                    rot,
                                    ADD,
                                    COS,
                                    bump,
                                    MUL,
                                    cy,
                                    ADD,
                                    ADD
                            },
                            0,
                            camelPi2,
                            60,
                            0);
            rc.drawPath(pathId);
        }
        {
            rc.getPainter().setColor(Color.RED).commit();
            int pathId =
                    rc.addPolarPathExpression(
                            new float[]{
                                    rad2, VAR1, 10, MUL, rot, ADD, COS, 20, MUL, ADD
                            },
                            0,
                            camelPi2,
                            60,
                            cx,
                            cy,
                            Rc.PathExpression.LOOP_PATH);
            rc.drawPath(pathId);
        }
        {
            float end =
                    rc.floatExpression(
                            RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                            (float) Math.PI,
                            ADD);
            rc.getPainter()
                    .setColor(Color.YELLOW)
                    .setStrokeCap(Paint.Cap.ROUND)
                    .commit();
            int pathId =
                    rc.addPolarPathExpression(
                            new float[]{
                                    rad2, VAR1, 10, MUL, rot, ADD, COS, 20, MUL, ADD,
                                    20,
                                    ADD
                            },
                            RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                            end,
                            60,
                            cx,
                            cy,
                            0);
            rc.drawPath(pathId);
        }
        {
            float root2 = (float) Math.sqrt(2);
            float s = 0.7f;

            rc.floatExpression(
                    RemoteComposeWriter.TIME_IN_CONTINUOUS_SEC,
                    (float) Math.PI,
                    ADD);
            rc.getPainter()
                    .setColor(Color.WHITE)
                    .setStrokeCap(Paint.Cap.ROUND)
                    .commit();
            int pathId =
                    rc.addPolarPathExpression(
                            new float[]{
                                    rad3, root2, MUL, VAR1, 2, MUL, SIN, ABS, 0.0001f,
                                    ADD,
                                    s, MUL, DIV, 1, 1, s * s, 2, VAR1, MUL, SIN, SQUARE,
                                    0.0001f, ADD, MUL, SUB, SQRT, SUB, SQRT, MUL
                            },
                            1,
                            (float) Math.PI * 2 + 1,
                            60,
                            cx,
                            cy,
                            Rc.PathExpression.LOOP_PATH);
            rc.drawPath(pathId);
        }
        rc.endCanvas();
    }


    /**
     * A demo of a custom Scroller that works with RefreshBugKt::dynamicPaging
     * To achieve paging like behavior
     */
    @SuppressLint("RestrictedApiAndroidX")
    static class CustomScroller implements RecordingModifier.Element {

        public static final int VERTICAL = 0;
        public static final int HORIZONTAL = 1;
        private final float mScrollPosition;
        private final float mTouchPosition;

        int mDirection;
        int mNotches;
        float mPositionId;
        CustomTouch mCustom;
        float mMax;
        int mMode;

        public interface CustomTouch {
            float touch(float max, float notchMax);
        }

        @SuppressLint("RestrictedApiAndroidX")
        CustomScroller(int mode, int direction, float touchPosition, float scrollPosition,
                int notches, float max) {
            this.mDirection = direction;
            mMode = mode;
            mMax = max;
            mNotches = notches;
            mScrollPosition = scrollPosition;
            mTouchPosition = touchPosition;
        }

        @Override
        @SuppressLint("RestrictedApiAndroidX")
        public void write(@NonNull RemoteComposeWriter writer) {
            addModifierCustomScroll(writer, mDirection, mScrollPosition, mTouchPosition, mNotches,
                    mMax);
        }

        @SuppressLint("RestrictedApiAndroidX")
        public void addModifierCustomScroll(RemoteComposeWriter writer,
                int direction, float scrollPosition, float touchPosition, int notches, float max) {
            //float max = this.reserveFloatVariable();
            float notchMax = writer.reserveFloatVariable();
            float touchExpressionDirection =
                    direction != 0 ? RemoteContext.FLOAT_TOUCH_POS_X
                            : RemoteContext.FLOAT_TOUCH_POS_Y;

            ScrollModifierOperation.apply(writer.getBuffer().getBuffer(), direction, scrollPosition,
                    max, notchMax);

            writer.getBuffer().addTouchExpression(
                    Utils.idFromNan(touchPosition),
                    0f,
                    (mMode & 1) == 0 ? 0 : Float.NaN,
                    notches + ((mMode & 1) == 0 ? 0 : 1),
                    0f,
                    3,
                    new float[]{
                            touchExpressionDirection, max, DIV, notches + 1, MUL, -1, MUL
                    },
                    (mMode & 2) == 0 ? TouchExpression.STOP_NOTCHES_EVEN
                            : TouchExpression.STOP_NOTCHES_SINGLE_EVEN,
                    new float[]{notches + ((mMode & 1) == 0 ? 0 : 1)},
                    writer.easing(0.5f, 10f, 0.1f));
            writer.getBuffer().addContainerEnd();
            writer.addDebugMessage("scroll " + touchPosition);
        }
    }

    // =====================================================================================
    // =====================================================================================
    // =====================================================================================

    @SuppressLint("RestrictedApiAndroidX")
    static void graph1(RemoteComposeWriterAndroid rc, float height) {

        rc.startBox(
                new RecordingModifier().fillMaxWidth().height(height),
                BoxLayout.START,
                BoxLayout.START);
        rc.startCanvas(new RecordingModifier().fillMaxSize());
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();
//        float cx = rc.floatExpression(w, 0.5f, MUL);
//        float cy = rc.floatExpression(h, 0.5f, MUL);
        float margin = rc.floatExpression(w, h, MIN, 0.1f, MUL);
        float lineBottom = rc.floatExpression(h, margin, AnimatedFloatExpression.SUB);
        float lineRight = rc.floatExpression(w, margin, AnimatedFloatExpression.SUB);
        rc.getPainter().setColor(Color.GRAY).setStyle(Paint.Style.FILL).commit();
        rc.drawRect(0, 0, w, h);
        float[] data = {2, 3, 5, 12, 3, 1, 8, 4, 2};
        rc.getPainter()
                .setStrokeWidth(10)
                .setColor(Color.DKGRAY)
                .setStyle(Paint.Style.STROKE)
                .commit();
        rc.drawLine(margin, margin, margin, lineBottom);
        rc.drawLine(margin, lineBottom, lineRight, lineBottom);
        float array = rc.addFloatArray(data);
        float max = rc.floatExpression(array, A_MAX);
        float min = rc.floatExpression(array, A_MIN, 1, AnimatedFloatExpression.SUB);
        float len = rc.floatExpression(array, A_LEN);
        float grHeight = rc.floatExpression(h, margin, 2, MUL, AnimatedFloatExpression.SUB);
        float grWidth = rc.floatExpression(w, margin, 2, MUL, AnimatedFloatExpression.SUB);
        float scale = rc.floatExpression(grHeight, max, min, AnimatedFloatExpression.SUB,
                AnimatedFloatExpression.DIV);
        rc.getPainter().setTextSize(60).commit();

        {
            float xstep = rc.floatExpression(grWidth, len, AnimatedFloatExpression.DIV);
            float index = rc.startLoopVar(0, 1f, len);
            float xPos1 = rc.floatExpression(xstep, index, MUL, margin, ADD);
            float xPos2 = rc.floatExpression(xPos1, xstep, ADD);
            float yVal = rc.floatExpression(array, index, A_DEREF);
            float lineTop =
                    rc.floatExpression(
                            grHeight, scale, yVal, min, AnimatedFloatExpression.SUB, MUL,
                            AnimatedFloatExpression.SUB, margin, ADD);
            rc.getPainter()
                    .setColor(Color.BLUE)
                    .setStyle(Paint.Style.FILL)
                    .setStrokeWidth(1)
                    .commit();
            rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
            int textId = rc.createTextFromFloat(yVal, 2, 0, 0);
            rc.getPainter()
                    .setColor(Color.WHITE)
                    .setStyle(Paint.Style.STROKE)
                    .setStrokeWidth(2)
                    .commit();
            rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
            float xPosCenter = rc.floatExpression(xPos1, xPos2, ADD, 2,
                    AnimatedFloatExpression.DIV);
            rc.getPainter().setStyle(Paint.Style.FILL).commit();

            rc.drawTextAnchored(textId, xPosCenter, lineTop, 0, 2, 0);
            rc.endLoop();
        }
        {
            rc.getPainter()
                    .setStrokeWidth(10)
                    .setColor(Color.GREEN)
                    .setStyle(Paint.Style.STROKE)
                    .setStrokeWidth(4)
                    .commit();
            float step = 10;
            float xOff = rc.floatExpression(margin, grWidth, len, AnimatedFloatExpression.DIV, 2,
                    AnimatedFloatExpression.DIV, ADD);
            float xScale = rc.floatExpression(1, grWidth, grWidth, len, AnimatedFloatExpression.DIV,
                    AnimatedFloatExpression.SUB, AnimatedFloatExpression.DIV);
            float yOff =
                    rc.floatExpression(grHeight, scale, min, MUL, AnimatedFloatExpression.SUB,
                            margin, ADD);
            float end = rc.floatExpression(w, margin, AnimatedFloatExpression.SUB);
            float sx1 = rc.startLoopVar(margin, step, end);
            float sx2 = rc.floatExpression(sx1, step, ADD);
            float x1 = rc.floatExpression(sx1, xOff, AnimatedFloatExpression.SUB, xScale, MUL);
            float x2 = rc.floatExpression(sx2, xOff, AnimatedFloatExpression.SUB, xScale, MUL);
            float y1 = rc.floatExpression(yOff, scale, array, x1, A_SPLINE, MUL,
                    AnimatedFloatExpression.SUB);
            float y2 = rc.floatExpression(yOff, scale, array, x2, A_SPLINE, MUL,
                    AnimatedFloatExpression.SUB);
            rc.drawLine(sx1, y1, sx2, y2);
            rc.endLoop();
        }

        rc.endCanvas();
        rc.endBox();

    }
    // =====================================================================================
    // =====================================================================================
    // =====================================================================================


    @SuppressLint("RestrictedApiAndroidX")
    static void graph2(RemoteComposeWriterAndroid rc, float height) {

        rc.startBox(
                new RecordingModifier().fillMaxWidth().height(height),
                BoxLayout.START,
                BoxLayout.START);
        rc.startCanvas(new RecordingModifier().fillMaxSize());
        float w = rc.addComponentWidthValue();
        float h = rc.addComponentHeightValue();
        float margin = rc.floatExpression(w, h, MIN, 0.1f, MUL);
        float lineBottom = rc.floatExpression(h, margin, AnimatedFloatExpression.SUB);
        float lineRight = rc.floatExpression(w, margin, AnimatedFloatExpression.SUB);
        rc.getPainter().setColor(Color.WHITE).setStyle(Paint.Style.FILL).commit();
        rc.drawRect(0, 0, w, h);
        float[] data = {2, 3, 5, 12, 3, 1, 8, 4, 2};

        float array = rc.addFloatArray(data);
        float max = rc.floatExpression(array, A_MAX);
        float min = rc.floatExpression(array, A_MIN, 1, AnimatedFloatExpression.SUB);
        float len = rc.floatExpression(array, A_LEN);
        float grHeight = rc.floatExpression(h, margin, 2, MUL, AnimatedFloatExpression.SUB);
        float grWidth = rc.floatExpression(w, margin, 2, MUL, AnimatedFloatExpression.SUB);
        float scale = rc.floatExpression(grHeight, max, min, AnimatedFloatExpression.SUB,
                AnimatedFloatExpression.DIV);
        rc.getPainter().setTextSize(60).commit();

        float xstep = rc.floatExpression(grWidth, len, AnimatedFloatExpression.DIV);
        float index = rc.startLoopVar(0, 1f, len);
        {
            float xPos1 = rc.floatExpression(xstep, index, MUL, margin, ADD);
            float xPos2 = rc.floatExpression(xPos1, xstep, ADD);
            float yVal = rc.floatExpression(array, index, A_DEREF);
            float lineTop =
                    rc.floatExpression(
                            grHeight, scale, yVal, min, AnimatedFloatExpression.SUB, MUL,
                            AnimatedFloatExpression.SUB, margin, ADD);
            rc.getPainter()
                    .setColor(Color.BLUE)
                    .setStyle(Paint.Style.FILL)
                    .setStrokeWidth(1)
                    .commit();
            rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
            int textId = rc.createTextFromFloat(yVal, 2, 0, 0);
            rc.getPainter()
                    .setColor(Color.WHITE)
                    .setStyle(Paint.Style.STROKE)
                    .setStrokeWidth(2)
                    .commit();
            rc.drawRect(xPos1, lineTop, xPos2, lineBottom);
            float xPosCenter = rc.floatExpression(xPos1, xPos2, ADD, 2,
                    AnimatedFloatExpression.DIV);
            rc.getPainter().setStyle(Paint.Style.FILL).commit();

            rc.drawTextAnchored(textId, xPosCenter, lineTop, 0, 2, 0);
        }
        rc.endLoop();

        {
            rc.getPainter()
                    .setStrokeWidth(10)
                    .setColor(Color.GREEN)
                    .setStyle(Paint.Style.STROKE)
                    .commit();

            float step = 10;
            float xOff = rc.floatExpression(margin, grWidth, len, AnimatedFloatExpression.DIV, 2,
                    AnimatedFloatExpression.DIV, ADD);
            float xScale = rc.floatExpression(1, grWidth, grWidth, len, AnimatedFloatExpression.DIV,
                    AnimatedFloatExpression.SUB, AnimatedFloatExpression.DIV);
            float yOff =
                    rc.floatExpression(grHeight, scale, min, MUL, AnimatedFloatExpression.SUB,
                            margin, ADD);
            float end = rc.floatExpression(w, margin, AnimatedFloatExpression.SUB);
            int path = rc.pathCreate(margin, lineBottom);
            float sx1 = rc.startLoopVar(margin, step, end);
            {
                float x1 = rc.floatExpression(sx1, xOff, AnimatedFloatExpression.SUB, xScale, MUL);
                float y1 =
                        rc.floatExpression(yOff, scale, array, x1, A_SPLINE, MUL,
                                AnimatedFloatExpression.SUB);
                rc.pathAppendLineTo(path, sx1, y1);
            }
            rc.endLoop();
            rc.pathAppendLineTo(path, end, lineBottom);
            rc.pathAppendClose(path);
            rc.getPainter()
                    .setStyle(Paint.Style.FILL)
                    .setLinearGradient(
                            0,
                            0,
                            0,
                            h,
                            new int[]{0xAAFF0000, 0x44FF0000},
                            null,
                            Shader.TileMode.CLAMP)
                    .commit();
            float top = rc.floatExpression(margin, 1, ADD);
            float left = rc.floatExpression(margin, 10, ADD);
            float bottom = rc.floatExpression(lineBottom, 10, AnimatedFloatExpression.SUB);
            float right = rc.floatExpression(end, 20, AnimatedFloatExpression.SUB);
            rc.save();
            rc.clipRect(left, top, right, bottom);
            rc.drawPath(path);
            rc.getPainter()
                    .setStrokeWidth(10)
                    .setShader(0)
                    .setStyle(Paint.Style.STROKE)
                    .setColor(Color.RED)
                    .commit();
            rc.drawPath(path);
            rc.restore();
        }
        // ======= axis
        rc.getPainter()
                .setStrokeWidth(10)
                .setColor(Color.BLACK)
                .setStyle(Paint.Style.STROKE)
                .commit();
        rc.drawLine(margin, margin, margin, lineBottom);
        rc.drawLine(margin, lineBottom, lineRight, lineBottom);
        // ====== wnd axis
        rc.endCanvas();
        rc.endBox();

    }

    // =====================================================================================
    // =====================================================================================
    // =====================================================================================


    @SuppressLint("RestrictedApiAndroidX")
    static void toggle(RemoteComposeWriterAndroid rc, float visFloat, float notVisFloat) {

        rc.startRunActions();
        float notCalc = rc.floatExpression(visFloat, 1f, ADD, 2f, MOD);
        float calc = rc.floatExpression(notVisFloat, 1f, ADD, 2f, MOD);
        ValueFloatExpressionChange refresh1 = new ValueFloatExpressionChange(
                Utils.idFromNan(visFloat), Utils.idFromNan(notCalc));
        ValueFloatExpressionChange refresh2 = new ValueFloatExpressionChange(
                Utils.idFromNan(notVisFloat), Utils.idFromNan(calc));
        rc.addAction(refresh2, refresh1);
        rc.endRunActions();
    }

    @SuppressLint("RestrictedApiAndroidX")
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

}



