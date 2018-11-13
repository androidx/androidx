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

import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;

import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_LIBRARY_SERVICE;
import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_SESSION_SERVICE;

import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import androidx.media.test.service.MockMediaBrowserServiceCompat;
import androidx.media2.MediaSessionManager;
import androidx.media2.SessionToken;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/**
 * Tests {@link MediaSessionManagerTest}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSessionManagerTest extends MediaTestBase {
    private Context mContext;

    private static final ComponentName MOCK_BROWSER_SERVICE_COMPAT_NAME = new ComponentName(
            SERVICE_PACKAGE_NAME, MockMediaBrowserServiceCompat.class.getCanonicalName());

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testGetSessionServiceTokens() {
        prepareLooper();
        boolean hasMockBrowserServiceCompat = false;
        boolean hasMockSessionService2 = false;
        boolean hasMockLibraryService2 = false;
        MediaSessionManager sessionManager = MediaSessionManager.getInstance(mContext);
        Set<SessionToken> serviceTokens = sessionManager.getSessionServiceTokens();
        for (SessionToken token : serviceTokens) {
            ComponentName componentName = token.getComponentName();
            if (MOCK_BROWSER_SERVICE_COMPAT_NAME.equals(componentName)) {
                hasMockBrowserServiceCompat = true;
            } else if (MOCK_MEDIA_SESSION_SERVICE.equals(componentName)) {
                hasMockSessionService2 = true;
            } else if (MOCK_MEDIA_LIBRARY_SERVICE.equals(componentName)) {
                hasMockLibraryService2 = true;
            }
        }
        assertTrue(hasMockBrowserServiceCompat);
        assertTrue(hasMockSessionService2);
        assertTrue(hasMockLibraryService2);
    }
}
