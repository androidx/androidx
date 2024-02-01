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

package androidx.car.app.mediaextensions.analytics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.Bundle;
import android.service.media.MediaBrowserService;

import androidx.annotation.RestrictTo;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;

/** Constants for Analytics Events. */
public class Constants {

    private Constants() {}

    /**
     * Current version of the Analytics feature.
     *
     * <p>Used by AnalyticsParser
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int ANALYTICS_VERSION = 1;

    /**
     * Presence of this flag in {@link MediaBrowserService#onGetRoot(String, int, Bundle)} rootHints
     * {@linkplain Bundle} with a string value indicates an op-in for analytics feature.
     * <p>
     * Value of this flag sets {@link android.content.ComponentName} for the analytics broadcast
     * receiver.
     * <p>
     * Absence of flag indicates no opt-in.
     * <p>
     * Type: String - component name for analytics broadcast receiver
     *
     * @see Constants#ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS
     * @see Constants#ANALYTICS_SHARE_OEM_DIAGNOSTICS
     */
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_ROOT_KEY_BROADCAST_COMPONENT_NAME =
            "androidx.car.app.mediaextension.analytics.broadcastcomponentname";

    /**
     * Passkey used to verify analytics broadcast is sent from an approved host. Handled by
     * AnalyticsManager and
     * {@link androidx.car.app.mediaextensions.analytics.client.RootHintsUtil}
     *
     * <p>Type: String - String value of passkey. E.g. a new UUID
     */
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_ROOT_KEY_PASSKEY =
            "androidx.car.app.mediaextensions.analytics.broadcastpasskey";

    /**
     * Session key used to identify which session generated the analytics event.
     *
     * <p>
     *     Include this key in {@link MediaBrowserService#onGetRoot(String, int, Bundle)} rootHints.
     *     Analytics broadcasts will include this key in {@link AnalyticsEvent#getSessionId()}.
     *
     * <p>Type: Integer - Integer value of session.
     */
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_ROOT_KEY_SESSION_ID =
            "androidx.car.app.mediaextensions.analytics.sessionid";

    /**
     * Presence of this flag in {@link MediaBrowserService#onGetRoot(String, int, Bundle)}
     * rootHints with a value of true indicates opt-in to share diagnostic analytics to platform.
     *
     * <p>Absence of this flag will result in no analytics collected and sent to platform.
     *
     * @see Constants#ANALYTICS_ROOT_KEY_BROADCAST_COMPONENT_NAME
     * @see Constants#ANALYTICS_SHARE_OEM_DIAGNOSTICS
     *
     * <p>Type: Boolean - Boolean value of true opts-in to feature.
     */
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS =
            "androidx.car.app.mediaextensions.analytics.shareplatformdiagnostics";

    /**
     * Presence of this flag in {@link MediaBrowserService#onGetRoot(String, int, Bundle)}
     * rootHints with a value of true indicates opt-in to share diagnostic analytics to OEM.
     *
     * <p>Absence of this flag will result in no analytics collected and sent to OEM.
     *
     * <p>
     *
     * @see Constants#ANALYTICS_ROOT_KEY_BROADCAST_COMPONENT_NAME
     * @see Constants#ANALYTICS_SHARE_PLATFORM_DIAGNOSTICS
     *     <p>Type: Boolean - Boolean value of true opts-in to feature.
     */
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_SHARE_OEM_DIAGNOSTICS =
            "androidx.car.app.mediaextensions.analytics.shareoemdiagnostics";

    /**
     * Broadcast Receiver intent action for analytics broadcast receiver.
     *
     * <p>Use the value of this string for the intent filter of analytics broadcast receivers.
     *
     * <p>Type: String - String value that indicates analytics event action.
     */
    public static final String ACTION_ANALYTICS =
            "androidx.car.app.mediaextensions.analytics.action.ANALYTICS";

    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_BUNDLE_ARRAY_KEY =
            "androidx.car.app.mediaextensions.analytics.bundlearraykey";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_BUNDLE_KEY_PASSKEY =
            "androidx.car.app.mediaextensions.analytics.passkey";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_MEDIA_CLICKED =
            "androidx.car.app.mediaextensions.analytics.mediaClicked";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_VISIBLE_ITEMS =
            "androidx.car.app.mediaextensions.analytics.visibleitems";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_BROWSE_NODE_CHANGE =
            "androidx.car.app.mediaextensions.analytics.browsenodechange";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_VIEW_CHANGE =
            "androidx.car.app.mediaextensions.analytics.viewchange";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_VERSION =
            "androidx.car.app.mediaextensions.analytics.versionKey";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_EVENT_NAME =
            "androidx.car.app.mediaextensions.analytics.eventnamekey";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_TIMESTAMP =
            "androidx.car.app.mediaextensions.analytics.timestamp";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_HOST_COMPONENT_ID =
            "androidx.car.app.mediaextensions.analytics.componentid";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_SESSION_ID =
            "androidx.car.app.mediaextensions.analytics.sessionID";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_MEDIA_ID =
            "androidx.car.app.mediaextensions.analytics.mediaId";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_VIEW_COMPONENT =
            "androidx.car.app.mediaextensions.analytics.viewcomponent";
    @RestrictTo(LIBRARY)
    @SuppressWarnings("IntentName")
    public static final String ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION =
            "androidx.car.app.mediaextensions.analytics.viewaction";
    @RestrictTo(LIBRARY)
    @SuppressWarnings("IntentName")
    public static final String ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION_MODE =
            "androidx.car.app.mediaextensions.analytics.viewactionmode";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_PARENT_NODE_ID =
            "androidx.car.app.mediaextensions.analytics.visibleitemsnodeid";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_ITEM_IDS =
            "androidx.car.app.mediaextensions.analytics.visibleitemsids";
    @RestrictTo(LIBRARY)
    public static final String ANALYTICS_EVENT_DATA_KEY_BROWSE_MODE =
            "androidx.car.app.mediaextensions.analytics.browsemode";
}
