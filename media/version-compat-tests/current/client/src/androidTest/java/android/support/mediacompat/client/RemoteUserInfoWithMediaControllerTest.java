/*
 * Copyright 2020 The Android Open Source Project
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

package android.support.mediacompat.client;

import static android.support.mediacompat.testlib.MediaSessionConstants.ROOT_HINT_EXTRA_KEY_CALLER_PKG;
import static android.support.mediacompat.testlib.MediaSessionConstants.ROOT_HINT_EXTRA_KEY_CALLER_UID;
import static android.support.mediacompat.testlib.MediaSessionConstants.SESSION_EVENT_NOTIFY_CALLBACK_METHOD_NAME_PREFIX;
import static android.support.mediacompat.testlib.VersionConstants.KEY_SERVICE_VERSION;
import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.test.platform.app.InstrumentationRegistry.getArguments;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test of {@link MediaSessionCompat#getCurrentControllerInfo()} with all
 * {@link MediaController} methods.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 22) // b/278620526
@LargeTest
public class RemoteUserInfoWithMediaControllerTest {
    private static final String TAG = "RemoteUserInfoFwk";
    private static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME,
            "android.support.mediacompat.service.StubMediaBrowserServiceCompat");
    private static final long CONNECTION_TIMEOUT_MS = 3_000;
    private static final long TIMEOUT_MS = 1_000;

    private String mServiceVersion;
    private MediaBrowserCompat mMediaBrowser;
    private MediaController mMediaController;
    private ControllerCallback mMediaControllerCallback;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws InterruptedException {
        mServiceVersion = getArguments().getString(KEY_SERVICE_VERSION, "");
        Log.d(TAG, "Service app version: " + mServiceVersion);
        Context context = getInstrumentation().getContext();
        CountDownLatch connectionLatch = new CountDownLatch(1);
        AtomicReference<MediaSessionCompat.Token> tokenRef = new AtomicReference<>();
        getInstrumentation().runOnMainSync(() -> {
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper();
            }

            MediaBrowserCompat.ConnectionCallback connectionCallback =
                    new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    tokenRef.set(mMediaBrowser.getSessionToken());
                    connectionLatch.countDown();
                }

                @Override
                public void onConnectionSuspended() {
                    connectionLatch.countDown();
                }

                @Override
                public void onConnectionFailed() {
                    connectionLatch.countDown();
                }
            };
            Bundle rootHints = new Bundle();
            rootHints.putString(ROOT_HINT_EXTRA_KEY_CALLER_PKG, context.getPackageName());
            rootHints.putInt(ROOT_HINT_EXTRA_KEY_CALLER_UID, Process.myUid());
            mMediaBrowser = new MediaBrowserCompat(getInstrumentation().getTargetContext(),
                    TEST_BROWSER_SERVICE, connectionCallback, rootHints);
            mMediaBrowser.connect();
        });
        assertTrue("Failed to connect to service",
                connectionLatch.await(CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        MediaSessionCompat.Token token = tokenRef.get();
        assertNotNull(token);
        mMediaController = new MediaController(context, (MediaSession.Token) token.getToken());
        mMediaControllerCallback = new ControllerCallback();
        mMediaController.registerCallback(
                mMediaControllerCallback,
                new Handler(Looper.getMainLooper()));
    }

    @After
    public void tearDown() {
        getInstrumentation().runOnMainSync(() -> {
            if (mMediaBrowser != null) {
                mMediaBrowser.disconnect();
                mMediaBrowser = null;
            }
        });
    }

    @Test
    public void testSendCommand() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onCommand");
        mMediaController.sendCommand("anyCommand", /* extras= */ null, /* cb= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testMediaButtonEvent() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onMediaButtonEvent");
        mMediaController.dispatchMediaButtonEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testPrepare() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPrepare");
        mMediaController.getTransportControls().prepare();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testPrepareFromMediaId() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPrepareFromMediaId");
        mMediaController.getTransportControls().prepareFromMediaId(
                "anyMediaId", /* extras= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testPrepareFromSearch() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPrepareFromSearch");
        mMediaController.getTransportControls().prepareFromSearch(
                "anySearchQuery", /* extras= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    public void testPrepareFromUri() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPrepareFromUri");
        mMediaController.getTransportControls().prepareFromUri(
                Uri.parse("https://test.com"), /* extras= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testPlay() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPlay");
        mMediaController.getTransportControls().play();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testPlayFromMediaId() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPlayFromMediaId");
        mMediaController.getTransportControls().playFromMediaId(
                "anyMediaId", /* extras= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testPlayFromSearch() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPlayFromSearch");
        mMediaController.getTransportControls().playFromSearch(
                "anySearchQuery", /* extras= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void testPlayFromUri() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onPlayFromUri");
        mMediaController.getTransportControls().playFromUri(
                Uri.parse("https://test.com"), /* extras= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testSkipToQueueId() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onSkipToQueueItem");
        mMediaController.getTransportControls().skipToQueueItem(/* id= */ 0);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testSkipToNext() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onSkipToNext");
        mMediaController.getTransportControls().skipToNext();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testSkipToPrevious() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onSkipToPrevious");
        mMediaController.getTransportControls().skipToPrevious();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testFastForward() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onFastForward");
        mMediaController.getTransportControls().fastForward();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testRewind() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onRewind");
        mMediaController.getTransportControls().rewind();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testStop() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onStop");
        mMediaController.getTransportControls().stop();
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testSetRating() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onSetRating");
        mMediaController.getTransportControls().setRating(
                Rating.newHeartRating(/* hasHeart= */ true));
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testSetPlaybackSpeed() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onSetPlaybackSpeed");
        mMediaController.getTransportControls().setPlaybackSpeed(/* speed= */ 1.0f);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    @Test
    public void testCustomAction() throws InterruptedException {
        mMediaControllerCallback.setExpectedCallbackMethodName("onCustomAction");
        mMediaController.getTransportControls().sendCustomAction("anyAction", /* args= */ null);
        mMediaControllerCallback.assertThatSessionHasReceivedExpectedCallback();
    }

    private class ControllerCallback extends MediaController.Callback {
        private final CountDownLatch mCountDownLatch = new CountDownLatch(1);
        private volatile String mExpectedCallbackMethodName;

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            if (!event.startsWith(SESSION_EVENT_NOTIFY_CALLBACK_METHOD_NAME_PREFIX)) {
                return;
            }
            String callbackMethodName =
                    event.substring(SESSION_EVENT_NOTIFY_CALLBACK_METHOD_NAME_PREFIX.length());
            if (TextUtils.equals(callbackMethodName, mExpectedCallbackMethodName)) {
                mCountDownLatch.countDown();
            }
        }

        public void setExpectedCallbackMethodName(String expectedCallbackMethodName) {
            mExpectedCallbackMethodName = expectedCallbackMethodName;
        }

        public void assertThatSessionHasReceivedExpectedCallback() throws InterruptedException {
            assertTrue(mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }
}
