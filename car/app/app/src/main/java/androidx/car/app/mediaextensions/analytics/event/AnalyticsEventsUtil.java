/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.mediaextensions.analytics.event;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_ACTION_HIDE;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_ACTION_MODE_NONE;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_ACTION_MODE_SCROLL;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_ACTION_SHOW;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_BROWSE_ACTION_OVERFLOW;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_BROWSE_LIST;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_BROWSE_TABS;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_ERROR_MESSAGE;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_LAUNCHER;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_MEDIA_HOST;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_MINI_PLAYBACK;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_PLAYBACK;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_QUEUE_LIST;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.VIEW_COMPONENT_SETTINGS_VIEW;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Utils for {@link AnalyticsEvent} and subclasses.
 */
@RestrictTo(LIBRARY)
public class AnalyticsEventsUtil {

    private AnalyticsEventsUtil() {}

    /**
     * Convert {@link AnalyticsEvent.ViewComponent} to human readable text.
     */
    @RestrictTo(LIBRARY)
    public static @NonNull String viewComponentToString(
            @AnalyticsEvent.ViewComponent int viewComponent) {
        switch (viewComponent) {
            case VIEW_COMPONENT_BROWSE_ACTION_OVERFLOW:
                return "BROWSE_ACTION_OVERFLOW";
            case VIEW_COMPONENT_BROWSE_LIST:
                return "BROWSE_LIST";
            case VIEW_COMPONENT_BROWSE_TABS:
                return "BROWSE_TABS";
            case VIEW_COMPONENT_ERROR_MESSAGE:
                return "ERROR_MESSAGE";
            case VIEW_COMPONENT_LAUNCHER:
                return "LAUNCHER";
            case VIEW_COMPONENT_MEDIA_HOST:
                return "MEDIA_HOST";
            case VIEW_COMPONENT_MINI_PLAYBACK:
                return "MINI_PLAYBACK";
            case VIEW_COMPONENT_PLAYBACK:
                return "PLAYBACK";
            case VIEW_COMPONENT_QUEUE_LIST:
                return "QUEUE_LIST";
            case VIEW_COMPONENT_SETTINGS_VIEW:
                return "SETTINGS_VIEW";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Converts {@link AnalyticsEvent.ViewAction} to human readable text.
     */
    @RestrictTo(LIBRARY)
    public static @NonNull String viewActionToString(@AnalyticsEvent.ViewAction int viewAction) {
        switch (viewAction) {
            case VIEW_ACTION_HIDE:
                return "HIDE";
            case VIEW_ACTION_SHOW:
                return "SHOW";
            default:
                return "UNKNOWN";
        }
    }

    /** converts view action flag to human readable text. */
    public static @NonNull String viewActionModeToString(
            @AnalyticsEvent.ViewActionMode int viewActionMode) {
        switch (viewActionMode) {
            case VIEW_ACTION_MODE_NONE:
                return "NONE";
            case VIEW_ACTION_MODE_SCROLL:
                return "SCROLL";
            default:
                return "UNKNOWN";
        }
    }

    /**Converts browse mode to human readable text */
    public static @NonNull String browseModeToString(@BrowseChangeEvent.BrowseMode int browseMode) {
        switch (browseMode) {
            case BrowseChangeEvent.BROWSE_MODE_LINK:
                return "LINK";
            case BrowseChangeEvent.BROWSE_MODE_LINK_BROWSE:
                return "LINK_BROWSE";
            case BrowseChangeEvent.BROWSE_MODE_TREE_TAB:
                return "TREE_TAB";
            case BrowseChangeEvent.BROWSE_MODE_TREE_ROOT:
                return "TREE_ROOT";
            case BrowseChangeEvent.BROWSE_MODE_TREE_BROWSE:
                return "TREE_BROWSE";
            case BrowseChangeEvent.BROWSE_MODE_SEARCH_BROWSE:
                return "SEARCH_BROWSE";
            case BrowseChangeEvent.BROWSE_MODE_SEARCH_RESULTS:
                return "SEARCH_RESULTS";
        }

        return "" + browseMode;
    }

}
