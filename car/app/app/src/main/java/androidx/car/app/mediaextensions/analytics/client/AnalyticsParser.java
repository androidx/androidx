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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent;
import androidx.car.app.mediaextensions.analytics.event.BrowseChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.ErrorEvent;
import androidx.car.app.mediaextensions.analytics.event.MediaClickedEvent;
import androidx.car.app.mediaextensions.analytics.event.ViewChangeEvent;
import androidx.car.app.mediaextensions.analytics.event.VisibleItemsEvent;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/** Provides tools to parse AnalyticEvents from Bundle. **/
@ExperimentalCarApi
@RestrictTo(LIBRARY)
public class AnalyticsParser {
    private static final String TAG = "AnalyticsParser";

    private AnalyticsParser() {}

    /**
     * Parses batch of {@link AnalyticsEvent}s from intent.
     * <p>
     *  Deserializes each event in batch and sends to analyticsCallback
     * <p>
     *
     * @param intent intent with batch of events in extras.
     * @param executor Valid Executor on which callback will be called.
     * @param analyticsCallback callback for deserialized events.
     */
    @SuppressWarnings("deprecation")
    public static void parseAnalyticsIntent(@NonNull Intent intent, @NonNull Executor executor,
            @NonNull AnalyticsCallback analyticsCallback) {
        Bundle intentExtras = intent.getExtras();

        if (intentExtras == null || intentExtras.isEmpty()) {
            Log.e(TAG, "Analytics event bundle is null or empty.");
            analyticsCallback.onErrorEvent(new ErrorEvent(new Bundle(),
                    ErrorEvent.ERROR_CODE_INVALID_INTENT));
            return;
        }

        ArrayList<Bundle> eventBundles =
                intentExtras.getParcelableArrayList(ANALYTICS_EVENT_BUNDLE_ARRAY_KEY);

        if (eventBundles == null || eventBundles.isEmpty()) {
            Log.e(TAG, "Analytics event bundle list is empty.");
            analyticsCallback.onErrorEvent(new ErrorEvent(new Bundle(),
                    ErrorEvent.ERROR_CODE_INVALID_BUNDLE));
            return;
        }

        for (Bundle bundle : eventBundles) {
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

    private static @AnalyticsEvent.EventType int getEventType(String eventName) {
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
