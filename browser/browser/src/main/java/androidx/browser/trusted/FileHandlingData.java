/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.browser.trusted;

import android.net.Uri;
import android.os.Bundle;

import androidx.core.os.BundleCompat;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains data to be delivered to the launch queue via a Trusted Web Activity.
 * See {@link androidx.browser.trusted.TrustedWebActivityIntentBuilder#setFileHandlingData}.
 */
public final class FileHandlingData {
    /** Bundle key for {@link #uris}. */
    public static final String KEY_URIS = "androidx.browser.trusted.KEY_URIS";

    /** URIs of files to be handled. */
    public final @NonNull List<Uri> uris;

    /**
     * Creates a {@link FileHandlingData} with the given parameters.
     * @param uris The {@link #uris}.
     */
    public FileHandlingData(@NonNull List<Uri> uris) {
        this.uris = Objects.requireNonNull(uris);
    }

    /** Packs the object into a {@link Bundle} */
    public @NonNull Bundle toBundle() {
        Bundle bundle = new Bundle();
        if (uris != null) {
            bundle.putParcelableArrayList(KEY_URIS, new ArrayList<>(uris));
        }
        return bundle;
    }

    /** Unpacks the object from a {@link Bundle}. */
    @SuppressWarnings("NullAway")
    public static @NonNull FileHandlingData fromBundle(@NonNull Bundle bundle) {
        return new FileHandlingData(
                BundleCompat.getParcelableArrayList(bundle, KEY_URIS, Uri.class));
    }
}
