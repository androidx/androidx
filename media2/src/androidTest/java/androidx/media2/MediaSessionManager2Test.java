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

import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Tests {@link MediaSessionManager2Test}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSessionManager2Test extends MediaTestBase {
    private Context mContext;

    private static final String TEST_PACKAGE_NAME = "androidx.media2.test";
    private static final String MOCK_BROWSER_SERVICE_COMPAT_NAME =
            androidx.media2.MockMediaBrowserServiceCompat.class.getName();
    private static final String MOCK_SESSION_SERVICE2_NAME =
            androidx.media2.MockMediaSessionService2.class.getName();
    private static final String MOCK_LIBRARY_SERVICE2_NAME =
            androidx.media2.MockMediaLibraryService2.class.getName();


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
        MediaSessionManager2 sessionManager2 = MediaSessionManager2.getInstance(mContext);
        List<SessionToken2> serviceTokens = sessionManager2.getSessionServiceTokens();
        for (SessionToken2 token2 : serviceTokens) {
            ComponentName componentName = token2.getComponentName();
            if (!TEST_PACKAGE_NAME.equals(componentName.getPackageName())) {
                continue;
            }
            String className = componentName.getClassName();
            if (MOCK_BROWSER_SERVICE_COMPAT_NAME.equals(className)) {
                hasMockBrowserServiceCompat = true;
            } else if (MOCK_SESSION_SERVICE2_NAME.equals(className)) {
                hasMockSessionService2 = true;
            } else if (MOCK_LIBRARY_SERVICE2_NAME.equals(className)) {
                hasMockLibraryService2 = true;
            }
        }
        assertTrue(hasMockBrowserServiceCompat);
        assertTrue(hasMockSessionService2);
        assertTrue(hasMockLibraryService2);
    }
}
