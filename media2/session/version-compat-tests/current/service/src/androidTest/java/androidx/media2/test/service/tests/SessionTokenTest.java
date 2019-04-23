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

import static junit.framework.Assert.assertEquals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import androidx.media2.session.MediaSession;
import androidx.media2.session.SessionToken;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.service.MockMediaLibraryService;
import androidx.media2.test.service.MockMediaSessionService;
import androidx.media2.test.service.MockPlayer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link SessionToken}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SessionTokenTest extends MediaTestBase {
    private Context mContext;
    private List<MediaSession> mSessions = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void cleanUp() throws Exception {
        for (MediaSession session : mSessions) {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test
    public void testConstructor_sessionService() {
        SessionToken token = new SessionToken(mContext, new ComponentName(
                mContext.getPackageName(),
                MockMediaSessionService.class.getCanonicalName()));
        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(Process.myUid(), token.getUid());
        assertEquals(SessionToken.TYPE_SESSION_SERVICE, token.getType());
    }

    @Test
    public void testConstructor_libraryService() {
        ComponentName testComponentName = new ComponentName(mContext.getPackageName(),
                MockMediaLibraryService.class.getCanonicalName());
        SessionToken token = new SessionToken(mContext, testComponentName);

        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(Process.myUid(), token.getUid());
        assertEquals(SessionToken.TYPE_LIBRARY_SERVICE, token.getType());
        assertEquals(testComponentName.getClassName(), token.getServiceName());
    }

    @Test
    public void testGetters_whenCreatedBySession() {
        prepareLooper();
        Bundle testTokenExtras = TestUtils.createTestBundle();
        MediaSession session = new MediaSession.Builder(mContext, new MockPlayer(0))
                .setId("testGetters_whenCreatedBySession")
                .setExtras(testTokenExtras)
                .build();
        mSessions.add(session);
        SessionToken token = session.getToken();

        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(Process.myUid(), token.getUid());
        assertEquals(SessionToken.TYPE_SESSION, token.getType());
        assertTrue(TestUtils.equals(testTokenExtras, token.getExtras()));
        assertNull(token.getServiceName());
    }
}
