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

package androidx.core.content;

import android.content.ContentProvider;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Helper for accessing features in {@link android.content.ContentProvider} in a backwards
 * compatible fashion.
 */
public final class ContentProviderCompat {
    private ContentProviderCompat() {
        /* Hide constructor */
    }

    /**
     * Returns NonNull context associated with given {@link android.content.ContentProvider}.
     *
     * A provider must be declared in the manifest and created automatically by the system, and
     * context is only available after {@link android.content.ContentProvider#onCreate}
     * is called.
     *
     * @param provider The {@link android.content.ContentProvider}.
     * @return The {@link android.content.Context} object associated with the
     * {@link android.content.ContentProvider}.
     */
    public static @NonNull Context requireContext(@NonNull final ContentProvider provider) {
        final Context ctx = provider.getContext();
        if (ctx == null) {
            throw new IllegalStateException("Cannot find context from the provider.");
        }
        return ctx;
    }
}
