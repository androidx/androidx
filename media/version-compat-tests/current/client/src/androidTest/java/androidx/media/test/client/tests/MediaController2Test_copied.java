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

package androidx.media.test.client.tests;

import androidx.media2.MediaController2;

/**
 * Tests {@link MediaController2}.
 */
public class MediaController2Test_copied extends MediaSession2TestBase {
    private static final String TAG = "MediaController2Test_copied";

// Temporaily commenting out, since we don't have the Mock services yet.
//    @Test
//    public void testGetServiceToken() {
//        prepareLooper();
//        SessionToken2 token = MediaTestUtils.getServiceToken(
//                  mContext, MockMediaSessionService2.ID);
//        assertNotNull(token);
//        assertEquals(mContext.getPackageName(), token.getPackageName());
//        assertEquals(MockMediaSessionService2.ID, token.getSessionId());
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
