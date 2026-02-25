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

import static androidx.compose.remote.creation.Rc.FloatExpression.MUL;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.operations.TextFromFloat;
import androidx.compose.remote.core.operations.TimeAttribute;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.managers.BoxLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.actions.Action;
import androidx.compose.remote.creation.actions.HostAction;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

/**
 * Demos for click and touch interactions.
 */
@SuppressLint("RestrictedApiAndroidX")
public class SimpleClick {
    static RcPlatformServices sPlatform = new AndroidxRcPlatformServices();

    private SimpleClick() {
    }

    /**
     * Demo showing a layout with boxes that respond to touch and click.
     *
     * @param n a number to display.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter clickDemo1(int n) {
        RemoteComposeWriter rc = new RemoteComposeWriter(300, 300, "HeartsDemo", sPlatform);
        int id = rc.textCreateId(">" + n + "<");
        int id2 = rc.textCreateId("Reset!");
        Action action = new HostAction(567);
        Action action2 = new HostAction(568);
        rc.root(() -> {
            rc.box(new RecordingModifier().fillMaxSize().background(Color.BLUE), BoxLayout.CENTER,
                    BoxLayout.CENTER, () -> {
                        rc.row(new RecordingModifier(), 0, 0, () -> {
                            rc.box(new RecordingModifier().background(Color.YELLOW).onTouchDown(
                                    action), 0, 0, () -> rc.textComponent(new RecordingModifier(),
                                    id, Color.RED, 120f, 1,
                                    1f, "Sans Serif", 0, 0, 1,
                                        () -> System.out.print("")));
                            rc.box(new RecordingModifier().background(Color.RED).onClick(action2),
                                    0, 0, () -> rc.textComponent(new RecordingModifier(), id2,
                                            Color.WHITE,
                                            120f, 1, 1f, "Sans Serif", 0, 0, 1,
                                            () -> System.out.print("")));
                        });
                    });
        });

        return rc;
    }

    /**
     * Demo showing the addClickArea API.
     *
     * @param n a number to display.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter clickDemo2(int n) {
        RemoteComposeWriterAndroid rc = new RemoteComposeWriterAndroid(300, 300, "HeartsDemo",
                sPlatform);
        rc.getPainter().setColor(Color.GREEN).setTextSize(150f).commit();
        float x = 200;
        float y = 200;
        float w = 800;
        float h = 420;
        rc.drawRect(x, y, x + w, y + h);
        Utils.log("add click area");
        rc.addClickArea(567, "This click area", x, y, x + w, y + h, "bob");
        rc.getPainter().setColor(Color.BLACK).setTextSize(150f).commit();
        rc.drawTextAnchored(">" + n + "<", x + w / 2, y + h / 2, 0, -2, 0);
        long docCreationTime = System.currentTimeMillis();
        int nowId = rc.addTimeLong(docCreationTime);
        float time = rc.timeAttribute(nowId, TimeAttribute.TIME_FROM_LOAD_SEC);
        float val = rc.floatExpression(time, -1, MUL);
        int id = rc.createTextFromFloat(val, 1, 3, TextFromFloat.PAD_AFTER_ZERO);

        rc.drawTextAnchored("round trip:", x + w / 2, y + h / 2, 0, 0, 0);
        rc.drawTextAnchored(id, x + w / 2, y + h / 2, 0, 2, 0);
        return rc;
    }

    /**
     * Demo showing simple text updates.
     *
     * @param n a number to display.
     * @return a {@link RemoteComposeWriter} containing the drawing commands.
     */
    public static @NonNull RemoteComposeWriter clickDemoUpdate(int n) {
        RemoteComposeWriter rc = new RemoteComposeWriter(300, 300, "HeartsDemo", sPlatform);
        int id = rc.textCreateId(">" + n + "<");

        Utils.log(" text id " + id);

        return rc;
    }
}
