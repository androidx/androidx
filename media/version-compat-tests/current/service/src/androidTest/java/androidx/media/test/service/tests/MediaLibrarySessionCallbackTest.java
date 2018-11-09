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

import static androidx.media.test.service.MediaTestUtils.assertEqualLibraryParams;
import static androidx.media2.MediaLibraryService2.LibraryResult.RESULT_CODE_SUCCESS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.media.test.service.MediaTestUtils;
import androidx.media.test.service.MockMediaLibraryService2;
import androidx.media.test.service.MockPlayer;
import androidx.media.test.service.RemoteMediaBrowser2;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;
import androidx.media2.MediaSession2;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaLibrarySession.MediaLibrarySessionCallback}.
 *
 * TODO: Make this class extend MediaSession2CallbackTest.
 * TODO: Create MediaLibrarySessionTest which extends MediaSession2Test.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaLibrarySessionCallbackTest extends MediaSession2TestBase {

    MockPlayer mPlayer;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(0);
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
    }

    @Test
    public void testOnSubscribe() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testSubscribeId";
        final LibraryParams testParams = MediaTestUtils.createLibraryParams();

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySession.MediaLibrarySessionCallback sessionCallback =
                new MediaLibrarySession.MediaLibrarySessionCallback() {
                    @Override
                    public int onSubscribe(MediaLibrarySession session,
                            MediaSession2.ControllerInfo controller, String parentId,
                            LibraryParams params) {
                        assertEquals(testParentId, parentId);
                        assertEqualLibraryParams(testParams, params);
                        latch.countDown();
                        return RESULT_CODE_SUCCESS;
                    }
        };

        MockMediaLibraryService2 service = new MockMediaLibraryService2();
        service.attachBaseContext(mContext);

        try (MediaLibrarySession session = new MediaLibrarySession.Builder(
                service, mPlayer, sHandlerExecutor, sessionCallback)
                .setId("testOnSubscribe")
                .build()) {
            RemoteMediaBrowser2 browser = createRemoteBrowser2(session.getToken());
            browser.subscribe(testParentId, testParams);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testOnUnsubscribe() throws InterruptedException {
        prepareLooper();
        final String testParentId = "testUnsubscribeId";

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaLibrarySession.MediaLibrarySessionCallback sessionCallback =
                new MediaLibrarySession.MediaLibrarySessionCallback() {
                    @Override
                    public int onUnsubscribe(MediaLibrarySession session,
                            MediaSession2.ControllerInfo controller, String parentId) {
                        assertEquals(testParentId, parentId);
                        latch.countDown();
                        return RESULT_CODE_SUCCESS;
                    }
                };

        MockMediaLibraryService2 service = new MockMediaLibraryService2();
        service.attachBaseContext(mContext);

        try (MediaLibrarySession session = new MediaLibrarySession.Builder(
                service, mPlayer, sHandlerExecutor, sessionCallback)
                .setId("testOnUnsubscribe")
                .build()) {
            RemoteMediaBrowser2 browser = createRemoteBrowser2(session.getToken());
            browser.unsubscribe(testParentId);
            assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
    }
}
