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
import static androidx.compose.remote.creation.Rc.FloatExpression.SUB;
import static androidx.compose.remote.creation.RemoteComposeWriter.STOP_ABSOLUTE_POS;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo showing haptic feedback capabilities.
 */
@SuppressLint("RestrictedApiAndroidX")
public class HapticDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private HapticDemo() {
    }

    static String[] sList =
            {"NO HAPTICS", "LONG PRESS", "VIRTUAL KEY", "KEYBOARD TAP", "CLOCK TICK",
                    "CONTEXT CLICK", "KEYBOARD PRESS", "KEYBOARD RELEASE", "VIRTUAL KEY RELEASE",
                    "TEXT HANDLE MOVE", "GESTURE START", "GESTURE END", "CONFIRM", "REJECT",
                    "TOGGLE ON", "TOGGLE OFF", "THRESHOLD ACTIVATE", "THRESHOLD DEACTIVATE",
                    "DRAG START", "SEGMENT TICK", "FREQUENT TICK"};

    /**
     * Creates a haptic feedback demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter demoHaptic1() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(1024, 1204, "Clock", 6, 0,
                sPlatform);
        rc.root(() -> {
            rc.startBox(
                    new RecordingModifier().fillMaxWidth().fillMaxHeight().background(Color.GRAY),
                    BoxLayout.START, BoxLayout.START);
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float event = RemoteContext.FLOAT_TOUCH_EVENT_TIME;

            float buttonWidth = rc.floatExpression(w, 3, DIV, 10, SUB);
            float buttonHeight = rc.floatExpression(h, 7, DIV, 20, SUB);
            float layoutStepX = rc.floatExpression(buttonWidth, 10, ADD);
            float layoutStepY = rc.floatExpression(buttonHeight, 10, ADD);
            float buttonCenterX = rc.floatExpression(w, 6, DIV);
            float buttonCenterY = rc.floatExpression(h, 14, DIV);

            float tx = rc.addTouch(0, 0, w, STOP_ABSOLUTE_POS, 0, 0, null, null,
                    RemoteContext.FLOAT_TOUCH_POS_X);
            float ty = rc.addTouch(0, 0, w, STOP_ABSOLUTE_POS, 0, 0, null, null,
                    RemoteContext.FLOAT_TOUCH_POS_Y);
            rc.getPainter().setTextSize(32f).commit();

            rc.save();
            int i = 0;
            for (int y = 0; y < 7; y++) {
                rc.save();
                for (int x = 0; x < sList.length / 6; x++) {
                    if (i < sList.length) {
                        rc.getPainter().setColor(Color.BLUE).commit();
                        rc.drawRoundRect(4, 4, buttonWidth, buttonHeight, 32, 32);
                        rc.getPainter().setColor(Color.WHITE).commit();
                        rc.drawTextAnchored(sList[i], buttonCenterX, buttonCenterY, 0, 0, 0);
                        rc.translate(layoutStepX, 0);

                        i++;
                    }
                }
                rc.restore();
                rc.translate(0, layoutStepY);
            }

            rc.restore();
            rc.impulse(0.1f, event);
            rc.performHaptic(8);
            rc.impulseProcess();
            rc.getPainter().setColor(Color.GREEN).commit();
            rc.drawCircle(tx, ty, 60);
            rc.impulseEnd();
            rc.impulseEnd();
            rc.endCanvas();
            rc.endBox();
        });
        return rc;
    }
}
