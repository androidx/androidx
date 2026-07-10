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

import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo showing different image scaling types.
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoImage1 {
    private DemoImage1() {
    }

    /**
     * Creates an image demo with the specified scaling type.
     *
     * @param type the image scaling type.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter demoImage(int type) {
        AndroidxRcPlatformServices platform = new AndroidxRcPlatformServices();

        Bitmap image = createImage(100, 300, true);
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(400, 400, "Clock", 6, 0,
                platform);
        rcDoc.setRootContentBehavior(
                RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FILL_BOUNDS);
        String str = typeToString(type);
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        rcDoc.getPainter().setColor(Color.BLUE).setTextSize(32.f).commit();
        rcDoc.drawTextAnchored(str, cx, 20, 0, 0, 0);
        rcDoc.drawScaledBitmap(
                image,
                0,
                0,
                100,
                300,
                0,
                40,
                RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT,
                type,
                1f,
                "test image");
        rcDoc.getPainter()
                .setColor(Color.RED)
                .setStyle(Paint.Style.STROKE)
                .setStrokeWidth(3f)
                .commit();
        rcDoc.drawRect(0, 40, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        return rcDoc;
    }

    /**
     * Creates a test image with a complex pattern
     *
     * @param tw        width of image
     * @param th        height of the image
     * @param darkTheme draw in a dark theme
     * @return Bitmap
     */
    static Bitmap createImage(int tw, int th, boolean darkTheme) {
        Bitmap image = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();

        Canvas canvas = new Canvas(image);
        paint.setColor(Color.GRAY);
        canvas.drawRect(0, 0, tw, th, paint);
        paint.setStrokeWidth(3f);
        if (darkTheme) {
            paint.setColor(Color.BLACK);
        } else {
            paint.setColor(Color.WHITE);
        }

        canvas.drawRect(tw / 3, tw / 3, tw * 2 / 3, th * 2 / 3, paint);
        paint.setColor(Color.RED);
        canvas.drawLine(0f, 0f, tw, th, paint);
        canvas.drawLine(0f, th, tw, 0f, paint);
        paint.setColor(Color.BLUE);
        canvas.drawLine(300f, 0f, 300f, 600f, paint);
        canvas.drawLine(0f, 300f, 600f, 300f, paint);
        String text = "R";
        Rect bounds = new Rect();
        paint.setAntiAlias(true);

        paint.setShader(
                new RadialGradient(
                        tw / 2f, th / 2f, tw / 2f, Color.WHITE, Color.BLUE,
                        Shader.TileMode.CLAMP));

        paint.setTextSize(Math.min(tw, th));
        paint.getTextBounds(text, 0, text.length(), bounds);
        paint.setTextAlign(Paint.Align.CENTER);

        canvas.drawText(text, tw / 2f, (th - (paint.descent() + paint.ascent())) / 2f, paint);
        return image;
    }

    private static String typeToString(int type) {
        String[] typeString = {
                "none",
                "inside",
                "fill_width",
                "fill_height",
                "fit",
                "crop",
                "fill_bounds",
                "fixed_scale"
        };
        return typeString[type];
    }
}
