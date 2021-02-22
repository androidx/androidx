/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.customtabs.testutil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.os.Handler;
import android.os.Looper;

import androidx.browser.trusted.TestBrowser;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;


/**
 * Utilities for testing Custom Tabs.
 */
public class TestUtil {

    /**
     * Waits until {@link TestBrowser} is launched and resumed, and returns it.
     *
     * @param launchRunnable Runnable that should start the activity.
     */
    public static TestBrowser getBrowserActivityWhenLaunched(Runnable launchRunnable) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor =
                instrumentation.addMonitor(TestBrowser.class.getName(), null, false);

        launchRunnable.run();
        TestBrowser activity =
                (TestBrowser) instrumentation.waitForMonitorWithTimeout(monitor, 3000);
        assertNotNull("TestBrowser wasn't launched", activity);

        // ActivityMonitor is triggered in onCreate and in onResume, which can lead to races when
        // launching several activity instances. So wait for onResume before returning.
        boolean resumed = activity.waitForResume(3000);
        assertTrue("TestBrowser didn't reach onResume", resumed);
        return activity;
    }

    /**
     * Runs the supplied Callable on the main thread, returning the result. Blocks until completes.
     */
    public static <T> T runOnUiThreadBlocking(Callable<T> c) {
        FutureTask<T> task = new FutureTask<>(c);
        new Handler(Looper.getMainLooper()).post(task);
        try {
            return task.get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for callable", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }


    /**
     * Runs the supplied Runnable on the main thread. Blocks until completes.
     */
    public static void runOnUiThreadBlocking(final Runnable c) {
        runOnUiThreadBlocking(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                c.run();
                return null;
            }
        });
    }

}
