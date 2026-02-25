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
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for using textures and shaders.
 */
@SuppressLint("RestrictedApiAndroidX")
public class TextureDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private TextureDemo() {
    }

    /**
     * Demo showing a basic texture shader.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter basicTexture() {

        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, "DClock", 7, 0,
                sPlatform);
        rc.floatExpression(Rc.Time.CONTINUOUS_SEC);

        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());

            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL);
            float cy = rc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL);
            float rad = rc.floatExpression(cx, cy, MIN);
            int texture = rc.addBitmap(createBall(64, 64, 0x10101));
            int texture2 = rc.addBitmap(createBall(64, 64, 0x00101));
            rc.drawScaledBitmap(texture, 0, 0, 64, 64, 0, 0, w, h, Rc.ImageScale.FILL_BOUNDS, 0,
                    "");
            rc.getPainter().setColor(Color.GREEN).commit();
            rc.getPainter().setTextureShader(texture2, (short) 1, (short) 1, (short) 0,
                    (short) 0).commit();
            rc.drawCircle(cx, cy, rad);
            rc.endCanvas();
        });
        return rc;
    }

    /**
     * Demo showing a clock with hands rendered using simplex noise textures.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter textureClock() {

        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, "DClock", 6, 0,
                sPlatform);
        rc.floatExpression(Rc.Time.CONTINUOUS_SEC);

        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());

            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL);
            float cy = rc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL);
            float rad = rc.floatExpression(cx, cy, MIN);
            DemoUtils.SimplexNoiseGenerator simplex = new DemoUtils.SimplexNoiseGenerator(
                    256229876543L);
            int texture1 = rc.addBitmap(simplex.generateSimpleNoise2D(128, 32, 0.4, 70, 80));

            int texture2 = rc.addBitmap(simplex.generateSimpleNoise2D(64, 64, 0.3, 100, 130));
            int texture3 = rc.addBitmap(simplex.generateSimpleNoise2D(64, 64, 0.3, 50, 150));
            int texture4 = rc.addBitmap(simplex.generateSimpleNoise2D(64, 64, 0.3, 50, 200));

            rc.getPainter().setTextureShader(texture1, (short) 1, (short) 1, (short) 0,
                    (short) 0).commit();
            rc.drawCircle(cx, cy, rad);

            float sec = rc.floatExpression(Rc.Time.CONTINUOUS_SEC, 60, MOD, 6, MUL);
            float min = rc.floatExpression(Rc.Time.TIME_IN_MIN, 60, MOD, 6, MUL);
            float hour = rc.floatExpression(Rc.Time.TIME_IN_HR, 24, MOD, 360 / 24f, MUL);

            float hrWidth = rc.floatExpression(rad, 30, DIV);
            float hrLength = rc.floatExpression(rad, 2, DIV);
            float hrL = rc.floatExpression(cx, hrWidth, SUB);
            float hrR = rc.floatExpression(cx, hrWidth, ADD);
            float hrT = rc.floatExpression(cy, hrLength, SUB);
            float hrB = rc.floatExpression(cy, 2, ADD);
            rc.getPainter().setTextureShader(texture2, (short) 1, (short) 1, (short) 0,
                    (short) 0).commit();

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
            rc.getPainter().setTextureShader(texture3, (short) 1, (short) 1, (short) 0,
                    (short) 0).commit();
            rc.save();
            rc.rotate(min, cx, cy);
            rc.drawRoundRect(minL, minT, minR, minB, 30, 30);
            rc.restore();
            rc.drawCircle(cx, cy, 30);
            rc.getPainter().setTextureShader(texture4, (short) 1, (short) 1, (short) 0,
                    (short) 0).commit();
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

    /**
     * Demo showing a clock with various tiling and filtering options for textures.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter textureClockTest() {

        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, "DClock", 6, 0,
                sPlatform);

        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());

            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL);
            float cy = rc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL);
            float rad = rc.floatExpression(cx, cy, MIN);

            Bitmap[] bitmap = new Bitmap[4];
            int[] texture = new int[4];
            int[] dimX = {32, 64, 32, 16};
            int[] dimY = {64, 16, 64, 16};

            for (int i = 0; i < bitmap.length; i++) {
                bitmap[i] = Bitmap.createBitmap(dimX[i], dimY[i], Bitmap.Config.ARGB_8888);
                int[] color = new int[dimX[i] * dimY[i]];
                for (int k = 0; k < color.length; k++) {

                    color[k] = (k % dimX[i] >= k / dimX[i]) ? (0xFF000000 | 0x10101 * i * 85)
                            : (0xFF000000 | (0xFF << (8 * i)));
                }
                bitmap[i].eraseColor(0xFFFFFFFF);
                bitmap[i].setPixels(color, 0, dimX[i], 0, 0, dimX[i], dimY[i]);
                texture[i] = rc.addBitmap(bitmap[i]);
            }

            rc.getPainter().setTextureShader(texture[0], Rc.Texture.TILE_MIRROR,
                    Rc.Texture.TILE_REPEAT, Rc.Texture.FILTER_DEFAULT, (short) 0).commit();
            rc.drawCircle(cx, cy, rad);

            float sec = rc.floatExpression(25, 60, MOD, 6, MUL);
            float min = rc.floatExpression(49, 60, MOD, 6, MUL);
            float hour = rc.floatExpression(2, 24, MOD, 360 / 24f, MUL);

            float hrWidth = rc.floatExpression(rad, 30, DIV);
            float hrLength = rc.floatExpression(rad, 2, DIV);
            float hrL = rc.floatExpression(cx, hrWidth, SUB);
            float hrR = rc.floatExpression(cx, hrWidth, ADD);
            float hrT = rc.floatExpression(cy, hrLength, SUB);
            float hrB = rc.floatExpression(cy, 2, ADD);
            rc.getPainter().setTextureShader(texture[1], Rc.Texture.TILE_MIRROR,
                    Rc.Texture.TILE_MIRROR, (short) 0, (short) 0).commit();

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
            rc.getPainter().setTextureShader(texture[2], Rc.Texture.TILE_REPEAT,
                    Rc.Texture.TILE_REPEAT, (short) 0, (short) 0).commit();
            rc.save();
            rc.rotate(min, cx, cy);
            rc.drawRoundRect(minL, minT, minR, minB, 30, 30);
            rc.restore();
            rc.getPainter().setShader(0).setColor(Color.GRAY).commit();
            rc.drawCircle(cx, cy, 60);
            rc.getPainter().setTextureShader(texture[3], (short) 1, (short) 1,
                    Rc.Texture.FILTER_NEAREST, (short) 0).commit();
            float secWidth = rc.floatExpression(rad, 10, DIV);
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

    static Bitmap createBall(int width, int height, int colorBits) {
        Bitmap ball = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

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
            data[i] = 0xFF000000 + colorBits * bright;
        }
        ball.setPixels(data, 0, w, 0, 0, w, h);
        return ball;
    }
}
