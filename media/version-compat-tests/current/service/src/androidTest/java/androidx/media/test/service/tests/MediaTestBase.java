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

import android.content.Context;
import android.media.AudioManager;
import android.os.Looper;

import androidx.test.InstrumentationRegistry;

import org.junit.BeforeClass;

/**
 * Base class for all media tests.
 */
abstract class MediaTestBase {
    /**
     * All tests methods should start with this.
     * <p>
     * MediaControllerCompat, which is wrapped by the MediaSession, can be only created by the
     * thread whose Looper is prepared. However, when the presubmit test runs on the server,
     * test runs with the {@link org.junit.internal.runners.statements.FailOnTimeout} which creates
     * dedicated thread for running test methods while methods annotated with @After or @Before
     * runs on the normal test different thread. This ensures that the current Looper is prepared.
     */
    public static void prepareLooper() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @BeforeClass
    public static void setupMainLooper() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Prepare the main looper if it hasn't.
                // Some framework APIs always run on the main looper.
                if (Looper.getMainLooper() == null) {
                    Looper.prepareMainLooper();
                }

                // Initialize AudioManager on the main thread to workaround b/78617702 that
                // audio focus listener is called on the thread where the AudioManager was
                // originally initialized.
                // Without posting this, audio focus listeners wouldn't be called because the
                // listeners would be posted to the test thread (here) where it waits until the
                // tests are finished.
                Context context = InstrumentationRegistry.getTargetContext();
                AudioManager manager =
                        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            }
        });
    }
}
