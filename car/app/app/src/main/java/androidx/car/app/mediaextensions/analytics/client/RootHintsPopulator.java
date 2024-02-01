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

import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_SHARE_OEM_DIAGNOSTICS;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_ROOT_KEY_BROADCAST_COMPONENT_NAME;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_ROOT_KEY_PASSKEY;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_ROOT_KEY_SESSION_ID;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.media.MediaBrowserServiceCompat;

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
 * @see AnalyticsBroadcastReceiver
 * @see MediaSessionCompat#setExtras(Bundle)
 */
@ExperimentalCarApi
public final class RootHintsPopulator {
    private final Bundle mRootHintsBundle;

    public RootHintsPopulator(@NonNull Bundle rootHints) {
        mRootHintsBundle = rootHints;
    }

    /**
     * Sets analytics opt in and {@link BroadcastReceiver} {@link ComponentName}.
     *
     * @param analyticsOptIn boolean value indicating opt-in to receive analytics.
     * @param receiverComponentName ComponentName of {@link BroadcastReceiver } that extends
     * {@link AnalyticsBroadcastReceiver}. This is the receiver that will receive analytics
     *                              event.
     */
    @NonNull
    public RootHintsPopulator setAnalyticsOptIn(boolean analyticsOptIn,
            @NonNull ComponentName receiverComponentName) {

        if (analyticsOptIn) {
            mRootHintsBundle.putString(ANALYTICS_ROOT_KEY_BROADCAST_COMPONENT_NAME,
                    receiverComponentName.flattenToString());
        } else {
            mRootHintsBundle.putString(ANALYTICS_ROOT_KEY_BROADCAST_COMPONENT_NAME, "");
        }

        mRootHintsBundle.putString(ANALYTICS_ROOT_KEY_PASSKEY,
                AnalyticsBroadcastReceiver.sAuthKey.toString());
        return this;
    }

    /**
     * Sets flag to share diagnostic analytics with OEM
     * @param shareOem boolean value indicating opt-in to share diagnostic analytics with OEM.
     */
    @NonNull
    public RootHintsPopulator setOemShare(boolean shareOem) {
        mRootHintsBundle.putBoolean(ANALYTICS_SHARE_OEM_DIAGNOSTICS, shareOem);
        return this;
    }

    /**
     * Sets flag to share diagnostic analytics with the platform
     * @param sharePlatform boolean value indicating opt-in to share diagnostic analytics with
     *                      the platform.
     */
    @NonNull
    public RootHintsPopulator setPlatformShare(boolean sharePlatform) {
        mRootHintsBundle.putBoolean(ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS, sharePlatform);
        return this;
    }

    /**
     * Sets sessionId. Session Id will set in each {@link AnalyticsEvent#getSessionId()}.
     * <p>
     *     Use this identify which session is generating {@link AnalyticsEvent events}.
     * </p>
     * @param sessionId Session Id used to identify which session generated event.
     */
    @NonNull
    public RootHintsPopulator setSessionId(int sessionId) {
        mRootHintsBundle.putInt(ANALYTICS_ROOT_KEY_SESSION_ID, sessionId);
        return this;
    }
}
