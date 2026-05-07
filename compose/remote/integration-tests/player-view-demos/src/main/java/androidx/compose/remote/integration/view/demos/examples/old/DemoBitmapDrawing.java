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

import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.annotation.SuppressLint;
import android.graphics.BlendMode;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for bitmap-based drawing operations.
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoBitmapDrawing {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private DemoBitmapDrawing() {
    }
    /**
     * Demo showing drawing on a bitmap and then rendering it multiple times.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter bitDraw1() {
        RemoteComposeWriterAndroid rc =
                new RemoteComposeWriterAndroid(500, 500, "sd", 7, RcProfiles.PROFILE_ANDROIDX,
                        sPlatform);
        rc.root(
                () -> {
                    rc.startBox(
                            new RecordingModifier().fillMaxSize(),
                            BoxLayout.START,
                            BoxLayout.START);
                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 2, DIV);
                    float cy = rc.floatExpression(h, 2, DIV);
                    float rad = rc.floatExpression(w, h, MIN, 2F, DIV);
                    int id = rc.createBitmap(256, 256);
                    rc.getPainter().setColor(Color.GRAY).commit();

                    rc.drawCircle(cx, cy, rad);

                    rc.drawOnBitmap(id, 0, 0);
                    rc.save();
                    float angle = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 30, MUL);
                    rc.rotate(angle, 128, 128);
                    rc.getPainter().setColor(Color.RED).commit();
                    rc.drawCircle(128, 128, 64);
                    rc.getPainter().setColor(Color.GREEN).commit();
                    rc.save();
                    rc.scale(0.5f, 2, 128, 128);
                    rc.drawCircle(128, 128, 64);
                    rc.restore();
                    rc.save();
                    rc.scale(2, 0.5f, 128, 128);
                    rc.getPainter().setColor(Color.BLUE).commit();
                    rc.drawCircle(128, 128, 64);
                    rc.restore();
                    rc.restore();
                    rc.drawOnBitmap(0, 0, 0);

                    for (int i = 0; i < 64; i++) {
                        int n = 8;
                        float top = ((int) (i / n)) * 150f;
                        float bottom = ((int) (i / n)) * 150f + 128f;
                        float left = (i % n) * 150f;
                        float right = (i % n) * 150f + 128;
                        rc.drawScaledBitmap(
                                id,
                                0,
                                0,
                                256,
                                256,
                                left,
                                top,
                                right,
                                bottom,
                                Rc.ImageScale.FIT,
                                0,
                                "");
                    }

                    // rc.drawCircle(cx, cy, rad);
                    rc.endCanvas();
                    rc.endBox();
                });
        return rc;
    }

    /**
     * Demo showing drawing on a bitmap with blend modes.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter bitDraw2() {
        RemoteComposeWriterAndroid rc =
                new RemoteComposeWriterAndroid(500, 500, "sd", 7, RcProfiles.PROFILE_ANDROIDX,
                        sPlatform);
        rc.root(
                () -> {
                    rc.startBox(
                            new RecordingModifier().fillMaxSize(),
                            BoxLayout.START,
                            BoxLayout.START);
                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 2, DIV);
                    float cy = rc.floatExpression(h, 2, DIV);
                    float rad = rc.floatExpression(w, h, MIN, 2F, DIV);
                    int id = rc.createBitmap(256, 256);
                    rc.getPainter().setColor(Color.GRAY).commit();
                    rc.drawCircle(cx, cy, rad);
                    rc.getPainter()
                            .setColor(Color.TRANSPARENT)
                            .setBlendMode(BlendMode.SRC_OUT)
                            .commit();
                    rc.drawCircle(cx, cy, 400);
                    rc.drawOnBitmap(id, 0, 0xFFFFFF00);
                    rc.getPainter()
                            .setColor(Color.TRANSPARENT)
                            .setBlendMode(BlendMode.SRC_OUT)
                            .commit();
                    rc.drawCircle(128, 128, 128);
                    rc.drawOnBitmap(0);
                    rc.getPainter().setColor(0xFF000000).setBlendMode(BlendMode.SRC_OVER).commit();

                    for (int i = 0; i < 64; i++) {
                        int n = 8;
                        float top = ((int) (i / n)) * 150f;
                        float bottom = ((int) (i / n)) * 150f + 128f;
                        float left = (i % n) * 150f;
                        float right = (i % n) * 150f + 128;
                        rc.drawScaledBitmap(
                                id,
                                0,
                                0,
                                256,
                                256,
                                left,
                                top,
                                right,
                                bottom,
                                Rc.ImageScale.FIT,
                                0,
                                "");
                    }

                    // rc.drawCircle(cx, cy, rad);
                    rc.endCanvas();
                    rc.endBox();
                });
        return rc;
    }
}
