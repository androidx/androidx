/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static org.junit.Assert.assertTrue;

import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaController} with framework MediaSession.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP) // For framework MediaSession
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaController_FrameworkMediaSessionTest extends MediaSessionTestBase {
    private static final String TAG = "MediaController_FrameworkMediaSessionTest";

    private android.media.session.MediaSession mFwkSession;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mFwkSession = new android.media.session.MediaSession(mContext, TAG);
        mFwkSession.setActive(true);
        mFwkSession.setFlags(android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mFwkSession.setCallback(new android.media.session.MediaSession.Callback() {});
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mFwkSession.release();
    }

    @Test
    public void connect() throws Exception {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        MediaController.ControllerCallback callback = new MediaController.ControllerCallback() {
            @Override
            public void onConnected(@NonNull MediaController controller,
                    @NonNull SessionCommandGroup allowedCommands) {
                connectedLatch.countDown();
            }
        };
        try (MediaController controller = new MediaController.Builder(mContext)
                .setSessionCompatToken(
                        MediaSessionCompat.Token.fromToken(mFwkSession.getSessionToken()))
                .setControllerCallback(sHandlerExecutor, callback)
                .build()) {
            assertTrue(connectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void playerStateChanged() throws Exception {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch playerStateChangedLatch = new CountDownLatch(1);
        MediaController.ControllerCallback callback = new MediaController.ControllerCallback() {
            @Override
            public void onConnected(@NonNull MediaController controller,
                    @NonNull SessionCommandGroup allowedCommands) {
                connectedLatch.countDown();
            }

            @Override
            public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
                playerStateChangedLatch.countDown();
            }
        };
        try (MediaController controller = new MediaController.Builder(mContext)
                .setSessionCompatToken(
                        MediaSessionCompat.Token.fromToken(mFwkSession.getSessionToken()))
                .setControllerCallback(sHandlerExecutor, callback)
                .build()) {
            assertTrue(connectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            mFwkSession.setPlaybackState(new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                    .build());
            assertTrue(playerStateChangedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }
}
