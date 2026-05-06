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

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.RootContentBehavior;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Procedural checks for path drawing.
 */
@SuppressLint("RestrictedApiAndroidX")
public class PathProceduralChecks {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private PathProceduralChecks() {
    }

    /**
     * Demo showing a basic path with a cubic curve.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter basicPath() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER, RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT);
        rcDoc.getPainter().setStrokeWidth(30f).setColor(0xFF3232FF).setStyle(
                Paint.Style.STROKE).commit();
        Path path = new Path();
        path.moveTo(200, 100);
        path.cubicTo(100f, 200f, 100f, 200f, 0f, 100f);
        rcDoc.drawPath(path);
        return rcDoc;
    }

    /**
     * Demo showing a path with various segments (cubic, line, quad).
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter allPath() {
        RemoteComposeWriterAndroid rcDoc = new RemoteComposeWriterAndroid(300, 300, "Clock", 6, 0,
                sPlatform);
        rcDoc.setRootContentBehavior(RootContentBehavior.NONE,
                RootContentBehavior.ALIGNMENT_CENTER, RootContentBehavior.SIZING_SCALE,
                RootContentBehavior.SCALE_FIT);
        rcDoc.getPainter().setStrokeWidth(30f).setColor(0xFF3232FF).setStyle(
                Paint.Style.STROKE).commit();
        Path path = new Path();
        path.moveTo(200, 100);
        path.cubicTo(100f, 200f, 100f, 200f, 0f, 100f);
        path.moveTo(200, 70);
        path.lineTo(0, 70);

        path.moveTo(200, 50);
        path.quadTo(100f, 0f, 0f, 50);
        rcDoc.drawPath(path);
        return rcDoc;
    }


}
