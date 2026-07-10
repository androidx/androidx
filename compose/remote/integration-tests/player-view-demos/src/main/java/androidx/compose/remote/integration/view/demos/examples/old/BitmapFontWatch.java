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
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Paint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.ConditionalOperations;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * A watch demo using bitmap fonts.
 */
@SuppressLint("RestrictedApiAndroidX")
public class BitmapFontWatch {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private BitmapFontWatch() {

    }

    /**
     * Creates a watch demo.
     *
     * @param activity the activity context.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter watch1(@NonNull Activity activity) {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7, 0,
                sPlatform);
        rc.root(
                () -> {
                    rc.startBox(
                            new RecordingModifier().fillMaxSize(),
                            BoxLayout.START,
                            BoxLayout.TOP);
                    rc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rc.addComponentWidthValue();
                    float h = rc.addComponentHeightValue();
                    float cx = rc.floatExpression(w, 2, DIV);
                    float cy = rc.floatExpression(h, 2, DIV);
                    float rad = rc.floatExpression(w, h, MIN, 2F, DIV);
                    float rad2 = rc.floatExpression(rad, 0.8f, MUL);
                    float topY = rc.floatExpression(cy, rad, SUB);

                    float edgeY = rc.floatExpression(cy, rad2, SUB);
                    float edge2Y = rc.floatExpression(cy, rad2, SUB);

                    rc.getPainter().setColor(0xFF433833).commit();
                    rc.drawCircle(cx, cy, rad);
                    rc.getPainter()
                            .setColor(0xFF8a6250)
                            .setStrokeWidth(2f)
                            .setStyle(Paint.Style.STROKE)
                            .commit();
                    rc.drawCircle(cx, cy, rad);
                    rc.drawCircle(cx, cy, rad2);
                    float fontSize = rc.floatExpression(rad, rad2, SUB, 0.6f, MUL);
                    float i = rc.createFloatId();
                    rc.save();
                    rc.loop(
                            Utils.idFromNan(i),
                            0f,
                            1,
                            60,
                            () -> {
                                float k = rc.floatExpression(i, 5, MOD);
                                rc.conditionalOperations(ConditionalOperations.TYPE_GT, k, 0.1f);
                                rc.drawLine(cx, topY, cx, edgeY);
                                rc.endConditionalOperations();

                                rc.rotate(6, cx, cy);
                            });
                    rc.restore();
                    float indexLen = 100;

                    float textCenter = rc.floatExpression(edge2Y, topY, ADD, 2, DIV);
                    rc.getPainter()
                            .setColor(0xFF8a6250)
                            .setTextSize(fontSize)
                            .setStyle(Paint.Style.FILL)
                            .commit();
                    String[] roman =
                            new String[]{
                                    "XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX",
                                    "X",
                                    "XI"
                            };
                    rc.save();
                    for (int j = 0; j < 12; j++) {
                        rc.drawTextAnchored(roman[j], cx, textCenter, 0, 0, 0);

                        rc.rotate(30, cx, cy);
                    }
                    rc.restore();

                    rc.getPainter().setColor(0xFFce7a53).commit();
                    float leftIndex = rc.floatExpression(cx, 10, SUB);
                    float rightIndex = rc.floatExpression(cx, 10, ADD);
                    float bottomIndex = rc.floatExpression(edge2Y, indexLen, ADD);
                    float topIndex = rc.floatExpression(edge2Y, 20, ADD);
                    float dotY = rc.floatExpression(edge2Y, 10, ADD);
                    rc.save();
                    for (int j = 0; j < 12; j++) {
                        if (!(j == 0 || j == 3)) {
                            rc.drawRect(leftIndex, topIndex, rightIndex, bottomIndex);
                        }
                        rc.drawCircle(cx, dotY, 5);
                        rc.rotate(30, cx, cy);
                    }
                    rc.restore();

                    rc.endCanvas();
                    rc.endBox();
                });
        return rc;
    }
}
