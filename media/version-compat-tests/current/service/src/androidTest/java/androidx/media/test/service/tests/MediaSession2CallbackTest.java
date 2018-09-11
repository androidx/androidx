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

package androidx.media.test.service.tests;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.media.test.lib.TestUtils;
import androidx.media.test.service.MockPlayer;
import androidx.media.test.service.RemoteMediaController2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.Rating2;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.StarRating2;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2.SessionCallback}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSession2CallbackTest extends MediaSession2TestBase {
    private static final String TAG = "MediaSession2CallbackTest";

    MockPlayer mPlayer;
    RemoteMediaController2 mController2;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testOnCommandRequest() throws InterruptedException {
        prepareLooper();
        mPlayer = new MockPlayer(1);

        final MockOnCommandCallback callback = new MockOnCommandCallback();
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnCommandRequest")
                .build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.pause();
            assertFalse(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            assertFalse(mPlayer.mPauseCalled);
            assertEquals(1, callback.commands.size());
            assertEquals(SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE,
                    (long) callback.commands.get(0).getCommandCode());

            mController2.play();
            assertTrue(mPlayer.mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            assertTrue(mPlayer.mPlayCalled);
            assertFalse(mPlayer.mPauseCalled);
            assertEquals(2, callback.commands.size());
            assertEquals(SessionCommand2.COMMAND_CODE_PLAYBACK_PLAY,
                    (long) callback.commands.get(1).getCommandCode());
        }
    }

    @Test
    public void testOnCustomCommand() throws InterruptedException {
        prepareLooper();
        // TODO(jaewan): Need to revisit with the permission.
        final SessionCommand2 testCommand = new SessionCommand2("testCustomCommand", null);
        final Bundle testArgs = new Bundle();
        testArgs.putString("args", "testOnCustomCommand");

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller) {
                SessionCommandGroup2 commands = new SessionCommandGroup2.Builder()
                        .addAllPredefinedCommands(SessionCommand2.COMMAND_VERSION_1)
                        .addCommand(testCommand)
                        .build();
                return commands;
            }

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

        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnCustomCommand")
                .build()) {
            mController2 = createRemoteController2(session.getToken());
            mController2.sendCustomCommand(testCommand, testArgs, null /* ResultReceiver */);
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
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
            mController2 = createRemoteController2(session.getToken());
            mController2.fastForward();
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
            mController2 = createRemoteController2(session.getToken());
            mController2.rewind();
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPlayFromSearch() throws InterruptedException {
        prepareLooper();
        final String testQuery = "random query";
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPlayFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testQuery, query);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPlayFromSearch").build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.playFromSearch(testQuery, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPlayFromUri() throws InterruptedException {
        prepareLooper();
        final Uri testUri = Uri.parse("foo://boo");
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPlayFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testUri, uri);
                assertTrue(TestUtils.equals(extras, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPlayFromUri")
                .build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.playFromUri(testUri, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPlayFromMediaId() throws InterruptedException {
        prepareLooper();
        final String testMediaId = "media_id";
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPlayFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(mediaId, mediaId);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPlayFromMediaId").build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.playFromMediaId(testMediaId, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPrepareFromSearch() throws InterruptedException {
        prepareLooper();
        final String testQuery = "random query";
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPrepareFromSearch(MediaSession2 session, ControllerInfo controller,
                    String query, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testQuery, query);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPrepareFromSearch").build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.prepareFromSearch(testQuery, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPrepareFromUri() throws InterruptedException {
        prepareLooper();
        final Uri testUri = Uri.parse("foo://boo");
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPrepareFromUri(MediaSession2 session, ControllerInfo controller, Uri uri,
                    Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testUri, uri);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPrepareFromUri").build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.prepareFromUri(testUri, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnPrepareFromMediaId() throws InterruptedException {
        prepareLooper();
        final String testMediaId = "media_id";
        final Bundle testExtras = TestUtils.createTestBundle();
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onPrepareFromMediaId(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Bundle extras) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testMediaId, mediaId);
                assertTrue(TestUtils.equals(testExtras, extras));
                latch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnPrepareFromMediaId").build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.prepareFromMediaId(testMediaId, testExtras);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnSetRating() throws InterruptedException {
        prepareLooper();
        final float ratingValue = 3.5f;
        final Rating2 testRating = new StarRating2(5, ratingValue);
        final String testMediaId = "media_id";

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaSession2.SessionCallback callback = new MediaSession2.SessionCallback() {
            @Override
            public void onSetRating(MediaSession2 session, ControllerInfo controller,
                    String mediaId, Rating2 rating) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertEquals(testMediaId, mediaId);
                assertEquals(testRating, rating);
                latch.countDown();
            }
        };

        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSetRating").build()) {
            mController2 = createRemoteController2(session.getToken());

            mController2.setRating(testMediaId, testRating);
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
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSubscribeRoutesInfo")
                .build()) {
            mController2 = createRemoteController2(session.getToken());

            callback.resetLatchCount(1);
            mController2.subscribeRoutesInfo();
            assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
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
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnUnsubscribeRoutesInfo")
                .build()) {
            mController2 = createRemoteController2(session.getToken());

            callback.resetLatchCount(1);
            mController2.unsubscribeRoutesInfo();
            assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnSelectRoute() throws InterruptedException {
        prepareLooper();
        final Bundle testRoute = TestUtils.createTestBundle();
        final TestSessionCallback callback = new TestSessionCallback() {
            @Override
            public void onSelectRoute(@NonNull MediaSession2 session,
                    @NonNull ControllerInfo controller, @NonNull Bundle route) {
                assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                assertTrue(TestUtils.equals(testRoute, route));
                mLatch.countDown();
            }
        };
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setSessionCallback(sHandlerExecutor, callback)
                .setId("testOnSelectRoute")
                .build()) {
            mController2 = createRemoteController2(session.getToken());

            callback.resetLatchCount(1);
            mController2.selectRoute(testRoute);
            assertTrue(callback.mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnConnect() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setId("testOnConnect")
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            ControllerInfo controller) {
                        // TODO: Get uid of client app's and compare.
                        if (!CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return null;
                        }
                        latch.countDown();
                        return super.onConnect(session, controller);
                    }
                }).build()) {
            mController2 = createRemoteController2(
                    session.getToken(), false /* waitForConnection */);
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnDisconnected() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setId("testOnDisconnected")
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public void onDisconnected(MediaSession2 session,
                            ControllerInfo controller) {
                        assertEquals(CLIENT_PACKAGE_NAME, controller.getPackageName());
                        // TODO: Get uid of client app's and compare.
                        latch.countDown();
                    }
                }).build()) {
            mController2 = createRemoteController2(session.getToken());
            mController2.close();
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
    }


    // TODO(jaewan): Add test for service connect rejection, when we differentiate session
    //               active/inactive and connection accept/refuse
    class TestSessionCallback extends MediaSession2.SessionCallback {
        CountDownLatch mLatch;

        void resetLatchCount(int count) {
            mLatch = new CountDownLatch(count);
        }
    }

    public class MockOnCommandCallback extends MediaSession2.SessionCallback {
        public final ArrayList<SessionCommand2> commands = new ArrayList<>();

        @Override
        public boolean onCommandRequest(MediaSession2 session, ControllerInfo controllerInfo,
                SessionCommand2 command) {
            // TODO: Get uid of client app's and compare.
            assertEquals(CLIENT_PACKAGE_NAME, controllerInfo.getPackageName());
            assertFalse(controllerInfo.isTrusted());
            commands.add(command);
            if (command.getCommandCode() == SessionCommand2.COMMAND_CODE_PLAYBACK_PAUSE) {
                return false;
            }
            return true;
        }
    }
}
