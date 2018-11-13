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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;

import androidx.media.test.service.MockMediaSessionService;
import androidx.media.test.service.MockPlayer;
import androidx.media.test.service.RemoteMediaController;
import androidx.media.test.service.TestServiceRegistry;
import androidx.media2.MediaSession;
import androidx.media2.MediaSessionService;
import androidx.media2.SessionToken;
import androidx.test.filters.SmallTest;

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
@SmallTest
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

    @Test
    public void testOnGetSession() throws InterruptedException {
        prepareLooper();
        final List<SessionToken> tokens = new ArrayList<>();
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession onGetSession() {
                        MockPlayer player = new MockPlayer(1);
                        MediaSession session = new MediaSession.Builder(mContext, player)
                                .setSessionCallback(sHandlerExecutor,
                                        new MediaSession.SessionCallback() {})
                                .setId("testOnGetSession" + System.currentTimeMillis()).build();
                        tokens.add(session.getToken());
                        return session;
                    }
                });

        RemoteMediaController controller1 = createRemoteController(mToken, true);
        RemoteMediaController controller2 = createRemoteController(mToken, true);

        assertNotEquals(controller1.getConnectedSessionToken(),
                controller2.getConnectedSessionToken());
        assertEquals(tokens.get(0), controller1.getConnectedSessionToken());
        assertEquals(tokens.get(1), controller2.getConnectedSessionToken());
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

        RemoteMediaController controller1 = createRemoteController(mToken, true);
        RemoteMediaController controller2 = createRemoteController(mToken, true);
        controller1.close();
        controller2.close();

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAllControllersDisconnected_multipleSession() throws InterruptedException {
        prepareLooper();
        TestServiceRegistry.getInstance().setOnGetSessionHandler(
                new TestServiceRegistry.OnGetSessionHandler() {
                    @Override
                    public MediaSession onGetSession() {
                        MockPlayer player = new MockPlayer(1);
                        MediaSession session = new MediaSession.Builder(mContext, player)
                                .setSessionCallback(sHandlerExecutor,
                                        new MediaSession.SessionCallback() {})
                                .setId("testAllControllersDisconnected"
                                        + System.currentTimeMillis()).build();
                        return session;
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

        RemoteMediaController controller1 = createRemoteController(mToken, true);
        RemoteMediaController controller2 = createRemoteController(mToken, true);
        controller1.close();
        controller2.close();

        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetSessions() throws InterruptedException {
        prepareLooper();
        RemoteMediaController controller = createRemoteController(mToken, true);
        MediaSessionService service = TestServiceRegistry.getInstance().getServiceInstance();
        try (MediaSession session = new MediaSession.Builder(mContext, new MockPlayer(0))
                .setId("testGetSessions")
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() { })
                .build()) {
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
        RemoteMediaController controller = createRemoteController(mToken, true);
        MediaSessionService service = TestServiceRegistry.getInstance().getServiceInstance();
        try (MediaSession session = new MediaSession.Builder(mContext, new MockPlayer(0))
                .setId("testAddSessions_removedWhenClose")
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() { })
                .build()) {
            service.addSession(session);
            List<MediaSession> sessions = service.getSessions();
            assertTrue(sessions.contains(session));
            assertEquals(2, sessions.size());

            session.close();
            sessions = service.getSessions();
            assertFalse(sessions.contains(session));
        }
    }
}
