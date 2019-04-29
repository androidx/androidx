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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.session.MediaBrowser;
import androidx.media2.session.MediaBrowser.BrowserCallback;
import androidx.media2.session.MediaController;
import androidx.media2.session.MediaController.ControllerCallback;
import androidx.media2.session.SessionToken;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests {@link MediaBrowser}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaBrowserTest extends MediaControllerTest {

    @Override
    MediaController onCreateController(final @NonNull SessionToken token,
            final @Nullable Bundle connectionHints,
            final @Nullable TestBrowserCallback callback) throws InterruptedException {
        final AtomicReference<MediaController> controller = new AtomicReference<>();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                // Create controller on the test handler, for changing MediaBrowserCompat's Handler
                // Looper. Otherwise, MediaBrowserCompat will post all the commands to the handler
                // and commands wouldn't be run if tests codes waits on the test handler.
                MediaController.Builder builder = new MediaController.Builder(mContext)
                        .setSessionToken(token)
                        .setControllerCallback(sHandlerExecutor, callback);
                if (connectionHints != null) {
                    builder.setConnectionHints(connectionHints);
                }
                controller.set(builder.build());
            }
        });
        return controller.get();
    }

    /**
     * Test if the {@link TestBrowserCallback} wraps the callback proxy without missing any method.
     */
    @Test
    public void testTestBrowserCallback() {
        prepareLooper();
        Method[] methods = TestBrowserCallback.class.getMethods();
        assertNotNull(methods);
        for (int i = 0; i < methods.length; i++) {
            // For any methods in the controller callback, TestBrowserCallback should have
            // overridden the method and call matching API in the callback proxy.
            assertNotEquals("TestBrowserCallback should override " + methods[i]
                            + " and call callback proxy",
                    BrowserCallback.class, methods[i].getDeclaringClass());
            assertNotEquals("TestBrowserCallback should override " + methods[i]
                            + " and call callback proxy",
                    ControllerCallback.class, methods[i].getDeclaringClass());
        }
    }

}
