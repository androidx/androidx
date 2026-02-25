/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.player.view;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.compose.remote.core.RcProfiles;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.ValueStringChange;
import androidx.compose.remote.creation.modifiers.RecordingModifier;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.core.platform.AndroidRemoteContext;
import androidx.compose.remote.player.view.platform.RemoteComposeView;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class InteractionTest {

    static Context sAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void testNoClickOnScroll() {
        int w = 1000;
        int h = 1000;
        RemoteComposeWriter writer = new RemoteComposeWriter(w, h, "Test",
                7, RcProfiles.PROFILE_ANDROIDX, new AndroidxRcPlatformServices());

        writer.root(() -> {
            int id = writer.addNamedString("status", "idle");
            writer.column(new RecordingModifier().fillMaxSize().verticalScroll(), 0, 0, () -> {
                // Button at (0,0) to (200,200)
                writer.box(new RecordingModifier().size(200).background(Color.RED)
                        .onClick(new ValueStringChange(id, "clicked")));
                // Long spacer to allow scrolling
                writer.box(new RecordingModifier().width(200).height(2000).background(Color.BLUE));
            });
        });

        byte[] bytes = writer.encodeToByteArray();
        RemoteDocument remoteDoc = new RemoteDocument(new ByteArrayInputStream(bytes));

        RemoteComposeView view = new RemoteComposeView(sAppContext);
        view.setDocument(remoteDoc);
        // Force layout
        view.measure(w, h);
        view.layout(0, 0, w, h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        AndroidRemoteContext context = (AndroidRemoteContext) view.getRemoteContext();

        // Verify initial state
        assertEquals("idle", context.getStringVariableName("status"));

        long downTime = SystemClock.uptimeMillis();

        // 1. Touch down on button
        view.onTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0));

        // 2. Drag up (scroll down) - distance > touch slop
        float slop = android.view.ViewConfiguration.get(sAppContext).getScaledTouchSlop();
        view.onTouchEvent(MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, 100f,
                100f - slop - 10f, 0));

        // 3. Touch up
        view.onTouchEvent(MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, 100f,
                100f - slop - 10f, 0));

        // Status should still be "idle" because we scrolled
        assertEquals("idle", context.getStringVariableName("status"));

        // 4. Test normal click
        downTime = SystemClock.uptimeMillis();
        view.onTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0));
        view.onTouchEvent(
                MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_UP, 100f, 100f, 0));

        // Status should now be "clicked"
        assertEquals("clicked", context.getStringVariableName("status"));
    }
}
