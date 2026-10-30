/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.view.platform;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteClock;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.SystemClock;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@RunWith(AndroidJUnit4.class)
public class RemoteComposeViewTest {

    private static final long VSYNC_INTERVAL_NANOS = 16_666_667L;
    private static final float FLOAT_TOLERANCE = 1e-4f;

    @Test
    public void doFrame_systemClock_usesChoreographerFrameTimeForAnimationTimeAndDelta() {
        Context context = ApplicationProvider.getApplicationContext();
        RemoteComposeView view = new RemoteComposeView(context);
        view.setDocument(new RemoteDocument(new CoreDocument(RemoteClock.SYSTEM)));
        view.layout(0, 0, 100, 100);

        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        long startNanos = view.mStart;
        RemoteContext remoteContext = view.getRemoteContext();

        // Simulate a Client-Side Render-Ahead onset burst of 3 back-to-back frames where
        // Choreographer frameTimeNanos advances by one VSYNC interval per frame without waiting
        // for wall-clock System.nanoTime() to elapse.
        long frame1Nanos = startNanos + VSYNC_INTERVAL_NANOS;
        view.mFrameCallback.doFrame(frame1Nanos);
        view.onDraw(canvas);

        float expectedTime1 = VSYNC_INTERVAL_NANOS * 1E-9f;
        assertEquals(expectedTime1, view.getAnimationTime(), FLOAT_TOLERANCE);
        assertEquals(
                expectedTime1,
                remoteContext.getFloat(RemoteContext.ID_ANIMATION_TIME),
                FLOAT_TOLERANCE);

        long frame2Nanos = startNanos + 2 * VSYNC_INTERVAL_NANOS;
        view.mFrameCallback.doFrame(frame2Nanos);
        view.onDraw(canvas);

        float expectedTime2 = (2 * VSYNC_INTERVAL_NANOS) * 1E-9f;
        float expectedDelta = VSYNC_INTERVAL_NANOS * 1E-9f;
        assertEquals(expectedTime2, view.getAnimationTime(), FLOAT_TOLERANCE);
        assertEquals(
                expectedTime2,
                remoteContext.getFloat(RemoteContext.ID_ANIMATION_TIME),
                FLOAT_TOLERANCE);
        assertEquals(
                expectedDelta,
                remoteContext.getFloat(RemoteContext.ID_ANIMATION_DELTA_TIME),
                FLOAT_TOLERANCE);

        long frame3Nanos = startNanos + 3 * VSYNC_INTERVAL_NANOS;
        view.mFrameCallback.doFrame(frame3Nanos);
        view.onDraw(canvas);

        float expectedTime3 = (3 * VSYNC_INTERVAL_NANOS) * 1E-9f;
        assertEquals(expectedTime3, view.getAnimationTime(), FLOAT_TOLERANCE);
        assertEquals(
                expectedTime3,
                remoteContext.getFloat(RemoteContext.ID_ANIMATION_TIME),
                FLOAT_TOLERANCE);
        assertEquals(
                expectedDelta,
                remoteContext.getFloat(RemoteContext.ID_ANIMATION_DELTA_TIME),
                FLOAT_TOLERANCE);
    }

    @Test
    public void onDraw_customClock_usesClockNanoTimeInsteadOfChoreographerFrameTime() {
        Context context = ApplicationProvider.getApplicationContext();
        final long[] fakeNanoTime = {1_000_000_000L};
        RemoteClock baseSnapshotClock =
                new SystemClock(Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC));
        RemoteClock customClock =
                new RemoteClock() {
                    @Override
                    public long millis() {
                        return fakeNanoTime[0] / 1_000_000L;
                    }

                    @Override
                    public long nanoTime() {
                        return fakeNanoTime[0];
                    }

                    @Override
                    public @NonNull String getZoneId() {
                        return "UTC";
                    }

                    @Override
                    public @NonNull TimeSnapshot snapshot(@Nullable Long millis) {
                        return baseSnapshotClock.snapshot(millis);
                    }
                };

        RemoteComposeView view = new RemoteComposeView(context);
        view.setDocument(new RemoteDocument(new CoreDocument(customClock)));
        view.layout(0, 0, 100, 100);

        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Even if mFrameCallback.doFrame is invoked with a different timestamp, custom clock
        // documents must continue to use customClock.nanoTime().
        fakeNanoTime[0] = 1_500_000_000L;
        view.mFrameCallback.doFrame(9_000_000_000L);
        view.onDraw(canvas);

        assertEquals(0.5f, view.getAnimationTime(), FLOAT_TOLERANCE);
        assertEquals(
                0.5f,
                view.getRemoteContext().getFloat(RemoteContext.ID_ANIMATION_TIME),
                FLOAT_TOLERANCE);
    }
}
