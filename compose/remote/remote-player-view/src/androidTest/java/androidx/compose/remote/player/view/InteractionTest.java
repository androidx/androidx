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
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.ValueFloatChange;
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
        RemoteComposeWriter writer =
                new RemoteComposeWriter(
                        w,
                        h,
                        "Test",
                        7,
                        RcProfiles.PROFILE_ANDROIDX,
                        new AndroidxRcPlatformServices());

        writer.root(
                () -> {
                    int id = writer.addNamedString("status", "idle");
                    writer.column(
                            new RecordingModifier().fillMaxSize().verticalScroll(),
                            0,
                            0,
                            () -> {
                                // Button at (0,0) to (200,200)
                                writer.box(
                                        new RecordingModifier()
                                                .size(200)
                                                .background(Color.RED)
                                                .onClick(new ValueStringChange(id, "clicked")));
                                // Long spacer to allow scrolling
                                writer.box(
                                        new RecordingModifier()
                                                .width(200)
                                                .height(2000)
                                                .background(Color.BLUE));
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
        view.onTouchEvent(
                MotionEvent.obtain(
                        downTime,
                        downTime + 10,
                        MotionEvent.ACTION_MOVE,
                        100f,
                        100f - slop - 10f,
                        0));

        // 3. Touch up
        view.onTouchEvent(
                MotionEvent.obtain(
                        downTime,
                        downTime + 20,
                        MotionEvent.ACTION_UP,
                        100f,
                        100f - slop - 10f,
                        0));

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

    @Test
    public void testTouchExceptionSwallowed() {
        int w = 100;
        int h = 100;
        RemoteComposeView view = new RemoteComposeView(sAppContext);

        RemoteComposeWriter writer =
                new RemoteComposeWriter(
                        w,
                        h,
                        "Test",
                        7,
                        RcProfiles.PROFILE_ANDROIDX,
                        new AndroidxRcPlatformServices());

        writer.root(
                () -> {
                    writer.box(
                            new RecordingModifier()
                                    .size(100)
                                    .background(Color.RED)
                                    .onClick(new ValueFloatChange(1000000, 1.0f)));
                });

        byte[] bytes = writer.encodeToByteArray();
        RemoteDocument remoteDoc = new RemoteDocument(new ByteArrayInputStream(bytes));
        view.setDocument(remoteDoc);

        // Force layout
        view.measure(w, h);
        view.layout(0, 0, w, h);

        long downTime = SystemClock.uptimeMillis();
        view.onTouchEvent(
                MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f, 0));
        boolean handled =
                view.onTouchEvent(
                        MotionEvent.obtain(
                                downTime, downTime + 10, MotionEvent.ACTION_UP, 50f, 50f, 0));

        // Exception was swallowed, so performClick should not crash the thread,
        // and returns false/handled accordingly.
        org.junit.Assert.assertFalse(handled);
    }

    @Test
    public void testMeasureExceptionSwallowed() {
        int w = 100;
        int h = 100;
        RemoteComposeView view = new RemoteComposeView(sAppContext);

        // 1. Load benign document first to initialize paintContext in mARContext
        RemoteComposeWriter doc1Writer =
                new RemoteComposeWriter(
                        w,
                        h,
                        "Test1",
                        7,
                        RcProfiles.PROFILE_ANDROIDX,
                        new AndroidxRcPlatformServices());
        doc1Writer.root(
                () -> {
                    doc1Writer.box(new RecordingModifier().size(100));
                });
        byte[] doc1Bytes = doc1Writer.encodeToByteArray();
        RemoteDocument doc1 = new RemoteDocument(new ByteArrayInputStream(doc1Bytes));
        view.setDocument(doc1);
        // Force measure and layout/draw so paintContext is initialized
        view.measure(w, h);
        view.layout(0, 0, w, h);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        // 2. Load doc2 with explicit FEATURE_PAINT_MEASURE = 0 and an empty StateLayout
        // which throws a RuntimeException during the measure pass.
        RemoteComposeWriter doc2Writer =
                new RemoteComposeWriter(
                        new AndroidxRcPlatformServices(),
                        7,
                        RemoteComposeWriter.hTag(Header.DOC_WIDTH, w),
                        RemoteComposeWriter.hTag(Header.DOC_HEIGHT, h),
                        RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, "Test2"),
                        RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                        RemoteComposeWriter.hTag(Header.FEATURE_PAINT_MEASURE, 0));

        doc2Writer.root(
                () -> {
                    doc2Writer.stateLayout(
                            new RecordingModifier(),
                            0,
                            () -> {
                                // Zero children -> empty StateLayout
                            });
                });
        byte[] doc2Bytes = doc2Writer.encodeToByteArray();
        RemoteDocument doc2 = new RemoteDocument(new ByteArrayInputStream(doc2Bytes));
        view.setDocument(doc2);

        // This call to measure should trigger StateLayout measure pass -> getLayout()
        // which throws RuntimeException because StateLayout is empty.
        // It must be swallowed by onMeasure()'s try-catch!
        view.measure(w, h);
    }
}
