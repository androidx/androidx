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

package androidx.media2.test.client.tests;

import static androidx.media2.test.common.CommonConstants.DEFAULT_TEST_NAME;
import static androidx.media2.test.common.CommonConstants.SERVICE_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.media2.test.client.RemoteMediaSessionCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Test {@link RemoteMediaSessionCompat}. */
@RunWith(AndroidJUnit4.class)
public class RemoteMediaSessionCompatTest {

    private Context mContext;
    private RemoteMediaSessionCompat mRemoteSessionCompat;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mRemoteSessionCompat = new RemoteMediaSessionCompat(DEFAULT_TEST_NAME, mContext);
    }

    @After
    public void cleanUp() {
        mRemoteSessionCompat.cleanUp();
    }

    @Test
    @SmallTest
    public void testGettingToken() {
        MediaSessionCompat.Token token = mRemoteSessionCompat.getSessionToken();
        assertNotNull(token);
    }

    @Test
    @SmallTest
    public void testCreatingControllerCompat() throws Exception {
        MediaSessionCompat.Token token = mRemoteSessionCompat.getSessionToken();
        assertNotNull(token);
        MediaControllerCompat controller = new MediaControllerCompat(mContext, token);
        assertEquals(SERVICE_PACKAGE_NAME, controller.getPackageName());
    }
}
