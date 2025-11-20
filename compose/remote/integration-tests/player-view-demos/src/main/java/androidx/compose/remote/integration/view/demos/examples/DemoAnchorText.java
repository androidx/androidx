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
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MOD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.PINGPONG;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

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
public class DemoAnchorText {
    @SuppressLint("RestrictedApiAndroidX")
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private DemoAnchorText() {
    }

    /**
     * Test of various computed paths
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RemoteComposeWriter anchoredText() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                PROFILE_ANDROIDX, sPlatform);
        rc.root(
                () -> {
                    rc.box(new RecordingModifier().fillMaxSize().background(Color.DKGRAY).padding(
                            4));


                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 0.5f, MUL);
                    float v1 = rc.floatExpression(cx, 20f, SUB);
                    float v2 = rc.floatExpression(cx, 20f, ADD);
                    float l1 = rc.floatExpression(h, 0.2f, MUL);
                    float l2 = rc.floatExpression(h, 0.4f, MUL);
                    float l3 = rc.floatExpression(h, 0.6f, MUL);
                    float l4 = rc.floatExpression(h, 0.8f, MUL);
                    float l5 = rc.floatExpression(h, 0.9f, MUL);
                    rc.getPainter().setColor(Color.WHITE).commit();
                    rc.drawRect(0, 0, w, h);
                    rc.getPainter().setColor(Color.RED).commit();
                    rc.drawLine(0, l1, w, l1);
                    rc.drawLine(0, l2, w, l2);
                    rc.drawLine(0, l3, w, l3);
                    rc.drawLine(0, l4, w, l4);
                    rc.drawLine(v1, 0, v1, h);
                    rc.drawLine(v2, 0, v2, h);
                    float dur = 10;
                    float sec = Rc.Time.CONTINUOUS_SEC;
                    float t = rc.floatExpression(sec, dur * 3, MOD);
                    float animatX  = rc.floatExpression(sec, 2 , MUL, 2, PINGPONG, 1, SUB);
                    float animatY  = rc.floatExpression(sec, 3 , MUL, 2, PINGPONG, 1, SUB);
                    int flag1 = Rc.TextAnchorMask.MEASURE_EVERY_TIME;
                    int flag2 = Rc.TextAnchorMask.BASELINE_RELATIVE;
                    rc.getPainter().setColor(Color.BLUE).setTextSize(64f).commit();

                    int strId = rc.addText("flip plop");
                    rc.conditionalOperations(Rc.Condition.LT, t, dur, () -> {
                        rc.drawTextAnchored("X Right top X", v1, l1, 1, 1, 0);
                        rc.drawTextAnchored("X Left top X", v2, l1, -1, 1, 0);
                        rc.drawTextAnchored("X Right center X", v1, l2, 1, 0, 0);
                        rc.drawTextAnchored("X Left center X", v2, l2, -1, 0, 0);
                        rc.drawTextAnchored("X Right bottom X", v1, l3, 1, -1, 0);
                        rc.drawTextAnchored("X Left bottom X", v2, l3, -1, -1, 0);
                        rc.drawTextAnchored("X Right baseline X", v1, l4, 1, 0, flag2);
                        rc.drawTextAnchored("X Left baseline X", v2, l4, -1, 0, flag2);
                    });

                    rc.conditionalOperations(Rc.Condition.GT, t, dur, () -> {
                        rc.drawTextAnchored(strId, v1, l1, 1, 1, 0);
                        rc.drawTextAnchored(strId, v1, l2, 1, 0, 0);
                        rc.drawTextAnchored(strId, v1, l3, 1, -1, 0);
                        rc.drawTextAnchored(strId, v1, l4, 1, 0, flag2);
                        rc.conditionalOperations(Rc.Condition.GT, t, dur * 2, () -> {
                            rc.getPainter().setColor(Color.BLUE).setTextSize(128f).commit();
                        });
                        rc.drawTextAnchored(strId, v2, l1, -1, 1, flag1);
                        rc.drawTextAnchored(strId, v2, l2, -1, 0, flag1);
                        rc.drawTextAnchored(strId, v2, l3, -1, -1, flag1);
                        rc.drawTextAnchored(strId, v2, l4, -1, 0, flag2 | flag1);
                        rc.drawTextAnchored(strId, v2, l5, animatX, animatY,  flag1);

                    });
                    rc.getPainter().setColor(Color.BLACK).commit();

                    rc.endCanvas();


                });
        return rc;
    }


}



