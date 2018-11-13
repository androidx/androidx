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

import static junit.framework.Assert.assertEquals;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Process;

import androidx.media.test.service.MockMediaLibraryService;
import androidx.media.test.service.MockMediaSessionService;
import androidx.media2.SessionToken;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests {@link SessionToken}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SessionTokenTest {
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
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
        SessionToken token = new SessionToken(mContext, new ComponentName(
                mContext.getPackageName(),
                MockMediaLibraryService.class.getCanonicalName()));
        assertEquals(mContext.getPackageName(), token.getPackageName());
        assertEquals(Process.myUid(), token.getUid());
        assertEquals(SessionToken.TYPE_LIBRARY_SERVICE, token.getType());
    }
}
