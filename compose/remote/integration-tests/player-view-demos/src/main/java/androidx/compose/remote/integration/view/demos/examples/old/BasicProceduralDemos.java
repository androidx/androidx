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

import static androidx.compose.remote.core.RemoteComposeBuffer.PAD_AFTER_ZERO;
import static androidx.compose.remote.creation.Rc.FloatExpression.ABS;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;
import static androidx.compose.remote.creation.RemoteComposeWriter.hTag;
import static androidx.compose.remote.creation.RemoteComposeWriter.map;

import static java.lang.Math.PI;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.RemotePath;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * A collection of basic procedural drawing demos using RemoteCompose.
 */
@SuppressLint("RestrictedApiAndroidX")
public class BasicProceduralDemos {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();
    private BasicProceduralDemos() {
    }
    /**
     * Simple demo drawing a circle.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simple1() {
        RemoteComposeWriter rcDoc = new RemoteComposeWriter(600, 600, "Clock", 6, 0, sPlatform);
        rcDoc.drawCircle(150, 150, 150);
        return rcDoc;
    }

    /**
     * Simple demo drawing an oval that fits the window.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simple2() {
        RemoteComposeWriter rcDoc = new RemoteComposeWriter(300, 300, "Clock", 6, 0, sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FIT);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        return rcDoc;
    }

    /**
     * Simple demo drawing an oval with a click area.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simple3() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        rcDoc.getPainter().setColor(Color.RED).commit();
        rcDoc.addClickArea(232, "foo", 0, 0, 300, 300, "bar");
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        return rcDoc;
    }

    /**
     * Simple demo drawing a rounded rectangle with an animated radius.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simple4() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        rcDoc.getPainter().setColor(Color.RED).commit();
        float rad = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 100, MUL, 100, MOD,
                50, SUB, ABS);
        rcDoc.drawRoundRect(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT, rad, rad);
        return rcDoc;
    }

    /**
     * Simple demo drawing an oval with animated scaling.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simple5() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        rcDoc.getPainter().setColor(Color.RED).commit();
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();
        return rcDoc;
    }

    /**
     * Slow Refresh Clock demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simpleClockSlow() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(sPlatform, 6,
                hTag(Header.DOC_WIDTH, 300), hTag(Header.DOC_HEIGHT, 300),
                hTag(Header.DOC_CONTENT_DESCRIPTION, "slow_clock"),
                hTag(Header.DOC_DESIRED_FPS, 5));
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float angle = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 360, MUL, 360,
                MOD);
        rcDoc.getPainter().setColor(Color.BLACK).commit();
        rcDoc.drawRoundRect(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT, RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.getPainter().setColor(Color.GREEN).setStrokeWidth(10f).commit();

        rcDoc.rotate(angle, cx, cy);
        rcDoc.drawLine(cx, cy, cx, 10);
        return rcDoc;
    }

    /**
     * Fast Refresh Clock demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simpleClockFast() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(sPlatform, 6,
                hTag(Header.DOC_WIDTH, 300), hTag(Header.DOC_HEIGHT, 300),
                hTag(Header.DOC_CONTENT_DESCRIPTION, "slow_clock"),
                hTag(Header.DOC_DESIRED_FPS, 120));
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float angle = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 360, MUL, 360,
                MOD);
        rcDoc.getPainter().setColor(Color.BLACK).commit();

        rcDoc.drawRoundRect(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT, RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.getPainter().setColor(Color.GREEN).setStrokeWidth(10f).commit();

        rcDoc.rotate(angle, cx, cy);
        rcDoc.drawLine(cx, cy, cx, 10);
        return rcDoc;
    }

    /**
     * Demo showing centered text with animated background.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter centerText1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        rcDoc.getPainter().setColor(Color.RED).setTextSize(64f).commit();
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();
        float count = rcDoc.floatExpression(99f, RemoteContext.FLOAT_TIME_IN_SEC, 100, MOD, SUB);
        int textId = rcDoc.createTextFromFloat(count, 3, 0, PAD_AFTER_ZERO);
        float textWidth = rcDoc.textMeasure(textId, 0);
        float textHeight = rcDoc.textMeasure(textId, 1);
        float left = rcDoc.floatExpression(cx, textWidth, 2, DIV, SUB);
        float top = rcDoc.floatExpression(cy, textHeight, 2, DIV, SUB);
        float bottom = rcDoc.floatExpression(top, textHeight, ADD);
        float right = rcDoc.floatExpression(left, textWidth, ADD);
        rcDoc.getPainter().setStyle(Paint.Style.STROKE).setStrokeWidth(2f).setColor(
                Color.BLACK).commit();

        rcDoc.drawRect(left, top, right, bottom);
        rcDoc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();

        rcDoc.drawTextAnchored(textId, cx, cy, 0, 0, 0);

        return rcDoc;
    }

    /**
     * Demo showing the API level version.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter version() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        rcDoc.getPainter().setColor(Color.RED).commit();
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();
        rcDoc.getPainter().setColor(Color.BLACK).setTextSize(32).commit();
        int id = rcDoc.createTextFromFloat(RemoteContext.FLOAT_API_LEVEL, 2, 2, 0);
        rcDoc.drawTextAnchored("Version", cx, cy, 0, -2, 0);
        rcDoc.drawTextAnchored(id, cx, cy, 0, 2, 0);
        return rcDoc;
    }

    /**
     * Demo showing data map lookup.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter lookUp1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);
        rcDoc.getPainter().setColor(Color.RED).setTextSize(64f).commit();
        int map = rcDoc.addDataMap(map("First", "John"), map("Last", "David"), map("DOB", 32));
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();
        int textId = rcDoc.mapLookup(map, "First");
        float textWidth = rcDoc.textMeasure(textId, 0);
        float textHeight = rcDoc.textMeasure(textId, 1);
        float left = rcDoc.floatExpression(cx, textWidth, 2, DIV, SUB);
        float top = rcDoc.floatExpression(cy, textHeight, 2, DIV, SUB);
        float bottom = rcDoc.floatExpression(top, textHeight, ADD);
        float right = rcDoc.floatExpression(left, textWidth, ADD);
        rcDoc.getPainter().setStyle(Paint.Style.STROKE).setStrokeWidth(2f).setColor(
                Color.BLACK).commit();

        rcDoc.drawRect(left, top, right, bottom);
        rcDoc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();

        rcDoc.drawTextAnchored(textId, cx, cy, 0, 0, 0);

        return rcDoc;
    }

    /**
     * Demo showing linear gradient.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter gradient1() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float height = RemoteContext.FLOAT_WINDOW_HEIGHT;
        rcDoc.getPainter().setLinearGradient(0, 0, 0, height, new int[]{0xFF00FF00, 0xFF0022FF},
                null, Shader.TileMode.REPEAT).setTextSize(64f).commit();

        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();

        rcDoc.drawTextAnchored("gradient", cx, cy, 0, 0, 0);

        return rcDoc;
    }

    /**
     * Demo showing linear gradient with animated color.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter gradient2() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float height = RemoteContext.FLOAT_WINDOW_HEIGHT;
        float hue = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, (float) (PI * 2),
                DIV,
                1f, MOD);

        int color = rcDoc.addColorExpression(0x8F, hue, 0.9f, 0.9f);
        rcDoc.getPainter().setLinearGradient(0, 0, 0, height, new int[]{color, 0xFF0022FF}, null,
                Shader.TileMode.REPEAT).setTextSize(64f).commit();

        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();

        rcDoc.drawTextAnchored("gradient", cx, cy, 0, 0, 0);

        return rcDoc;
    }

    /**
     * Demo showing radial gradient with animated color.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter gradient3() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float height = RemoteContext.FLOAT_WINDOW_HEIGHT;
        float hue = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, (float) (PI * 2),
                DIV,
                1f, MOD);
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        int color = rcDoc.addColorExpression(0x8F, hue, 0.9f, 0.9f);
        rcDoc.getPainter().setRadialGradient(cx, cy, height, new int[]{color, 0xFF0022FF}, null,
                Shader.TileMode.REPEAT).setTextSize(64f).commit();

        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();

        rcDoc.drawTextAnchored("gradient", cx, cy, 0, 0, 0);

        return rcDoc;
    }

    /**
     * Demo showing sweep gradient with animated color.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter gradient4() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float hue = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, (float) (PI * 2),
                DIV,
                1f, MOD);
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        int color = rcDoc.addColorExpression(0x8F, hue, 0.9f, 0.9f);
        rcDoc.getPainter().setSweepGradient(cx, cy, new int[]{0xFF0022FF, color, 0xFF0022FF},
                null).setTextSize(64f).commit();

        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 1, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();

        rcDoc.drawTextAnchored("gradient", cx, cy, 0, 0, 0);

        return rcDoc;
    }

    /**
     * Clock demo using paths and rotation.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter simple6() {

        RemotePath rPoly = makePathSVG();

        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FIT);

        rcDoc.getPainter().setColor(Color.RED).commit();

        float centerX = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float centerY = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        float scale = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH,
                RemoteContext.FLOAT_WINDOW_HEIGHT, MIN, 2f, MUL);
        float hourHandLength = rcDoc.floatExpression(centerX, centerY, MIN, 0.4f, MUL);
        float minHandLength = rcDoc.floatExpression(centerX, centerY, MIN, 0.3f, MUL);
        float secondAngle = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 60f, MOD,
                60f,
                MUL);
        float minAngle = rcDoc.floatExpression(RemoteContext.FLOAT_TIME_IN_MIN, 6f, MUL);
        float hrAngle = rcDoc.floatExpression(RemoteContext.FLOAT_TIME_IN_HR, 30f, MUL);
        float hourWidth = 8f;
        float handWidth = 4f;
        int baseColorId = rcDoc.addNamedColor("android.colorAccent", 0xFF1A1A5E);
        rcDoc.getPainter().setColorId(baseColorId).setStyle(Paint.Style.FILL).commit();
        rcDoc.save();
        rcDoc.translate(centerX, centerY);
        rcDoc.scale(scale, scale);
        rcDoc.drawPath(rPoly);
        rcDoc.restore();
        //        rcDoc.getPainter().setColorId(Color.GREEN).setStrokeWidth(8f).commit();

        rcDoc.save();
        rcDoc.getPainter().setColor(Color.GRAY).setStrokeWidth(hourWidth).setStrokeCap(
                Paint.Cap.ROUND).commit();
        rcDoc.rotate(hrAngle, centerX, centerY);
        rcDoc.drawLine(centerX, centerY, centerX, hourHandLength);
        rcDoc.restore();

        rcDoc.save();
        rcDoc.getPainter().setColor(Color.WHITE).setStrokeWidth(handWidth).commit();
        rcDoc.rotate(minAngle, centerX, centerY);
        rcDoc.drawLine(centerX, centerY, centerX, minHandLength);
        rcDoc.restore();

        // sec
        rcDoc.save();
        rcDoc.rotate(secondAngle, centerX, centerY);
        rcDoc.getPainter().setColor(Color.RED).setStrokeWidth(4f).commit();
        rcDoc.drawLine(centerX, centerY, centerX, minHandLength);
        rcDoc.restore();
        // cap
        rcDoc.drawCircle(centerX, centerY, handWidth / 2);

        return rcDoc;
    }

    /**
     * Demo showing text with path effects and animated sweep gradient.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter textPathEffects() {
        String text = "0123456789";
        Paint paint = new Paint();
        Path path = buildPathFromText(text, paint, 4);
        RectF src = new RectF();
        path.computeBounds(src, false);
        Matrix matrix = new Matrix();
        matrix.postTranslate(-src.centerX(), -src.centerY());
        path.transform(matrix);
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER,
                RootContentBehavior.SIZING_SCALE, RootContentBehavior.SCALE_FILL_BOUNDS);

        float hue = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, (float) (PI * 2),
                DIV,
                1f, MOD);
        float cx = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_WIDTH, 0.5f, MUL);
        float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
        int color = rcDoc.addColorExpression(0x8F, hue, 0.9f, 0.9f);
        float tcy = rcDoc.floatExpression(cy, 100f, SUB);
        rcDoc.getPainter().setSweepGradient(cx, cy, new int[]{0xFF0022FF, color, 0xFF0022FF},
                null).setTextSize(64f).commit();

        float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
        rcDoc.save();
        rcDoc.scale(anim, 0.5f, cx, cy);
        rcDoc.drawOval(0, 0, RemoteContext.FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
        rcDoc.restore();
        rcDoc.save();

        rcDoc.translate(cx, tcy);
        rcDoc.drawPath(path);
        float strokeWidth = rcDoc.floatExpression(anim, ABS, 10f, MUL);
        rcDoc.getPainter().setStyle(Paint.Style.STROKE).setStrokeWidth(strokeWidth).commit();
        rcDoc.drawPath(path);
        rcDoc.getPainter().setStyle(Paint.Style.FILL).commit();
        rcDoc.restore();
        rcDoc.getPainter().clearColorFilter().commit();
        rcDoc.drawTextAnchored(text, cx, cy, 0, 0, 0);

        return rcDoc;
    }

    static RemotePath makePathSVG() {
        String svg = "M 0.503 0.224 C 0.503 0.266 0.457 0.296 0.438 0.33 "
                + "C 0.418 0.365 0.414 0.42 0.379 0.44 " + "C 0.345 0.46 0.296 0.436 0.254 0.436 "
                + "C 0.212 0.436 0.163 0.459 0.129 0.44 " + "C 0.094 0.42 0.09 0.365 0.07 0.33 "
                + "C 0.05 0.296 0.005 0.266 0.005 0.224 " + "C 0.005 0.182 0.051 0.152 0.07 0.118 "
                + "C 0.09 0.083 0.094 0.029 0.129 0.008 "
                + "C 0.163 -0.012 0.212 0.012 0.254 0.012 "
                + "C 0.296 0.012 0.345 -0.011 0.379 0.008 "
                + "C 0.414 0.028 0.418 0.083 0.438 0.118 "
                + "C 0.458 0.152 0.503 0.182 0.503 0.224 Z";
        RemotePath path = new RemotePath(svg);
        Matrix matrix = new Matrix();
        matrix.postTranslate(-0.255f, -0.225f);
        // matrix.postScale(1/2f,1/2f);
        path.transform(matrix);
        return path;
    }

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
