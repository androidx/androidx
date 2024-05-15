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

package androidx.core.app;

import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper for accessing features in {@link Bundle}.
 *
 * @deprecated Replaced with {@link androidx.core.os.BundleCompat}.
 */
@Deprecated
public final class BundleCompat {

    private BundleCompat() {}

    /**
     * A convenience method to handle getting an {@link IBinder} inside a {@link Bundle} for all
     * Android versions.
     * @param bundle The bundle to get the {@link IBinder}.
     * @param key    The key to use while getting the {@link IBinder}.
     * @return       The {@link IBinder} that was obtained.
     * @deprecated Call {@link Bundle#getBinder()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "bundle.getBinder(key)")
    @Nullable
    public static IBinder getBinder(@NonNull Bundle bundle, @Nullable String key) {
        return bundle.getBinder(key);
    }

    /**
     * A convenience method to handle putting an {@link IBinder} inside a {@link Bundle} for all
     * Android versions.
     * @param bundle The bundle to insert the {@link IBinder}.
     * @param key    The key to use while putting the {@link IBinder}.
     * @param binder The {@link IBinder} to put.
     * @deprecated Call {@link Bundle#putBinder()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "bundle.putBinder(key, binder)")
    public static void putBinder(@NonNull Bundle bundle, @Nullable String key,
            @Nullable IBinder binder) {
        bundle.putBinder(key, binder);
    }
}
