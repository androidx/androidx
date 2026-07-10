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
import static androidx.compose.remote.core.RemoteContext.FLOAT_ANIMATION_DELTA_TIME;
import static androidx.compose.remote.core.RemoteContext.FLOAT_WINDOW_HEIGHT;
import static androidx.compose.remote.core.RemoteContext.FLOAT_WINDOW_WIDTH;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.RAND;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.SQRT;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.VAR1;
import static androidx.compose.remote.creation.Rc.FloatExpression.ADD;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.RemotePath;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for impulse-based animations and particle systems.
 */
@SuppressLint("RestrictedApiAndroidX")
public class ImpulseDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();
    static RemotePath sBallon = ballon();
    private ImpulseDemo() {
    }

    /**
     * Demo showing animated balloons triggered by impulse.
     *
     * @param image the image to be masked by the balloons.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter balloonDemo(@NonNull Bitmap image) {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "HeartsDemo",
                6, 0, sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            float cx = rcDoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
            float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
            rcDoc.getPainter().setColor(0xFF_AAAAAA).setStyle(Paint.Style.FILL).commit();

            rcDoc.save();

            rcDoc.drawRoundRect(0, 0, FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT, 72,
                    72);

            rcDoc.restore();

            rcDoc.addTouch(cy, 0, RemoteContext.FLOAT_WINDOW_HEIGHT,
                    RemoteComposeWriter.STOP_ABSOLUTE_POS, 0, 0, null, null,
                    new float[]{RemoteContext.FLOAT_TOUCH_POS_Y});
            rcDoc.addTouch(cx, 0, FLOAT_WINDOW_WIDTH,
                    RemoteComposeWriter.STOP_ABSOLUTE_POS, 0, 0, null, null,
                    new float[]{RemoteContext.FLOAT_TOUCH_POS_X});
            float event = RemoteContext.FLOAT_TOUCH_EVENT_TIME;
            float imgTop = rcDoc.floatExpression(FLOAT_WINDOW_HEIGHT, 0.15f, MUL);
            float imgLeft = rcDoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.15f, MUL);
            float imgRight = rcDoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.85f, MUL);
            float imgBottom = rcDoc.floatExpression(FLOAT_WINDOW_HEIGHT, 0.85f, MUL);
            RemotePath circle = circleEngine(rcDoc, imgTop, imgLeft, imgRight, imgBottom, false);

            RemotePath circle2 = circleEngine(rcDoc, imgTop, imgLeft, imgRight, imgBottom, true);
            int pathId = rcDoc.addPathData(circle);
            int pathId2 = rcDoc.addPathData(circle2);

            rcDoc.getPainter().setColor(0xFF553243).setStyle(Paint.Style.FILL).commit();
            rcDoc.drawPath(pathId);
            balloons(rcDoc, event, cx, cy);
            rcDoc.save();
            rcDoc.addClipPath(pathId2);
            rcDoc.drawScaledBitmap(image, 0, 0, image.getWidth(), image.getHeight(), imgLeft,
                    0, // imgTop,
                    imgRight, imgBottom, RemoteComposeWriter.IMAGE_SCALE_CROP, 1, "person");
            rcDoc.restore();
            balloons(rcDoc, event, cx, cy);
            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    static RemotePath ballon() {
        RemotePath ballon = new RemotePath();
        for (int i = 0; i < 100; i++) {
            double theta = -Math.PI + 2 * Math.PI * i / 100.;
            double k = 2.75;
            double s = 0.375;
            double r = 1 + s / Math.cosh(k * (theta + Math.PI / 2));
            float x = (float) (r * Math.cos(theta));
            float y = -(float) (r * Math.sin(theta));

            if (i == 0) {
                ballon.moveTo(x, y);
            } else {
                ballon.lineTo(x, y);
            }
        }

        ballon.close();
        return ballon;
    }

    static void drawBalloon(RemoteComposeWriter rcDoc, float cx, float cy, float dx, float dy) {

        rcDoc.save();
        rcDoc.translate(cx, cy);
        rcDoc.scale(30, 30);
        rcDoc.drawPath(sBallon);
        float tx = rcDoc.floatExpression(dx, -0.003f, MUL);
        rcDoc.drawLine(0, 1.1f, tx, 2.5f);

        rcDoc.restore();
    }

    static void balloons(RemoteComposeWriterAndroid rcDoc, float event, float tx, float ty) {
        {
            float cx = rcDoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);

            rcDoc.getPainter().setTextSize(64f).commit();
            rcDoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.FILL).commit();
            rcDoc.impulse(10f, event);
            float[] variables = new float[6];
            float ps = rcDoc.createParticles(variables,
                    new float[][]{{FLOAT_WINDOW_WIDTH, RAND, MUL}, // x
                            {FLOAT_WINDOW_WIDTH, 1, RAND, ADD, MUL}, // y
                            {RAND, 0.5f, SUB, 20, MUL}, // dx
                            {RAND, -6, ADD, 50, MUL}, // dy
                            {RAND, 2, MUL, 2, ADD}, {1f}}, 10);
            float x = variables[0];
            float y = variables[1];
            float dx = variables[2];
            float dy = variables[3];
            float h = variables[4];
            float alpha = variables[5];
            rcDoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.FILL).commit();
            rcDoc.impulseProcess(); // current_time - envnt_time  < 0.5
            rcDoc.particlesLoop(ps, new float[]{y, -1, MUL},
                    new float[][]{{x, dx, FLOAT_ANIMATION_DELTA_TIME, MUL, ADD},
                            {y, dy, FLOAT_ANIMATION_DELTA_TIME, MUL, ADD},
                            {cx, x, SUB, RAND, MUL, 0.01f, MUL, dx, ADD}, {dy}, {h},
                            {alpha, 0.999f, MUL}}, () -> {
                        rcDoc.getPainter().setAlpha(alpha).commit();
                        rcDoc.save();
                        rcDoc.scale(h, h, x, y);
                        rcDoc.getPainter().setColor(Color.RED).setStyle(Paint.Style.FILL).commit();

                        drawBalloon(rcDoc, x, y, dx, dy);

                        rcDoc.getPainter().setColor(Color.BLACK).setStyle(
                                Paint.Style.STROKE).commit();
                        drawBalloon(rcDoc, x, y, dx, dy);
                        rcDoc.restore();
                    });
            rcDoc.impulseEnd();
            rcDoc.impulseEnd();
        }
    }

    /** Create something that approximates a circular shape */
    static RemotePath circleEngine(RemoteComposeWriter rc, float top, float left, float right,
            float bottom, boolean expand) {
        RemotePath rp = new RemotePath();
        float midX = rc.floatExpression(right, left, ADD, 2, DIV);
        float midy = rc.floatExpression(bottom, top, ADD, 2, DIV);
        rp.moveTo(left, midy);
        if (expand) {
            rp.cubicTo(left, 0, left, 0, midX, 0);
            rp.cubicTo(right, 0, right, 0, right, midy);
        } else {
            rp.cubicTo(left, top, left, top, midX, top);
            rp.cubicTo(right, top, right, top, right, midy);
        }

        rp.cubicTo(right, bottom, right, bottom, midX, bottom);
        rp.cubicTo(left, bottom, left, bottom, left, midy);

        rp.close();
        return rp;
    }

    /**
     * Demo showing hearts animation triggered by impulse.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter heartsDemo() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "HeartsDemo",
                6, 0, sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());

            float cx = rcDoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
            float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
            float anim = rcDoc.floatExpression(RemoteContext.FLOAT_CONTINUOUS_SEC, 2, MOD, 1, SUB);
            float ty = rcDoc.addTouch(cy, 0, RemoteContext.FLOAT_WINDOW_HEIGHT,
                    RemoteComposeWriter.STOP_ABSOLUTE_POS, 0, 0, null, null,
                    new float[]{RemoteContext.FLOAT_TOUCH_POS_Y});
            float tx = rcDoc.addTouch(cx, 0, FLOAT_WINDOW_WIDTH,
                    RemoteComposeWriter.STOP_ABSOLUTE_POS, 0, 0, null, null,
                    new float[]{RemoteContext.FLOAT_TOUCH_POS_X});
            rcDoc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();

            float event = RemoteContext.FLOAT_TOUCH_EVENT_TIME;

            rcDoc.getPainter().setColor(Color.GREEN).setAlpha(0.3f).setTextSize(64f).commit();
            rcDoc.save();
            rcDoc.scale(anim, 1, cx, cy);

            rcDoc.drawOval(0, 0, FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);
            rcDoc.restore();

            int textId = rcDoc.createTextFromFloat(500, 3, 0, PAD_AFTER_ZERO);

            rcDoc.getPainter().setStyle(Paint.Style.STROKE).setStrokeWidth(2f).setColor(
                    Color.BLACK).commit();

            rcDoc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();

            rcDoc.drawTextAnchored(textId, cx, cy, 0, 0, 0);
            hearts(rcDoc, event, tx, ty);
            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    static void hearts(RemoteComposeWriterAndroid rcDoc, float event, float tx, float ty) {
        {
            rcDoc.getPainter().setTextSize(64f).commit();

            rcDoc.impulse(7.9f, event);
            float[] variables = new float[6];
            float ps = rcDoc.createParticles(variables,
                    new float[][]{{tx, RAND, 300f, MUL, 150, SUB, ADD},
                            {ty, RAND, 300f, MUL, 150, SUB, ADD},
                            {RAND, SQRT, RAND, SQRT, SUB, 10f, MUL,
                                    RemoteContext.FLOAT_TOUCH_VEL_X, ADD},
                            {RAND, SQRT, RAND, SQRT, SUB, 10f, MUL,
                                    RemoteContext.FLOAT_TOUCH_VEL_Y, ADD}, {RAND, 2, MUL}, {1f}},
                    50);
            float x = variables[0];
            float y = variables[1];
            float dx = variables[2];
            float dy = variables[3];
            float h = variables[4];
            float alpha = variables[5];
            rcDoc.impulseProcess(); // current_time - envnt_time  < 0.5
            rcDoc.particlesLoop(ps, new float[]{y, RemoteContext.FLOAT_WINDOW_HEIGHT, SUB},
                    new float[][]{{x, dx, ADD}, {y, dy, ADD}, {dx},
                            {dy, 9.8f, FLOAT_ANIMATION_DELTA_TIME, MUL, ADD}, {h},
                            {alpha, 0.99f, MUL}}, () -> {
                        rcDoc.getPainter().setAlpha(alpha).commit();
                        rcDoc.save();
                        rcDoc.scale(h, h, x, y);
                        rcDoc.drawTextRun("❤", 0, 1, 0, 0, x, y, false);
                        rcDoc.restore();
                    });
            rcDoc.impulseEnd();
            rcDoc.impulseEnd();
        }
    }

    // =========================== confetti ====================================

    static Bitmap sBall = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);

    static {
        int w = sBall.getWidth();
        int h = sBall.getHeight();
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
            data[i] = 0x88000000 + 0x10101 * bright;
        }
        sBall.setPixels(data, 0, w, 0, 0, w, h);
    }

    /**
     * Demo showing confetti animation triggered by impulse.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter confettiDemo() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "HeartsDemo",
                6, 0, sPlatform);
        rcDoc.root(() -> {
            rcDoc.startBox(new RecordingModifier().fillMaxWidth().fillMaxHeight(), BoxLayout.START,
                    BoxLayout.START);
            rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
            float cx = rcDoc.floatExpression(FLOAT_WINDOW_WIDTH, 0.5f, MUL);
            float cy = rcDoc.floatExpression(RemoteContext.FLOAT_WINDOW_HEIGHT, 0.5f, MUL);
            rcDoc.addTouch(cx, 0, FLOAT_WINDOW_WIDTH,
                    RemoteComposeWriter.STOP_ABSOLUTE_POS, 0, 0, null, null,
                    new float[]{RemoteContext.FLOAT_TOUCH_POS_X});
            rcDoc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();
            float event = RemoteContext.FLOAT_TOUCH_EVENT_TIME;

            rcDoc.getPainter().setColor(0xFF234488).commit();

            rcDoc.drawOval(0, 0, FLOAT_WINDOW_WIDTH, RemoteContext.FLOAT_WINDOW_HEIGHT);

            int textId = rcDoc.createTextFromFloat(500, 3, 0, PAD_AFTER_ZERO);

            rcDoc.getPainter().setStyle(Paint.Style.STROKE).setStrokeWidth(2f).setColor(
                    Color.BLACK).commit();

            rcDoc.getPainter().setColor(Color.BLUE).setStyle(Paint.Style.FILL).commit();

            rcDoc.drawTextAnchored(textId, cx, cy, 0, 0, 0);
            confettiEngine(rcDoc, event, FLOAT_WINDOW_WIDTH, FLOAT_WINDOW_HEIGHT);

            rcDoc.endCanvas();
            rcDoc.endBox();
        });
        return rcDoc;
    }

    static void confettiEngine(RemoteComposeWriterAndroid rcDoc, float event, float width,
            float height) {
        {
            rcDoc.getPainter().setTextSize(64f).setColor(Color.YELLOW).commit();
            rcDoc.getPainter().setColor(0xFFFFFF).setAlpha(0.7f).setStyle(
                    Paint.Style.FILL).commit();

            rcDoc.impulse(20f, event);
            float[] variables = new float[6];
            float ps = rcDoc.createParticles(variables,
                    new float[][]{{width, RAND, MUL}, {height, RAND, MUL, -2, MUL}, {0}, {0},
                            {VAR1, 2, DIV, 10, ADD}, {1f}}, 100);
            float x = variables[0];
            float y = variables[1];
            float dx = variables[2];
            float dy = variables[3];
            float dist = variables[4];
            float alpha = variables[5];
            float dt = FLOAT_ANIMATION_DELTA_TIME;
            rcDoc.impulseProcess(); // current_time - envnt_time  < 0.5

            rcDoc.particlesLoop(ps, new float[]{y, height, SUB},
                    new float[][]{{x, dx, dt, MUL, ADD}, {y, dist, dt, 10, MUL, MUL, ADD}, {dx},
                            {dy, 98f, dist, ADD, dt, MUL, 2, MUL, ADD}, {dist}, {alpha}}, () -> {
                        rcDoc.save();
                        rcDoc.translate(x, y);
                        rcDoc.scale(dist, dist);
                        rcDoc.scale(0.04f, 0.04f);
                        rcDoc.drawBitmap(sBall, 0, 0, sBall.getWidth(), sBall.getHeight(), "");

                        rcDoc.restore();

                    });

            rcDoc.impulseEnd();
            rcDoc.impulseEnd();
        }
    }
}
