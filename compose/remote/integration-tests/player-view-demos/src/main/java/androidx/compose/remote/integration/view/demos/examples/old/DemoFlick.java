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
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.TouchExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo for flick-based touch interactions.
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoFlick {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private DemoFlick() {
    }

    /**
     * Creates a flick demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter flickTest() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        rc.root(
                () -> {
                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    rc.drawRect(0, 0, w, h);
                    rc.getPainter().setColor(Color.GRAY).commit();
                    float cx = rc.floatExpression(w, 0.5f, MUL);
                    float cy = rc.floatExpression(h, 0.5f, MUL);

                    float top = 20f;
                    float bottom = rc.floatExpression(h, 20, SUB);
                    float left = rc.floatExpression(cx, 15f, SUB);
                    float right = rc.floatExpression(cy, 15f, ADD);
                    rc.getPainter().setColor(Color.GRAY).commit();
                    rc.drawRoundRect(left, top, right, bottom, 2f, 2f);
                    int clicks = 5;
                    rc.getPainter().setColor(Color.DKGRAY).commit();
                    for (int i = 0; i <= clicks; i++) {
                        float t1 =
                                rc.floatExpression(
                                        bottom,
                                        top,
                                        i / (float) clicks,
                                        Rc.FloatExpression.LERP,
                                        2,
                                        SUB);
                        float t2 =
                                rc.floatExpression(
                                        bottom,
                                        top,
                                        i / (float) clicks,
                                        Rc.FloatExpression.LERP,
                                        2,
                                        ADD);
                        rc.drawRoundRect(left, t1, right, t2, 2f, 2f);
                    }
                    float yPos =
                            rc.addTouch(
                                    top,
                                    top,
                                    bottom,
                                    TouchExpression.STOP_NOTCHES_EVEN,
                                    0f,
                                    0,
                                    new float[]{clicks},
                                    null,
                                    RemoteContext.FLOAT_TOUCH_POS_Y,
                                    1,
                                    MUL);
                    // rc.addDebugMessage(">>>>> [" + Utils.idFromNan(pos) + "]", pos);

                    rc.getPainter().setColor(Color.GREEN).setTextSize(64f).commit();
                    rc.drawTextAnchored(rc.createTextFromFloat(yPos, 3, 1, 0), 0, 0, -1, 1, 0);

                    rc.drawCircle(cx, yPos, 20);
                    rc.endCanvas();
                });
        return rc;
    }
}
