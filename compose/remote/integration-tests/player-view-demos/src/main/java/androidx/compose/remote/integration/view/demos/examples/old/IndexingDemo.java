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


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.creation.Rc;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo showing resource indexing and lookup in RemoteCompose.
 */
@SuppressLint("RestrictedApiAndroidX")
public class IndexingDemo {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private IndexingDemo() {
    }

    /**
     * Creates a path indexing demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter pathIndex() {

        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "DClock", 7,
                RcProfiles.PROFILE_ANDROIDX, sPlatform);
        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());

            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            float cx = rc.floatExpression(w, 0.5f, AnimatedFloatExpression.MUL);
            float cy = rc.floatExpression(h, 0.5f, AnimatedFloatExpression.MUL);
            rc.getPainter().setColor(0xFFAABBCC).commit();
            rc.drawRoundRect(0, 0, w, h, 32, 32);
            float sec = rc.floatExpression(Rc.Time.TIME_IN_SEC, 10, Rc.FloatExpression.MOD);

            Paint paint = new Paint();
            int[] paths = new int[10];
            for (int i = 0; i < paths.length; i++) {
                paths[i] = rc.addPathData(DemoUtils.buildPathFromText("" + i, paint, 10));
            }

            float listId = rc.addList(paths);
            int id = rc.idLookup(listId, sec);

            rc.save();
            rc.translate(cx, cy);
            rc.getPainter().setColor(Color.RED).commit();
            rc.drawPath(paths[0]);
            rc.translate(0, -120);
            rc.drawPath(id | Rc.System.ID_DEREF);
            rc.restore();
            rc.getPainter().setColor(Color.BLACK).setTextSize(152f).commit();

            int textId = rc.createTextFromFloat(sec, 1, 0, 0);
            rc.drawTextAnchored(textId, cx, cy, 0, 0, 0);
            rc.endCanvas();
        });
        return rc;
    }
}
