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

package androidx.media.test.client;

import androidx.media.MediaController2;

/**
 * Tests {@link MediaController2}.
 */
//// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
//@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
//@RunWith(AndroidJUnit4.class)
//@SmallTest
//@FlakyTest
//@Ignore
public class MediaController2Test_copied extends MediaSession2TestBase {
    private static final String TAG = "MediaController2Test_copied";

//    // TODO: This test will be moved into MediaSession2Test.
//    /**
//     * Test potential deadlock for calls between controller and session.
//     */
//    @Test
//    public void testDeadlock() throws InterruptedException {
//        prepareLooper();
//        sHandler.postAndSync(new Runnable() {
//            @Override
//            public void run() {
//                mSession.close();
//                mSession = null;
//            }
//        });
//
//        // Two more threads are needed not to block test thread nor test wide thread (sHandler).
//        final HandlerThread sessionThread = new HandlerThread("testDeadlock_session");
//        final HandlerThread testThread = new HandlerThread("testDeadlock_test");
//        sessionThread.start();
//        testThread.start();
//        final SyncHandler sessionHandler = new SyncHandler(sessionThread.getLooper());
//        final Handler testHandler = new Handler(testThread.getLooper());
//        final CountDownLatch latch = new CountDownLatch(1);
//        try {
//            final MockPlayer player = new MockPlayer(0);
//            sessionHandler.postAndSync(new Runnable() {
//                @Override
//                public void run() {
//                    mSession = new MediaSession2.Builder(mContext)
//                            .setPlayer(mPlayer)
//                            .setSessionCallback(sHandlerExecutor, new SessionCallback() {})
//                            .setId("testDeadlock").build();
//                }
//            });
//            final MediaController2 controller = createController(mSession.getToken());
//            testHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    final int state = BaseMediaPlayer.PLAYER_STATE_ERROR;
//                    for (int i = 0; i < 100; i++) {
//                        // triggers call from session to controller.
//                        player.notifyPlayerStateChanged(state);
//                        // triggers call from controller to session.
//                        controller.play();
//
//                        // Repeat above
//                        player.notifyPlayerStateChanged(state);
//                        controller.pause();
//                        player.notifyPlayerStateChanged(state);
//                        controller.reset();
//                        player.notifyPlayerStateChanged(state);
//                        controller.skipToNextItem();
//                        player.notifyPlayerStateChanged(state);
//                        controller.skipToPreviousItem();
//                    }
//                    // This may hang if deadlock happens.
//                    latch.countDown();
//                }
//            });
//            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
//        } finally {
//            if (mSession != null) {
//                sessionHandler.postAndSync(new Runnable() {
//                    @Override
//                    public void run() {
//                        // Clean up here because sessionHandler will be removed afterwards.
//                        mSession.close();
//                        mSession = null;
//                    }
//                });
//            }
//
//            if (Build.VERSION.SDK_INT >= 18) {
//                sessionThread.quitSafely();
//                testThread.quitSafely();
//            } else {
//                sessionThread.quit();
//                testThread.quit();
//            }
//        }
//    }

// Temporaily commenting out, since we don't have the Mock services yet.
//    @Test
//    public void testGetServiceToken() {
//        prepareLooper();
//        SessionToken2 token = MediaTestUtils.getServiceToken(
//                  mContext, MockMediaSessionService2.ID);
//        assertNotNull(token);
//        assertEquals(mContext.getPackageName(), token.getPackageName());
//        assertEquals(MockMediaSessionService2.ID, token.getId());
//        assertEquals(SessionToken2.TYPE_SESSION_SERVICE, token.getType());
//    }
//
//    @Test
//    public void testConnectToService_sessionService() throws InterruptedException {
//        prepareLooper();
//        testConnectToService(MockMediaSessionService2.ID);
//    }
//
//    @Test
//    public void testConnectToService_libraryService() throws InterruptedException {
//        prepareLooper();
//        testConnectToService(MockMediaLibraryService2.ID);
//    }
//
//    public void testConnectToService(String id) throws InterruptedException {
//        prepareLooper();
//        final CountDownLatch latch = new CountDownLatch(1);
//        final MediaLibrarySessionCallback sessionCallback = new MediaLibrarySessionCallback() {
//            @Override
//            public SessionCommandGroup2 onConnect(@NonNull MediaSession2 session,
//                    @NonNull ControllerInfo controller) {
//                if (Process.myUid() == controller.getUid()) {
//                    if (mSession != null) {
//                        mSession.close();
//                    }
//                    mSession = session;
//                    mPlayer = (MockPlayer) session.getPlayer();
//                    assertEquals(mContext.getPackageName(), controller.getPackageName());
//                    assertFalse(controller.isTrusted());
//                    latch.countDown();
//                }
//                return super.onConnect(session, controller);
//            }
//        };
//        TestServiceRegistry.getInstance().setSessionCallback(sessionCallback);
//
//        final SessionCommand2 testCommand = new SessionCommand2("testConnectToService", null);
//        final CountDownLatch controllerLatch = new CountDownLatch(1);
//        mController = createController(MediaTestUtils.getServiceToken(mContext, id), true,
//                new ControllerCallback() {
//                    @Override
//                    public void onCustomCommand(MediaController2 controller,
//                            SessionCommand2 command, Bundle args, ResultReceiver receiver) {
//                        if (testCommand.equals(command)) {
//                            controllerLatch.countDown();
//                        }
//                    }
//                }
//        );
//        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//
//        // Test command from controller to session service.
//        mController.play();
//        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
//        assertTrue(mPlayer.mPlayCalled);
//
//        // Test command from session service to controller.
//        mSession.sendCustomCommand(testCommand, null);
//        assertTrue(controllerLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
//    }


// Temporaily commenting out, since we don't have the Mock services yet.
//    @Test
//    public void testControllerAfterSessionIsClosed_sessionService() throws InterruptedException {
//        prepareLooper();
//        testConnectToService(MockMediaSessionService2.ID);
//        testControllerAfterSessionIsClosed(MockMediaSessionService2.ID);
//    }

// Temporaily commenting out, since we don't have the Mock services yet.
//    @Test
//    public void testClose_sessionService() throws InterruptedException {
//        prepareLooper();
//        testCloseFromService(MockMediaSessionService2.ID);
//    }
//
//    @Test
//    public void testClose_libraryService() throws InterruptedException {
//        prepareLooper();
//        testCloseFromService(MockMediaLibraryService2.ID);
//    }
//
//    private void testCloseFromService(String id) throws InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(1);
//        TestServiceRegistry.getInstance().setSessionServiceCallback(new SessionServiceCallback() {
//            @Override
//            public void onCreated() {
//                // Do nothing.
//            }
//
//            @Override
//            public void onDestroyed() {
//                latch.countDown();
//            }
//        });
//        mController = createController(MediaTestUtils.getServiceToken(mContext, id));
//        mController.close();
//        // Wait until close triggers onDestroy() of the session service.
//        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
//        assertNull(TestServiceRegistry.getInstance().getServiceInstance());
//        testNoInteraction();
//
//        // Test whether the controller is notified about later close of the session or
//        // re-creation.
//        testControllerAfterSessionIsClosed(id);
//    }
}