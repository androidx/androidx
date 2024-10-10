/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.car.app.mediaextensions.analytics.client;

import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_ROOT_KEY_OPT_IN;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_SHARE_OEM_DIAGNOSTICS;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.media.MediaBrowserServiceCompat;

import org.jspecify.annotations.NonNull;

/**
 * Populates Root hints {@link Bundle} for {@link MediaBrowserServiceCompat.BrowserRoot}
 * returned in {@link MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)}.
 *
 * <p>
 * RootExtras can be updated after
 * {@link MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)} with a call to
 * {@link MediaSessionCompat#setExtras(Bundle)}.
 *
 * @see MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)
 * @see MediaSessionCompat#setExtras(Bundle)
 */
@ExperimentalCarApi
public final class RootHintsPopulator {
    private final Bundle mRootHintsBundle;

    public RootHintsPopulator(@NonNull Bundle rootHints) {
        mRootHintsBundle = rootHints;
    }

    /**
     * Sets analytics opt in state.
     *
     * @param analyticsOptIn boolean value indicating opt-in to receive analytics.
     */
    public @NonNull RootHintsPopulator setAnalyticsOptIn(boolean analyticsOptIn) {
        mRootHintsBundle.putBoolean(ANALYTICS_ROOT_KEY_OPT_IN, analyticsOptIn);
        return this;
    }

    /**
     * Sets flag to share diagnostic analytics with OEM
     * @param shareOem boolean value indicating opt-in to share diagnostic analytics with OEM.
     */
    public @NonNull RootHintsPopulator setShareOem(boolean shareOem) {
        mRootHintsBundle.putBoolean(ANALYTICS_SHARE_OEM_DIAGNOSTICS, shareOem);
        return this;
    }

    /**
     * Sets flag to share diagnostic analytics with the platform
     * @param sharePlatform boolean value indicating opt-in to share diagnostic analytics with
     *                      the platform.
     */
    public @NonNull RootHintsPopulator setSharePlatform(boolean sharePlatform) {
        mRootHintsBundle.putBoolean(ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS, sharePlatform);
        return this;
    }
}
