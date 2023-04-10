/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appcompat.app.g3;

import static androidx.core.util.Preconditions.checkNotNull;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Adapted from g3's AndroidTestUtil.java with modifications to pass AndroidX lint.
 */
public class AndroidTestUtil {
    private static final String TAG = AndroidTestUtil.class.getSimpleName();
    private static final Lock LOCK = new ReentrantLock();
    private static final Condition CONDITION = LOCK.newCondition();

    /** Represents possible screen orientations. */
    public enum ScreenOrientation {
        PORTRAIT(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
        LANDSCAPE(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        private final int mScreenOrientation;

        ScreenOrientation(int screenOrientation) {
            this.mScreenOrientation = screenOrientation;
        }

        @SuppressWarnings("unused")
        private int value() {
            return this.mScreenOrientation;
        }
    }

    /**
     * Sets the screen orientation.
     *
     * <p>This will block the UI-thread until the operation completes, or throw an
     * InterruptedException after 5 seconds.
     *
     * @param context the context of the application.
     * @param screenOrientation absolute value to set the screen orientation.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void setScreenOrientation(Context context, ScreenOrientation screenOrientation) {
        checkNotNull(context);

        // This should have been an instanceof Application check when the method was first added,
        // but it's too hard to back out now. Try really hard to make sure we're using the right
        // context.
        Context appContext = context.getApplicationContext();

        // Of course, they might be passing the instrumentation's getContext(), which doesn't have
        // access to the application context. They should have used getTargetContext()!
        if (appContext != null) {
            context = appContext;
        }

        try {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            switch (screenOrientation) {
                case PORTRAIT:
                    device.setOrientationNatural();
                    break;
                case LANDSCAPE:
                    device.setOrientationRight();
                    break;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        LOCK.lock(); // block until condition holds
        try {
            while (screenOrientation != getScreenOrientation(context)) {
                CONDITION.await(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Log.w(TAG, "setScreenOrientation: Thread wait failure.");
            throw new RuntimeException("Unable to change screen orientation.", ex);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Returns current orientation of the screen.
     *
     * @param context the context of the application.
     * @return returns current screen orientation.
     */
    public static ScreenOrientation getScreenOrientation(final Context context) {
        checkNotNull(context);

        int currentOrientation = context.getResources().getConfiguration().orientation;
        if (Configuration.ORIENTATION_LANDSCAPE == currentOrientation) {
            return ScreenOrientation.LANDSCAPE;
        } else {
            return ScreenOrientation.PORTRAIT;
        }
    }
}
