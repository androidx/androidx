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

import static androidx.compose.remote.creation.Rc.FloatExpression.DIV;
import static androidx.compose.remote.creation.Rc.FloatExpression.MIN;
import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demo showing path winding rules.
 */
@SuppressLint("RestrictedApiAndroidX")
public class DemoWindingRule {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();
    // private static final int WINDING = 0; // (default)
    // private static final int EVEN_ODD = 1;
    // private static final int INVERSE_EVEN_ODD = 2;
    // private static final int INVERSE_WINDING = 3;
    private DemoWindingRule() {

    }
    /**
     * Creates a path winding demo.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter pathWinding() {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(500, 500, "sd", 7,
                RcProfiles.PROFILE_ANDROIDX, sPlatform);
        rc.root(() -> {
            rc.startCanvas(new RecordingModifier().fillMaxSize());
            float w = rc.addComponentWidthValue();
            float h = rc.addComponentHeightValue();
            rc.getPainter().setColor(Color.GRAY).commit();

            rc.drawRect(0, 0, w, h);

            float cx = rc.floatExpression(w, 0.5f, MUL);
            float cy = rc.floatExpression(h, 0.5f, MUL);
            float rad = rc.floatExpression(cx, cy, MIN, 2, DIV);

            rc.getPainter().setColor(0xFF7777ff).commit();
            rc.drawCircle(cx, cy, rad);
            rc.getPainter().setColor(0xFF77aaff).commit();
            for (int i = 0; i < 4; i++) {
                int id = rc.addPathData(spiral(), i);
                rc.save();

                switch (i) {
                    case 0:
                        rc.translate(0, 0);
                        break;
                    case 1:
                        rc.translate(cx, 0);
                        break;
                    case 2:
                        rc.translate(0, cy);
                        break;
                    case 3:
                        rc.translate(cx, cy);
                        break;
                }

                rc.scale(0.01f, 0.01f);
                rc.scale(rad, rad);
                rc.clipRect(0, 0, 200, 200);
                int color = (i == 0) ? 0xFF5555ff
                        : (i == 1) ? 0xFF55ff55 : (i == 2) ? 0xFFff5555 : 0xFFffff55;
                rc.getPainter().setColor(Color.LTGRAY).setStyle(Paint.Style.FILL).commit();
                rc.drawCircle(100, 100, 100);
                rc.getPainter().setColor(color).setStyle(Paint.Style.FILL).setStrokeWidth(
                        0.1f).commit();

                rc.drawPath(id);
                rc.restore();

            }
            rc.getPainter().setColor(0xFF7777ff).commit();


            rc.endCanvas();

        });
        return rc;
    }


    static Path spiral() {
        Path rp = new Path();
        for (int i = 0; i <= 100; i++) {
            double angle = 3 * 2 * Math.PI * i / 100f;
            float x = (float) (Math.sin(angle) * (1 + i)) + 100f;
            float y = (float) (Math.cos(angle) * (1 + i)) + 100f;
            if (i == 0) {
                rp.moveTo(x, y);
            } else {
                rp.lineTo(x, y);
            }

        }
        rp.close();
        return rp;
    }

}
