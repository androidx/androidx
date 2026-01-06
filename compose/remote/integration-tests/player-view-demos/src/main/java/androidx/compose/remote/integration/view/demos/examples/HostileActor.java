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
import static androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC;
import static androidx.compose.remote.core.operations.DrawTextAnchored.ANCHOR_MONOSPACE_MEASURE;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MAX;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MOD;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.MUL;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAND;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAND_SEED;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

@SuppressLint("RestrictedApiAndroidX")
public class HostileActor {
    static @NonNull RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private HostileActor() {
    }

    /**
     * Demo image
     * Create a pattern of ever increasing drawImages to the screen.
     *
     * @return RemoteComposeWriter
     */
    @SuppressLint("RestrictedApiAndroidX")
    public @NonNull
    static RemoteComposeWriter demoImage() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(500, 500, "sd", 6,
                PROFILE_ANDROIDX, sPlatform);

        rcDoc.setRootContentBehavior(RootContentBehavior.NONE, RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float count = rcDoc.floatExpression(6990, RAND_SEED, FLOAT_CONTINUOUS_SEC, 200, MUL, 100,
                MAX, 2000, MOD);

        float value = count; // rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC);
        rcDoc.getPainter().setColor(Color.BLUE).setTextSize(32.f).commit();
        // int image = rcDoc.addBitmapRaw(512,512, TYPE_PNG_8888,
        // ENCODING_URL,url.getBytes(StandardCharsets.UTF_8));
        float index = rcDoc.createFloatId();
        rcDoc.loop(Utils.idFromNan(index), 0f, 1f, count, () -> {
            float rx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, RAND, MUL, 25, SUB);
            float ry = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, RAND, MUL, 25, SUB);
            rcDoc.save();
            rcDoc.translate(rx, ry);
            rcDoc.drawScaledBitmap(initBall(0x10101), 0, 0, 50, 50, 0, 0, 50, 50,
                    RemoteComposeWriter.IMAGE_SCALE_FIT, 3f, "test image");
            rcDoc.restore();
        });
        int tid = rcDoc.createTextFromFloat(value, 5, 2, 0);
        rcDoc.getPainter().setTextSize(32.f).setColor(Color.RED).commit();
        rcDoc.drawTextAnchored(tid, cx, 20, 0, 0, ANCHOR_MONOSPACE_MEASURE);

        return rcDoc;
    }

    /**
     * Color hostile actor
     * Create a pattern of ever increasing drawImages to the screen
     */
    @SuppressLint("RestrictedApiAndroidX")
    public @NonNull
    static RemoteComposeWriter demoImageColor() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(500, 500, "sd", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE, RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float count = rcDoc.floatExpression(6990, RAND_SEED, FLOAT_CONTINUOUS_SEC, 200, MUL, 20,
                MAX, 1000, MOD);

        float value = count; // rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC);
        rcDoc.getPainter().setColor(Color.BLUE).setTextSize(32.f).commit();
        // int image = rcDoc.addBitmapRaw(512,512, TYPE_PNG_8888,
        // ENCODING_URL,url.getBytes(StandardCharsets.UTF_8));
        Bitmap redBall = initBall(0x10000);
        Bitmap greenBall = initBall(0x100);
        Bitmap blueBall = initBall(0x1);
        int redBallId = rcDoc.addBitmap(redBall);
        int greenBallId = rcDoc.addBitmap(greenBall);
        int blueBallId = rcDoc.addBitmap(blueBall);
        float index = rcDoc.createFloatId();
        rcDoc.loop(Utils.idFromNan(index), 0f, 1f, count, () -> {
            float rx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, RAND, MUL, 25, SUB);
            float ry = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, RAND, MUL, 25, SUB);
            rcDoc.save();
            rcDoc.translate(rx, ry);
            rcDoc.drawScaledBitmap(redBallId, 0, 0, 50, 50, 0, 0, 50, 50,
                    RemoteComposeWriter.IMAGE_SCALE_FIT, 3f, "test image");
            rcDoc.restore();
            rx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, RAND, MUL);
            ry = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, RAND, MUL);
            rcDoc.save();
            rcDoc.translate(rx, ry);
            rcDoc.drawScaledBitmap(greenBallId, 0, 0, 50, 50, 0, 0, 50, 50,
                    RemoteComposeWriter.IMAGE_SCALE_FIT, 3f, "test image");
            rcDoc.restore();
            rx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, RAND, MUL);
            ry = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, RAND, MUL);
            rcDoc.save();
            rcDoc.translate(rx, ry);
            rcDoc.drawScaledBitmap(blueBallId, 0, 0, 50, 50, 0, 0, 50, 50,
                    RemoteComposeWriter.IMAGE_SCALE_FIT, 3f, "test image");
            rcDoc.restore();
        });
        int tid = rcDoc.createTextFromFloat(value, 5, 2, 0);
        rcDoc.getPainter().setTextSize(32.f).setColor(Color.RED).commit();
        rcDoc.drawTextAnchored(tid, cx, 20, 0, 0, ANCHOR_MONOSPACE_MEASURE);

        rcDoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.STROKE).setStrokeWidth(
                3f).commit();
        rcDoc.drawRoundRect(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT, 25, 25);

        return rcDoc;
    }

    /**
     * create a bitmap
     *
     * @param color color of the ball
     */
    @SuppressLint("RestrictedApiAndroidX")
    static @NonNull Bitmap initBall(int color) {
        Bitmap ball = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        int w = ball.getWidth();
        int h = ball.getHeight();
        float cx = w / 2;
        float cy = h / 2;
        float radius = cx * 0.9f;
        float radius2 = radius * radius;
        int[] data = new int[w * h];
        for (int i = 0; i < data.length; i++) {
            int x = i % w;
            int y = i / w;
            float dx = x - cx;
            float dy = y - cy;
            float dist2 = dx * dx + dy * dy;
            if (dist2 > radius2) {
                continue;
            }
            float norm2 = radius * radius - dist2;
            int bright = (int) (norm2 * 255 / radius2);
            data[i] = 0x33000000 + color * bright;
        }
        ball.setPixels(data, 0, w, 0, 0, w, h);
        return ball;
    }
}
