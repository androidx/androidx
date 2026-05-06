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
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.FLOOR;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo showing the use of the wakeIn API for scheduled refreshes.
 */
@SuppressLint("RestrictedApiAndroidX")
public class WakeDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private WakeDemo() {
    }

    /**
     * Creates a clock demo that uses wakeIn to update every 15 seconds.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter wakeClock() {

        RemoteComposeWriterAndroid rc =
                new RemoteComposeWriterAndroid(300, 300, "DClock", 7, RcProfiles.PROFILE_ANDROIDX,
                        sPlatform);

        rc.root(
                () -> {
                    rc.startCanvas(new RecordingModifier().fillMaxSize());

                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL);
                    float cy = rc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL);
                    float rad = rc.floatExpression(cx, cy, MIN);
                    DemoUtils.SimplexNoiseGenerator simplex =
                            new DemoUtils.SimplexNoiseGenerator(256229876543L);
                    int texture1 =
                            rc.addBitmap(simplex.generateSimpleNoise2D(128, 128, 1f, 120, 126));

                    int texture2 = rc.addBitmap(
                            simplex.generateSimpleNoise2D(64, 64, 0.3, 49, 50));
                    int texture3 =
                            rc.addBitmap(simplex.generateSimpleNoise2D(64, 64, 0.3, 50, 150));
                    int texture4 =
                            rc.addBitmap(simplex.generateSimpleNoise2D(64, 64, 0.3, 50, 200));

                    rc.getPainter()
                            .setTextureShader(texture1, (short) 1, (short) 1, (short) 0, (short) 0)
                            .commit();
                    rc.drawCircle(cx, cy, rad);
                    rc.wakeIn(15);
                    float secRaw = rc.timeAttribute(0, Rc.TimeAttributes.TIME_IN_SEC);
                    float sec =
                            rc.floatExpression(
                                    rc.exp(secRaw, 15, DIV, FLOOR, 15, MUL, 6, MUL),
                                    rc.anim(0.5f, Rc.Animate.CUBIC_LINEAR, null, Float.NaN, 360));
                    float min = rc.floatExpression(Rc.Time.TIME_IN_MIN, 60, MOD, 6, MUL);
                    float hour = rc.floatExpression(Rc.Time.TIME_IN_HR, 24, MOD, 360 / 24f, MUL);
                    rc.addDebugMessage("------------------------------------", 0f, 0);
                    rc.addDebugMessage("center x", cx, Rc.Debug.SHOW_USAGE);

                    float hrWidth = rc.floatExpression(rad, 30, DIV);
                    float hrLength = rc.floatExpression(rad, 2, DIV);
                    float hrL = rc.floatExpression(cx, hrWidth, SUB);
                    float hrR = rc.floatExpression(cx, hrWidth, ADD);
                    float hrT = rc.floatExpression(cy, hrLength, SUB);
                    float hrB = rc.floatExpression(cy, 2, ADD);
                    rc.getPainter()
                            .setTextureShader(texture2, (short) 1, (short) 1, (short) 0, (short) 0)
                            .commit();

                    rc.save();
                    rc.rotate(hour, cx, cy);
                    rc.drawRoundRect(hrL, hrT, hrR, hrB, 30, 30);
                    rc.restore();

                    float minWidth = rc.floatExpression(rad, 30, DIV);
                    float minLength = rc.floatExpression(rad, 0.8f, MUL);
                    float minL = rc.floatExpression(cx, minWidth, SUB);
                    float minR = rc.floatExpression(cx, minWidth, ADD);
                    float minT = rc.floatExpression(cy, minLength, SUB);
                    float minB = rc.floatExpression(cy, 2, ADD);
                    rc.getPainter()
                            .setTextureShader(texture3, (short) 1, (short) 1, (short) 0, (short) 0)
                            .commit();
                    rc.save();
                    rc.rotate(min, cx, cy);
                    rc.drawRoundRect(minL, minT, minR, minB, 30, 30);
                    rc.restore();
                    rc.drawCircle(cx, cy, 30);
                    rc.getPainter()
                            .setTextureShader(texture4, (short) 1, (short) 1, (short) 0, (short) 0)
                            .commit();
                    float secWidth = rc.floatExpression(rad, 70, DIV);
                    float secLength = rc.floatExpression(rad, 0.8f, MUL);
                    float secL = rc.floatExpression(cx, secWidth, SUB);
                    float secR = rc.floatExpression(cx, secWidth, ADD);
                    float secT = rc.floatExpression(cy, secLength, SUB);
                    float secB = rc.floatExpression(cy, 2, ADD);

                    rc.save();
                    rc.rotate(sec, cx, cy);
                    rc.drawRoundRect(secL, secT, secR, secB, 30, 30);
                    rc.restore();
                    rc.endCanvas();
                });
        return rc;
    }
}
