/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media.test.service;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static androidx.media.test.lib.CommonConstants.KEY_ARGUMENTS;
import static androidx.media.test.lib.CommonConstants.KEY_COMMAND;
import static androidx.media.test.lib.CommonConstants.KEY_EXTRAS;
import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ID;
import static androidx.media.test.lib.CommonConstants.KEY_QUERY;
import static androidx.media.test.lib.CommonConstants.KEY_RATING;
import static androidx.media.test.lib.CommonConstants.KEY_RESULT_RECEIVER;
import static androidx.media.test.lib.CommonConstants.KEY_ROUTE;
import static androidx.media.test.lib.CommonConstants.KEY_URI;
import static androidx.media.test.lib.MediaController2Constants.FAST_FORWARD;
import static androidx.media.test.lib.MediaController2Constants.PLAY_FROM_MEDIA_ID;
import static androidx.media.test.lib.MediaController2Constants.PLAY_FROM_SEARCH;
import static androidx.media.test.lib.MediaController2Constants.PLAY_FROM_URI;
import static androidx.media.test.lib.MediaController2Constants.PREPARE_FROM_MEDIA_ID;
import static androidx.media.test.lib.MediaController2Constants.PREPARE_FROM_SEARCH;
import static androidx.media.test.lib.MediaController2Constants.PREPARE_FROM_URI;
import static androidx.media.test.lib.MediaController2Constants.REWIND;
import static androidx.media.test.lib.MediaController2Constants.SELECT_ROUTE;
import static androidx.media.test.lib.MediaController2Constants.SEND_CUSTOM_COMMAND;
import static androidx.media.test.lib.MediaController2Constants.SET_RATING;
import static androidx.media.test.lib.MediaController2Constants.SUBSCRIBE_ROUTES_INFO;
import static androidx.media.test.lib.MediaController2Constants.UNSUBSCRIBE_ROUTES_INFO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.NonNull;
import androidx.media.MediaSession2;
import androidx.media.MediaSession2.ControllerInfo;
import androidx.media.Rating2;
import androidx.media.SessionCommand2;
import androidx.media.SessionCommandGroup2;
import androidx.media.test.lib.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2.SessionCallback}.
 */
// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaSession2CallbackTest extends MediaSession2TestBase {
    private static final String TAG = "MediaSession2CallbackTest";

    MediaSession2 mSession;
    MockPlayer mPlayer;
    MockPlaylistAgent mMockAgent;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);
        mMockAgent = new MockPlaylistAgent();
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setPlaylistAgent(mMockAgent)
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            MediaSession2.ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                })
                .setId(TAG)
                .build();
        TestServiceRegistry.getInstance().setHandler(sHandler);
        mTestHelper.createDefaultController2(mSession.getToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
        TestServiceRegistry.getInstance().cleanUp();
    }


    @Test
    public void testOnCustomCommand() throws InterruptedException {
        prepareLooper();
        // TODO(jaewan): Need to revisit with the permission.
        final SessionCommand2 testCommand =
                new SessionCommand2(SessionCommand2.COMMAND_CODE_PLAYBACK_PREPARE);
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "testOnCustomCommand");

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onCustomCommand(MediaSession2 session,
                    MediaSession2.ControllerInfo controller,
                    SessionCommand2 customCommand, Bundle args, ResultReceiver cb) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testCommand, customCommand);
                assertTrue(TestUtils.equals(testArgs, args));
                assertNull(cb);
                latch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        mTestHelper.createDefaultController2(mSession.getToken());

        Bundle args = new Bundle();
        args.putBundle(KEY_COMMAND, testCommand.toBundle());
        args.putBundle(KEY_ARGUMENTS, testArgs);
        args.putParcelable(KEY_RESULT_RECEIVER, null);
        mTestHelper.callMediaController2Method(SEND_CUSTOM_COMMAND, args);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnFastForward() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onFastForward(MediaSession2 session, ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnFastForward").build()) {
            mTestHelper.createDefaultController2(session.getToken());
            mTestHelper.callMediaController2Method(FAST_FORWARD, null);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnRewind() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onRewind(MediaSession2 session, ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnRewind").build()) {
            mTestHelper.createDefaultController2(session.getToken());
            mTestHelper.callMediaController2Method(REWIND, null);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPlayFromSearch() throws InterruptedException {
        prepareLooper();
        final String request = "random query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPlayFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPlayFromSearch").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putString(KEY_QUERY, request);
            args.putBundle(KEY_EXTRAS, bundle);
            mTestHelper.callMediaController2Method(PLAY_FROM_SEARCH, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPlayFromUri() throws InterruptedException {
        prepareLooper();
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPlayFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPlayFromUri").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putParcelable(KEY_URI, request);
            args.putBundle(KEY_EXTRAS, bundle);
            mTestHelper.callMediaController2Method(PLAY_FROM_URI, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPlayFromMediaId() throws InterruptedException {
        prepareLooper();
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPlayFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPlayFromMediaId").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putString(KEY_MEDIA_ID, request);
            args.putBundle(KEY_EXTRAS, bundle);
            mTestHelper.callMediaController2Method(PLAY_FROM_MEDIA_ID, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPrepareFromSearch() throws InterruptedException {
        prepareLooper();
        final String request = "random query";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPrepareFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, query);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPrepareFromSearch").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putString(KEY_QUERY, request);
            args.putBundle(KEY_EXTRAS, bundle);
            mTestHelper.callMediaController2Method(PREPARE_FROM_SEARCH, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPrepareFromUri() throws InterruptedException {
        prepareLooper();
        final Uri request = Uri.parse("foo://boo");
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPrepareFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, uri);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPrepareFromUri").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putParcelable(KEY_URI, request);
            args.putBundle(KEY_EXTRAS, bundle);
            mTestHelper.callMediaController2Method(PREPARE_FROM_URI, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPrepareFromMediaId() throws InterruptedException {
        prepareLooper();
        final String request = "media_id";
        final Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPrepareFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(request, mediaId);
                assertTrue(TestUtils.equals(bundle, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPrepareFromMediaId").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putString(KEY_MEDIA_ID, request);
            args.putBundle(KEY_EXTRAS, bundle);
            mTestHelper.callMediaController2Method(PREPARE_FROM_MEDIA_ID, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnSetRating() throws InterruptedException {
        prepareLooper();
        final int ratingType = Rating2.RATING_5_STARS;
        final float ratingValue = 3.5f;
        final Rating2 rating = Rating2.newStarRating(ratingType, ratingValue);
        final String mediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onSetRating(MediaSession2 session, ControllerInfo controller,
                    String mediaIdOut, Rating2 ratingOut) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(mediaId, mediaIdOut);
                assertEquals(rating, ratingOut);
                latch.countDown();
            }
        };

        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSetRating").build()) {
            mTestHelper.createDefaultController2(session.getToken());

            Bundle args = new Bundle();
            args.putString(KEY_MEDIA_ID, mediaId);
            args.putBundle(KEY_RATING, rating.toBundle());
            mTestHelper.callMediaController2Method(SET_RATING, args);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnSubscribeRoutesInfo() throws InterruptedException {
        prepareLooper();
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public void onSubscribeRoutesInfo(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                mLatch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        mTestHelper.createDefaultController2(mSession.getToken());

        callback.resetLatchCount(1);
        mTestHelper.callMediaController2Method(SUBSCRIBE_ROUTES_INFO, null);
        assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnUnsubscribeRoutesInfo() throws InterruptedException {
        prepareLooper();
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public void onUnsubscribeRoutesInfo(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                mLatch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        mTestHelper.createDefaultController2(mSession.getToken());

        callback.resetLatchCount(1);
        mTestHelper.callMediaController2Method(UNSUBSCRIBE_ROUTES_INFO, null);
        assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testOnSelectRoute() throws InterruptedException {
        prepareLooper();
        final Bundle testRoute = new Bundle();
        testRoute.putString("id", "testOnSelectRoute");
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public void onSelectRoute(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller, @NonNull Bundle route) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertTrue(TestUtils.equals(route, testRoute));
                mLatch.countDown();
            }
        };
        mSession.close();
        mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback).setId(TAG).build();
        mTestHelper.createDefaultController2(mSession.getToken());

        callback.resetLatchCount(1);
        Bundle args = new Bundle();
        args.putBundle(KEY_ROUTE, testRoute);
        mTestHelper.callMediaController2Method(SELECT_ROUTE, args);
        assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // TODO(jaewan): Add test for service connect rejection, when we differentiate session
    //               active/inactive and connection accept/refuse

    class TestSessionCallback extends MediaSession2.SessionCallback {
        CountDownLatch mLatch;

        void resetLatchCount(int count) {
            mLatch = new CountDownLatch(count);
        }
    }
}
