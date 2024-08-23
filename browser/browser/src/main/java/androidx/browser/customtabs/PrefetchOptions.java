/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.customtabs;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Composes optional parameters used in {@link CustomTabsSession#prefetch(Uri,
 * PrefetchOptions)}.
 */
@ExperimentalPrefetch
public final class PrefetchOptions {
    /**
     * Set true to anonymize IP address of the prefetch request when it is cross-origin.
     */
    public final boolean requiresAnonymousIpWhenCrossOrigin;

    /**
     * The origin that prefetch is requested from.
     */
    @Nullable public final Uri sourceOrigin;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    PrefetchOptions(
            boolean requiresAnonymousIpWhenCrossOrigin, @Nullable Uri sourceOrigin) {
        this.requiresAnonymousIpWhenCrossOrigin = requiresAnonymousIpWhenCrossOrigin;
        this.sourceOrigin = sourceOrigin;
    }

    /**
     * Bundle key for {@link #requiresAnonymousIpWhenCrossOrigin}.
     */
    private static final String KEY_REQUIRES_ANONYMOUS_IP_WHEN_CROSS_ORIGIN =
            "androidx.browser.customtabs.PrefetchOptions."
            + "KEY_REQUIRES_ANONYMOUS_IP_WHEN_CROSS_ORIGIN";

    /**
     * Bundle key for {@link #sourceOrigin}.
     */
    private static final String KEY_SOURCE_ORIGIN =
            "androidx.browser.customtabs.PrefetchOptions.KEY_SOURCE_ORIGIN";

    @NonNull
    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(
                KEY_REQUIRES_ANONYMOUS_IP_WHEN_CROSS_ORIGIN, requiresAnonymousIpWhenCrossOrigin);
        if (sourceOrigin != null) {
            bundle.putParcelable(KEY_SOURCE_ORIGIN, sourceOrigin);
        }
        return bundle;
    }

    @SuppressWarnings({"WeakerAccess", "deprecation"}) /* synthetic access */
    @NonNull
    static PrefetchOptions fromBundle(@NonNull Bundle bundle) {
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uri = Api33Impl.getParcelable(bundle, KEY_SOURCE_ORIGIN,
                    Uri.class);
        } else {
            uri = bundle.getParcelable(KEY_SOURCE_ORIGIN);
        }

        return new PrefetchOptions(
                bundle.getBoolean(KEY_REQUIRES_ANONYMOUS_IP_WHEN_CROSS_ORIGIN),
                uri);
    }

    /**
     * Builder class for {@link PrefetchOptions}
     */
    @ExperimentalPrefetch
    public static final class Builder {
        private boolean mRequiresAnonymousIpWhenCrossOrigin;
        @Nullable private Uri mSourceOrigin;

        /**
         * Creates an empty {@link PrefetchOptions.Builder} object.
         */
        public Builder() {}

        /**
         * Set true to anonymize IP address of the prefetch request when it is cross-origin.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setRequiresAnonymousIpWhenCrossOrigin(
                boolean requiresAnonymousIpWhenCrossOrigin) {
            mRequiresAnonymousIpWhenCrossOrigin = requiresAnonymousIpWhenCrossOrigin;
            return this;
        }

        /**
         * The origin that prefetch is requested from.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder setSourceOrigin(@NonNull Uri sourceOrigin) {
            mSourceOrigin = sourceOrigin;
            return this;
        }

        /**
         * Combines all the options that have been into a {@link PrefetchOptions} object.
         */
        @NonNull
        public PrefetchOptions build() {
            return new PrefetchOptions(
                    mRequiresAnonymousIpWhenCrossOrigin, mSourceOrigin);
        }
    }
}
