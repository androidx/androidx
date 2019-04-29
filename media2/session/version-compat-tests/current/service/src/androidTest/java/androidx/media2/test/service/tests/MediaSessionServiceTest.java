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

package androidx.media2.test.service.tests;

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.media2.session.MediaController;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.MediaSessionService;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionToken;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.service.MockMediaSessionService;
import androidx.media2.test.service.MockPlayer;
import androidx.media2.test.service.RemoteMediaController;
import androidx.media2.test.service.TestServiceRegistry;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSessionService}.
 */
@MediumTest
public class MediaSessionServiceTest extends MediaSessionTestBase {
    private SessionToken mToken;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        TestServiceRegistry.getInstance().setHandler(sHandler);
        mToken = new SessionToken(mContext,
                new ComponentName(mContext, MockMediaSessionService.class));
    }

    @Override
    @After
    public void cleanUp() throws Exception {
        super.cleanUp();
        TestServiceRegistry.getInstance().cleanUp();
    }


    /**
     * Tests whether {@link MediaSessionService#onGetSession(ControllerInfo)}
     * is called when controller tries to connect, with the proper arguments.
     */
    @Test
    public void testOnGetSessionIsCalled() throws InterruptedException {
        final List<ControllerInfo> controllerInfoList = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession onGetSession(ControllerInfo controllerInfo) {
                        controllerInfoList.add(controllerInfo);
                        latch.countDown();
                        return null;
                    }
                });

        Bundle testHints = new Bundle();
        testHints.putString("test_key", "test_value");
        RemoteMediaController controller = createRemoteController(mToken, true, testHints);

        // onGetSession() should be called.
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(CLIENT_PACKAGE_NAME, controllerInfoList.get(0).getPackageName());
        assertTrue(TestUtils.equals(controllerInfoList.get(0).getConnectionHints(), testHints));
    }


    /**
     * Tests whether the controller is connected to the session which is returned from
     * {@link MediaSessionService#onGetSession(ControllerInfo)}.
     * Also checks whether the connection hints are properly passed to
     * {@link MediaSession.SessionCallback#onConnect(MediaSession, ControllerInfo)}.
     */
    @Test
    public void testOnGetSession_returnsSession() throws InterruptedException {
        prepareLooper();
        final List<ControllerInfo> controllerInfoList = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        try (MediaSession testSession = new MediaSession.Builder(mContext, new MockPlayer(0))
                .setId("testOnGetSession_returnsSession")
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(MediaSession session,
                            ControllerInfo controller) {
                        controllerInfoList.add(controller);
                        latch.countDown();
                        return new SessionCommandGroup.Builder().build();
                    }
                }).build()) {

            TestServiceRegistry.getInstance().setOnGetSessionHandler(
                    new TestServiceRegistry.OnGetSessionHandler() {
                        @Override
                        public MediaSession onGetSession(ControllerInfo controllerInfo) {
                            return testSession;
                        }
                    });

            Bundle testHints = new Bundle();
            testHints.putString("test_key", "test_value");
            RemoteMediaController controller = createRemoteController(mToken, true, testHints);

            // MediaSession.SessionCallback#onConnect() should be called.
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertEquals(CLIENT_PACKAGE_NAME, controllerInfoList.get(0).getPackageName());
            assertTrue(TestUtils.equals(controllerInfoList.get(0).getConnectionHints(), testHints));

            // The controller should be connected to the right session.
            assertNotEquals(mToken, controller.getConnectedSessionToken());
            assertEquals(testSession.getToken(), controller.getConnectedSessionToken());
        }
    }

    /**
     * Tests whether {@link MediaSessionService#onGetSession(ControllerInfo)}
     * can return different sessions for different controllers.
     */
    @Test
    public void testOnGetSession_returnsDifferentSessions() {
        prepareLooper();
        final List<SessionToken> tokens = new ArrayList<>();
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession onGetSession(ControllerInfo controllerInfo) {
                        MediaSession session = createMediaSession(
                                "testOnGetSession_returnsDifferentSessions"
                                        + System.currentTimeMillis());
                        tokens.add(session.getToken());
                        return session;
                    }
                });

        RemoteMediaController controller1 = createRemoteController(mToken, true, null);
        RemoteMediaController controller2 = createRemoteController(mToken, true, null);

        assertNotEquals(controller1.getConnectedSessionToken(),
                controller2.getConnectedSessionToken());
        assertEquals(tokens.get(0), controller1.getConnectedSessionToken());
        assertEquals(tokens.get(1), controller2.getConnectedSessionToken());
    }


    /**
     * Tests whether {@link MediaSessionService#onGetSession(ControllerInfo)}
     * can reject incoming connection by returning null.
     */
    @Test
    public void testOnGetSession_rejectsConnection() throws InterruptedException {
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession onGetSession(ControllerInfo controllerInfo) {
                        return null;
                    }
                });
        final CountDownLatch latch = new CountDownLatch(1);
        MediaController controller = new MediaController.Builder(mContext)
                .setSessionToken(mToken)
                .setControllerCallback(sHandlerExecutor, new MediaController.ControllerCallback() {
                    @Override
                    public void onDisconnected(MediaController controller) {
                        latch.countDown();
                    }
                })
                .build();

        // MediaController2.ControllerCallback#onDisconnected() should be called.
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertNull(controller.getConnectedToken());
        controller.close();
    }

    @Test
    public void testAllControllersDisconnected_oneSession() throws InterruptedException {
        prepareLooper();
        final CountDownLatch latch = new CountDownLatch(1);
        TestServiceRegistry.getInstance().setSessionServiceCallback(
                new TestServiceRegistry.SessionServiceCallback() {
                    @Override
                    public void onCreated() {
                        // no-op
                    }

                    @Override
                    public void onDestroyed() {
                        latch.countDown();
                    }
                });

        RemoteMediaController controller1 = createRemoteController(mToken, true, null);
        RemoteMediaController controller2 = createRemoteController(mToken, true, null);
        controller1.close();
        controller2.close();

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAllControllersDisconnected_multipleSessions() throws InterruptedException {
        prepareLooper();
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession onGetSession(ControllerInfo controllerInfo) {
                        return createMediaSession("testAllControllersDisconnected"
                                + System.currentTimeMillis());
                    }
                });
        final CountDownLatch latch = new CountDownLatch(1);
        TestServiceRegistry.getInstance().setSessionServiceCallback(
                new TestServiceRegistry.SessionServiceCallback() {
                    @Override
                    public void onCreated() {
                        // no-op
                    }

                    @Override
                    public void onDestroyed() {
                        latch.countDown();
                    }
                });

        RemoteMediaController controller1 = createRemoteController(mToken, true, null);
        RemoteMediaController controller2 = createRemoteController(mToken, true, null);

        controller1.close();
        assertFalse(latch.await(WAIT_TIME_FOR_NO_RESPONSE_MS, TimeUnit.MILLISECONDS));

        // Service should be closed only when all controllers are closed.
        controller2.close();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetSessions() throws InterruptedException {
        prepareLooper();
        RemoteMediaController controller = createRemoteController(mToken, true, null);
        MediaSessionService service = TestServiceRegistry.getInstance().getServiceInstance();
        try (MediaSession session = createMediaSession("testGetSessions")) {
            service.addSession(session);
            List<MediaSession> sessions = service.getSessions();
            assertTrue(sessions.contains(session));
            assertEquals(2, sessions.size());

            service.removeSession(session);
            sessions = service.getSessions();
            assertFalse(sessions.contains(session));
        }
    }

    @Test
    public void testAddSessions_removedWhenClose() throws InterruptedException {
        prepareLooper();
        RemoteMediaController controller = createRemoteController(mToken, true, null);
        MediaSessionService service = TestServiceRegistry.getInstance().getServiceInstance();
        try (MediaSession session = createMediaSession("testAddSessions_removedWhenClose")) {
            service.addSession(session);
            List<MediaSession> sessions = service.getSessions();
            assertTrue(sessions.contains(session));
            assertEquals(2, sessions.size());

            session.close();
            sessions = service.getSessions();
            assertFalse(sessions.contains(session));
        }
    }

    private MediaSession createMediaSession(String id) {
        return new MediaSession.Builder(mContext, new MockPlayer(0))
                .setId(id)
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {})
                .build();
    }
}
