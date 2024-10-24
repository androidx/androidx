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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_BROWSE_NODE_CHANGE;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_BUNDLE_ARRAY_KEY;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_EVENT_NAME;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_MEDIA_CLICKED;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_VIEW_CHANGE;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_VISIBLE_ITEMS;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.mediaextensions.analytics.Constants;
import androidx.car.app.mediaextensions.analytics.ThreadUtils;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.car.app.mediaextensions.analytics.event.BrowseChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.ErrorEvent;
import androidx.car.app.mediaextensions.analytics.event.MediaClickedEvent;
import androidx.car.app.mediaextensions.analytics.event.ViewChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.VisibleItemsEvent;
import androidx.media.MediaBrowserServiceCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/** Provides tools to parse AnalyticEvents from Bundle. **/
@ExperimentalCarApi
@RestrictTo(LIBRARY)
public class AnalyticsParser {
    private static final String TAG = "AnalyticsParser";

    private AnalyticsParser() {}

    /**
     *
     * Checks if supplied action is an Analytics action.
     *
     * @param action custom action
     * @return boolean value whether the action is an analytics action.
     */
    public static boolean isAnalyticsAction(@NonNull String action) {
        return Constants.ACTION_ANALYTICS.equalsIgnoreCase(action);
    }

    /**
     * Parses a batch of {@link AnalyticsEvent}s from a custom action and extras.
     * <p>
     *  Deserializes each event in batch and sends to analyticsCallback.
     * <p>
     *
     * <p>
     *     Usage: Pass in action string and extras bundle to this method from
     *     {@link androidx.media.MediaBrowserServiceCompat#onCustomAction(String, Bundle,
     *     MediaBrowserServiceCompat.Result)}.
     *     If present, the batch of analytic events will be parsed, deserialized and passed to the
     *     supplied {@link AnalyticsCallback}.
     * </p>
     *
     * @param action custom action
     * @param extras custom action extras.
     * @param analyticsCallback callback for deserialized events.
     */
    public static void parseAnalyticsAction(@NonNull String action, @Nullable Bundle extras,
            @NonNull AnalyticsCallback analyticsCallback) {
        parseAnalyticsAction(action, extras, ThreadUtils.getMainThreadExecutor(),
                analyticsCallback);
    }

    /**
     * Parses a batch of {@link AnalyticsEvent}s from a custom action and extras.
     * <p>
     *  Deserializes each event in batch and sends to analyticsCallback.
     * <p>
     *
     * <p>
     *     Usage: Pass in action string and extras bundle to this method from
     *     {@link
     *     androidx.media.MediaBrowserServiceCompat#onCustomAction(String, Bundle,
     *     MediaBrowserServiceCompat.Result)}.
     *     If present, the batch of analytic events will be parsed, deserialized and passed to the
     *     supplied {@link AnalyticsCallback}.
     * </p>
     *
     * @param action custom action
     * @param extras custom action extras.
     * @param executor Valid Executor on which callback will be called.
     * @param analyticsCallback callback for deserialized events.
     */
    @SuppressWarnings("deprecation")
    public static void parseAnalyticsAction(@NonNull String action, @Nullable Bundle extras,
            @NonNull Executor executor, @NonNull AnalyticsCallback analyticsCallback) {

        if (!action.equals(Constants.ACTION_ANALYTICS)) {
            analyticsCallback.onErrorEvent(new ErrorEvent(new Bundle(),
                    ErrorEvent.ERROR_CODE_INVALID_EVENT));
            return;
        }

        if (extras == null || extras.isEmpty()) {
            Log.e(TAG, "Analytics event bundle is null or empty.");
            analyticsCallback.onErrorEvent(new ErrorEvent(new Bundle(),
                    ErrorEvent.ERROR_CODE_INVALID_EXTRAS));
            return;
        }

        ArrayList<Bundle> eventBundles =
                extras.getParcelableArrayList(ANALYTICS_EVENT_BUNDLE_ARRAY_KEY);

        if (eventBundles == null || eventBundles.isEmpty()) {
            Log.e(TAG, "Analytics event bundle list is empty.");
            analyticsCallback.onErrorEvent(new ErrorEvent(new Bundle(),
                    ErrorEvent.ERROR_CODE_INVALID_BUNDLE));
            return;
        }

        for (Bundle bundle : eventBundles) {
            // TODO(b/322512398): Handle version mismatch
            AnalyticsParser.parseAnalyticsBundle(bundle, executor, analyticsCallback);
        }
    }

    /**
     * Helper method to deserialize analytics event bundles marshalled through an intent bundle.
     * <p>
     * @param analyticsBundle Bundle with serialized analytics event
     * @param analyticsCallback Callback for deserialized analytics object.
     */
    public static void parseAnalyticsBundle(@NonNull Bundle analyticsBundle,
            @NonNull Executor executor, @NonNull AnalyticsCallback analyticsCallback) {
        String eventName = analyticsBundle.getString(ANALYTICS_EVENT_DATA_KEY_EVENT_NAME, "");

        if (TextUtils.isEmpty(eventName)) {
            executor.execute(() -> analyticsCallback.onErrorEvent(new ErrorEvent(analyticsBundle,
                    ErrorEvent.ERROR_CODE_INVALID_EVENT)));
            return;
        }

        executor.execute(() -> createEvent(analyticsCallback, getEventType(eventName),
                analyticsBundle));
    }

    private static void createEvent(
            @NonNull AnalyticsCallback analyticsCallback,
            @AnalyticsEvent.EventType int eventType,
            Bundle analyticsBundle) {

        switch (eventType) {
            case AnalyticsEvent.EVENT_TYPE_VISIBLE_ITEMS_EVENT:
                analyticsCallback.onVisibleItemsEvent(new VisibleItemsEvent(analyticsBundle));
                break;
            case AnalyticsEvent.EVENT_TYPE_MEDIA_CLICKED_EVENT:
                analyticsCallback.onMediaClickedEvent(new MediaClickedEvent(analyticsBundle));
                break;
            case AnalyticsEvent.EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT:
                analyticsCallback.onBrowseNodeChangeEvent(new BrowseChangeEvent(analyticsBundle));
                break;
            case AnalyticsEvent.EVENT_TYPE_VIEW_CHANGE_EVENT:
                analyticsCallback.onViewChangeEvent(new ViewChangeEvent(analyticsBundle));
                break;
            default:
                analyticsCallback.onUnknownEvent(analyticsBundle);
                break;
        }
    }

    @AnalyticsEvent.EventType
    private static int getEventType(String eventName) {
        switch (eventName) {
            case ANALYTICS_EVENT_MEDIA_CLICKED:
                return AnalyticsEvent.EVENT_TYPE_MEDIA_CLICKED_EVENT;
            case ANALYTICS_EVENT_BROWSE_NODE_CHANGE:
                return AnalyticsEvent.EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT;
            case ANALYTICS_EVENT_VIEW_CHANGE:
                return AnalyticsEvent.EVENT_TYPE_VIEW_CHANGE_EVENT;
            case ANALYTICS_EVENT_VISIBLE_ITEMS:
                return AnalyticsEvent.EVENT_TYPE_VISIBLE_ITEMS_EVENT;
            default:
                return AnalyticsEvent.EVENT_TYPE_UNKNOWN_EVENT;
        }
    }
}
