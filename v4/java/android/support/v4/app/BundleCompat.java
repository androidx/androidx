/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.app;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

/**
 * Helper for accessing features in {@link Bundle}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public final class BundleCompat {

    private BundleCompat() {}

    /**
     * A convenience method to handle getting an {@link IBinder} inside a {@link Bundle} for all
     * Android versions.
     * @param bundle The bundle to get the {@link IBinder}.
     * @param key    The key to use while getting the {@link IBinder}.
     * @return       The {@link IBinder} that was obtained.
     */
    public static IBinder getBinder(Bundle bundle, String key) {
        if (Build.VERSION.SDK_INT >= 18) {
            return BundleCompatJellybeanMR2.getBinder(bundle, key);
        } else {
            return BundleCompatDonut.getBinder(bundle, key);
        }
    }

    /**
     * A convenience method to handle putting an {@link IBinder} inside a {@link Bundle} for all
     * Android versions.
     * @param bundle The bundle to insert the {@link IBinder}.
     * @param key    The key to use while putting the {@link IBinder}.
     * @param binder The {@link IBinder} to put.
     */
    public static void putBinder(Bundle bundle, String key, IBinder binder) {
        if (Build.VERSION.SDK_INT >= 18) {
            BundleCompatJellybeanMR2.putBinder(bundle, key, binder);
        } else {
            BundleCompatDonut.putBinder(bundle, key, binder);
        }
    }
}
