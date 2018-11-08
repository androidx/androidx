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

package androidx.media2;

import android.content.Context;
import android.media.session.MediaSessionManager;

import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSession.SessionCallback;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link MediaSessionManager} with {@link MediaSession} specific APIs.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
@Ignore
public class MediaSessionManager_MediaSessionTest extends MediaSessionTestBase {
    private static final String TAG = "MediaSessionManager_MediaSessionTest";

    private MediaSessionManager mManager;
    private MediaSession mSession;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mManager = (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);

        // Specify TAG here so {@link MediaSession.getInstance()} doesn't complaint about
        // per test thread differs across the {@link MediaSession} with the same TAG.
        final MockPlayer player = new MockPlayer(1);
        mSession = new MediaSession.Builder(mContext, player)
                .setSessionCallback(sHandlerExecutor, new SessionCallback() { })
                .setId(TAG)
                .build();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        sHandler.removeCallbacksAndMessages(null);
        mSession.close();
    }

    // TODO(jaewan): Make this host-side test to see per-user behavior.
    @Ignore
    @Test
    public void testGetMediaSessionTokens_hasMediaController() throws InterruptedException {
        prepareLooper();
        final MockPlayer player = (MockPlayer) mSession.getPlayer();
        player.notifyPlayerStateChanged(SessionPlayer.PLAYER_STATE_IDLE);

        MediaController controller = null;
//        List<SessionToken> tokens = mManager.getActiveSessionTokens();
//        assertNotNull(tokens);
//        for (int i = 0; i < tokens.size(); i++) {
//            SessionToken token = tokens.get(i);
//            if (mContext.getPackageName().equals(token.getPackageName())
//                    && TAG.equals(token.getSessionId())) {
//                assertNull(controller);
//                controller = createController(token);
//            }
//        }
//        assertNotNull(controller);
//
//        // Test if the found controller is correct one.
//        assertEquals(SessionPlayer.PLAYER_STATE_IDLE, controller.getPlayerState());
//        controller.play();
//
//        assertTrue(player.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//        assertTrue(player.mPlayCalled);
    }

    /**
     * Test if server recognizes a session even if the session refuses the connection from server.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetSessionTokens_sessionRejected() throws InterruptedException {
        prepareLooper();
        mSession.close();
        mSession = new MediaSession.Builder(mContext, new MockPlayer(0))
                .setId(TAG).setSessionCallback(sHandlerExecutor, new SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(
                            MediaSession session, ControllerInfo controller) {
                        // Reject all connection request.
                        return null;
                    }
                }).build();

        boolean foundSession = false;
//        List<SessionToken> tokens = mManager.getActiveSessionTokens();
//        assertNotNull(tokens);
//        for (int i = 0; i < tokens.size(); i++) {
//            SessionToken token = tokens.get(i);
//            if (mContext.getPackageName().equals(token.getPackageName())
//                    && TAG.equals(token.getSessionId())) {
//                assertFalse(foundSession);
//                foundSession = true;
//            }
//        }
//        assertTrue(foundSession);
    }

    @Test
    public void testGetMediaSessionTokens_sessionClosed() throws InterruptedException {
        prepareLooper();
        mSession.close();

        // When a session is closed, it should lose binder connection between server immediately.
        // So server will forget the session.
//        List<SessionToken> tokens = mManager.getActiveSessionTokens();
//        for (int i = 0; i < tokens.size(); i++) {
//            SessionToken token = tokens.get(i);
//            assertFalse(mContext.getPackageName().equals(token.getPackageName())
//                    && TAG.equals(token.getSessionId()));
//        }
    }

    @Test
    public void testGetMediaSessionServiceToken() throws InterruptedException {
        prepareLooper();
        boolean foundTestSessionService = false;
        boolean foundTestLibraryService = false;
//        List<SessionToken> tokens = mManager.getSessionServiceTokens();
//        for (int i = 0; i < tokens.size(); i++) {
//            SessionToken token = tokens.get(i);
//            if (mContext.getPackageName().equals(token.getPackageName())
//                    && MockMediaSessionService.ID.equals(token.getSessionId())) {
//                assertFalse(foundTestSessionService);
//                assertEquals(SessionToken.TYPE_SESSION_SERVICE, token.getType());
//                foundTestSessionService = true;
//            } else if (mContext.getPackageName().equals(token.getPackageName())
//                    && MockMediaLibraryService.ID.equals(token.getSessionId())) {
//                assertFalse(foundTestLibraryService);
//                assertEquals(SessionToken.TYPE_LIBRARY_SERVICE, token.getType());
//                foundTestLibraryService = true;
//            }
//        }
//        assertTrue(foundTestSessionService);
//        assertTrue(foundTestLibraryService);
    }

    @Test
    public void testGetAllSessionTokens() throws InterruptedException {
        prepareLooper();
        boolean foundTestSession = false;
        boolean foundTestSessionService = false;
        boolean foundTestLibraryService = false;
//        List<SessionToken> tokens = mManager.getAllSessionTokens();
//        for (int i = 0; i < tokens.size(); i++) {
//            SessionToken token = tokens.get(i);
//            if (!mContext.getPackageName().equals(token.getPackageName())) {
//                continue;
//            }
//            switch (token.getSessionId()) {
//                case TAG:
//                    assertFalse(foundTestSession);
//                    foundTestSession = true;
//                    break;
//                case MockMediaSessionService.ID:
//                    assertFalse(foundTestSessionService);
//                    foundTestSessionService = true;
//                    assertEquals(SessionToken.TYPE_SESSION_SERVICE, token.getType());
//                    break;
//                case MockMediaLibraryService.ID:
//                    assertFalse(foundTestLibraryService);
//                    assertEquals(SessionToken.TYPE_LIBRARY_SERVICE, token.getType());
//                    foundTestLibraryService = true;
//                    break;
//                default:
//                    fail("Unexpected session " + token + " exists in the package");
//            }
//        }
//        assertTrue(foundTestSession);
//        assertTrue(foundTestSessionService);
//        assertTrue(foundTestLibraryService);
    }

    @Test
    public void testAddOnSessionTokensChangedListener() throws InterruptedException {
//        prepareLooper();
//        TokensChangedListener listener = new TokensChangedListener();
//        mManager.addOnSessionTokensChangedListener(sHandlerExecutor, listener);
//
//        listener.reset();
//        MediaSession session1 = new MediaSession.Builder(mContext)
//                .setPlayer(new MockPlayer(0))
//                .setId(UUID.randomUUID().toString())
//                .build();
//        assertTrue(listener.await());
//        assertTrue(listener.findToken(session1.getToken()));
//
//        listener.reset();
//        session1.close();
//        assertTrue(listener.await());
//        assertFalse(listener.findToken(session1.getToken()));
//
//        listener.reset();
//        MediaSession session = new MediaSession.Builder(mContext)
//                .setPlayer(new MockPlayer(0))
//                .setId(UUID.randomUUID().toString())
//                .build();
//        assertTrue(listener.await());
//        assertFalse(listener.findToken(session1.getToken()));
//        assertTrue(listener.findToken(session.getToken()));
//
//        listener.reset();
//        MediaSession session3 = new MediaSession.Builder(mContext)
//                .setPlayer(new MockPlayer(0))
//                .setId(UUID.randomUUID().toString())
//                .build();
//        assertTrue(listener.await());
//        assertFalse(listener.findToken(session1.getToken()));
//        assertTrue(listener.findToken(session.getToken()));
//        assertTrue(listener.findToken(session3.getToken()));
//
//        listener.reset();
//        session.close();
//        assertTrue(listener.await());
//        assertFalse(listener.findToken(session1.getToken()));
//        assertFalse(listener.findToken(session.getToken()));
//        assertTrue(listener.findToken(session3.getToken()));
//
//        listener.reset();
//        session3.close();
//        assertTrue(listener.await());
//        assertFalse(listener.findToken(session1.getToken()));
//        assertFalse(listener.findToken(session.getToken()));
//        assertFalse(listener.findToken(session3.getToken()));
//
//        mManager.removeOnSessionTokensChangedListener(listener);
    }

    @Test
    public void testRemoveOnSessionTokensChangedListener() throws InterruptedException {
//        prepareLooper();
//        TokensChangedListener listener = new TokensChangedListener();
//        mManager.addOnSessionTokensChangedListener(sHandlerExecutor, listener);
//
//        listener.reset();
//        MediaSession session1 = new MediaSession.Builder(mContext)
//                .setPlayer(new MockPlayer(0))
//                .setId(UUID.randomUUID().toString())
//                .build();
//        assertTrue(listener.await());
//
//        mManager.removeOnSessionTokensChangedListener(listener);
//
//        listener.reset();
//        session1.close();
//        assertFalse(listener.await());
//
//        listener.reset();
//        MediaSession session = new MediaSession.Builder(mContext)
//                .setPlayer(new MockPlayer(0))
//                .setId(UUID.randomUUID().toString())
//                .build();
//        assertFalse(listener.await());
//
//        listener.reset();
//        MediaSession session3 = new MediaSession.Builder(mContext)
//                .setPlayer(new MockPlayer(0))
//                .setId(UUID.randomUUID().toString())
//                .build();
//        assertFalse(listener.await());
//
//        listener.reset();
//        session.close();
//        assertFalse(listener.await());
//
//        listener.reset();
//        session3.close();
//        assertFalse(listener.await());
    }

//    private class TokensChangedListener implements OnSessionTokensChangedListener {
//        private CountDownLatch mLatch;
//        private List<SessionToken> mTokens;
//
//        private void reset() {
//            mLatch = new CountDownLatch(1);
//            mTokens = null;
//        }
//
//        private boolean await() throws InterruptedException {
//            return mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
//        }
//
//        private boolean findToken(SessionToken token) {
//            return mTokens.contains(token);
//        }
//
//        @Override
//        public void onSessionTokensChanged(List<SessionToken> tokens) {
//            mTokens = tokens;
//            mLatch.countDown();
//        }
//    }
}
