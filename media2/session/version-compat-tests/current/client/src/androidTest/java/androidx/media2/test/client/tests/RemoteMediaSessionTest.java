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
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Bundle;

import androidx.media2.session.MediaController;
import androidx.media2.session.SessionToken;
import androidx.media2.test.client.RemoteMediaSession;
import androidx.media2.test.common.TestUtils;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

/** Test {@link RemoteMediaSession}. */
@RunWith(AndroidJUnit4.class)
public class RemoteMediaSessionTest {

    private Context mContext;
    private RemoteMediaSession mRemoteSession2;
    private Bundle mTokenExtras;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mTokenExtras = TestUtils.createTestBundle();
        mRemoteSession2 = new RemoteMediaSession(DEFAULT_TEST_NAME, mContext, mTokenExtras);
    }

    @After
    public void cleanUp() {
        if (mRemoteSession2 != null) {
            mRemoteSession2.cleanUp();
        }
    }

    @Test
    @SmallTest
    public void testGettingToken() {
        SessionToken token = mRemoteSession2.getToken();
        assertNotNull(token);
        assertEquals(SERVICE_PACKAGE_NAME, token.getPackageName());
        assertTrue(TestUtils.equals(mTokenExtras, token.getExtras()));
    }

    @Test
    @SmallTest
    public void testCreatingController() {
        SessionToken token = mRemoteSession2.getToken();
        assertNotNull(token);
        MediaController controller = new MediaController.Builder(mContext)
                .setSessionToken(token)
                .setControllerCallback(new Executor() {
                    @Override
                    public void execute(Runnable command) {
                        command.run();
                    }
                }, new MediaController.ControllerCallback() {})
                .build();
    }
}
