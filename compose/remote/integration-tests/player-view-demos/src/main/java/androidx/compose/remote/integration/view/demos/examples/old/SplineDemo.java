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

import static androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC;
import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.A_SPLINE;
import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MOD;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo for spline-based animation scaling.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SplineDemo {
    private SplineDemo() {
    }

    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    /**
     * Creates a spline animation demo.
     *
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter splineDemo1() {

        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.root(
                () -> {
                    rcDoc.startBox(
                            new RecordingModifier().fillMaxWidth().fillMaxHeight(),
                            BoxLayout.START,
                            BoxLayout.START);
                    rcDoc.startCanvas(new RecordingModifier().fillMaxSize());
                    float w = rcDoc.addComponentWidthValue();
                    float h = rcDoc.addComponentHeightValue();
                    float cx = rcDoc.floatExpression(w, 0.5f, MUL);
                    float cy = rcDoc.floatExpression(h, 0.5f, MUL);
                    float[] data = {1, 1, 1, 0.5f, 0.8f, 1.0f, 0.8f, 1.0f};
                    float array = rcDoc.addFloatArray(data);
                    rcDoc.getPainter().setTextSize(256f).commit();
                    float scale =
                            rcDoc.floatExpression(
                                    array, FLOAT_CONTINUOUS_SEC, 1.8f, MOD, 1.8f, DIV, A_SPLINE);
                    rcDoc.save();
                    rcDoc.scale(scale, scale, cx, cy);
                    rcDoc.drawTextAnchored("❤", cx, cy, 0, 0, 0);
                    rcDoc.restore();
                    rcDoc.endCanvas();
                    rcDoc.endBox();
                });
        return rcDoc;
    }
}
