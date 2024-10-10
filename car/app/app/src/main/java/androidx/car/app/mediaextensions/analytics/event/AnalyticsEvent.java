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

package androidx.car.app.mediaextensions.analytics.event;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_HOST_COMPONENT_ID;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_TIMESTAMP;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_VERSION;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;

/** Base class for Analytics events. */
public  abstract class AnalyticsEvent {


    /** Indicates event generated from Browse List view component*/
    public static final int VIEW_COMPONENT_BROWSE_LIST = 1;
    /** Indicates event generated from Browse Tabs view component*/
    public static final int VIEW_COMPONENT_BROWSE_TABS = 2;
    /** Indicates event generated from Queue List view component*/
    public static final int VIEW_COMPONENT_QUEUE_LIST = 3;
    /** Indicates event generated from Playback view component*/
    public static final int VIEW_COMPONENT_PLAYBACK = 4;
    /** Indicates event generated from Mini Playback view component*/
    public static final int VIEW_COMPONENT_MINI_PLAYBACK = 5;
    /** Indicates event generated from Launcher view component*/
    public static final int VIEW_COMPONENT_LAUNCHER = 6;
    /** Indicates event generated from Settings view component*/
    public static final int VIEW_COMPONENT_SETTINGS_VIEW = 7;
    /** Indicates event generated from Browse Action Overflow view component*/
    public static final int VIEW_COMPONENT_BROWSE_ACTION_OVERFLOW = 8;
    /** Indicates event generated from Media Host view component*/
    public static final int VIEW_COMPONENT_MEDIA_HOST = 9;
    /** Indicates event generated from Error Message view component*/
    public static final int VIEW_COMPONENT_ERROR_MESSAGE = 10;
    /** Indicates event generated from an Unknown view component*/
    public static final int VIEW_COMPONENT_UNKNOWN_COMPONENT = 0;


    @Retention(SOURCE)
    @IntDef(
            value = {VIEW_COMPONENT_BROWSE_LIST, VIEW_COMPONENT_BROWSE_TABS,
                    VIEW_COMPONENT_QUEUE_LIST, VIEW_COMPONENT_PLAYBACK,
                    VIEW_COMPONENT_MINI_PLAYBACK, VIEW_COMPONENT_LAUNCHER,
                    VIEW_COMPONENT_SETTINGS_VIEW, VIEW_COMPONENT_BROWSE_ACTION_OVERFLOW,
                    VIEW_COMPONENT_MEDIA_HOST, VIEW_COMPONENT_ERROR_MESSAGE,
                    VIEW_COMPONENT_UNKNOWN_COMPONENT}
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ViewComponent {}

    /** Indicates view was hidden*/
    public static final int VIEW_ACTION_HIDE = 0;
    /** Indicates view was shown*/
    public static final int VIEW_ACTION_SHOW = 1;

    @Retention(SOURCE)
    @IntDef(
            value = {VIEW_ACTION_SHOW, VIEW_ACTION_HIDE})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ViewAction {}

    /** Indicates view action mode is default/none */
    public static final int VIEW_ACTION_MODE_NONE = 0;
    /** Indicates view action mode is from a scroll */
    public static final int VIEW_ACTION_MODE_SCROLL = 1;

    @Retention(SOURCE)
    @IntDef(
            value = {VIEW_ACTION_MODE_NONE, VIEW_ACTION_MODE_SCROLL}
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ViewActionMode {}

    /** Indicates {@link VisibleItemsEvent} */
    public static final int EVENT_TYPE_VISIBLE_ITEMS_EVENT = 1;
    /** Indicates {@link MediaClickedEvent} */
    public static final int EVENT_TYPE_MEDIA_CLICKED_EVENT = 2;
    /** Indicates {@link BrowseChangeEvent} */
    public static final int EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT = 3;
    /** Indicates {@link ViewChangeEvent} */
    public static final int EVENT_TYPE_VIEW_CHANGE_EVENT = 4;
    /** Indicates {@link ErrorEvent} */
    public static final int EVENT_TYPE_ERROR_EVENT = 5;
    /** Indicates an unknown event */
    public static final int EVENT_TYPE_UNKNOWN_EVENT = 0;

    @Retention(SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(
            value = {EVENT_TYPE_VISIBLE_ITEMS_EVENT, EVENT_TYPE_MEDIA_CLICKED_EVENT,
                    EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT,
                    EVENT_TYPE_VIEW_CHANGE_EVENT, EVENT_TYPE_ERROR_EVENT, EVENT_TYPE_UNKNOWN_EVENT}
    )
    public @interface EventType {}

    private final int mAnalyticsVersion;
    private final @EventType int mEventType;
    private final long mTimeStampMillis;
    private final String mComponent;

    @RestrictTo(LIBRARY)
    public AnalyticsEvent(@NonNull Bundle eventBundle, @EventType int eventType) {
        mAnalyticsVersion = eventBundle.getInt(ANALYTICS_EVENT_DATA_KEY_VERSION, -1);
        mTimeStampMillis = eventBundle.getLong(ANALYTICS_EVENT_DATA_KEY_TIMESTAMP, -1);
        mComponent = eventBundle.getString(ANALYTICS_EVENT_DATA_KEY_HOST_COMPONENT_ID, "");
        mEventType = eventType;
    }

    /** Returns the analytics version. */
    public int getAnalyticsVersion() {
        return mAnalyticsVersion;
    }

    /**
     * Returns the {@link EventType}.
     * @return event type
     */
    public @EventType int getEventType() {
        return mEventType;
    }

    /**
     * Returns the time of the event in milliseconds since 1970-01-01T00:00:00Z.
     */
    public long getTimestampMillis() {
        return mTimeStampMillis;
    }

    /**
     * Returns the platform component that is generating analytic events.
     * @return component of platform that is sending events.
     */
    public @NonNull String getComponent() {
        return mComponent;
    }

    @Override
    public @NonNull String toString() {
        final StringBuilder sb = new StringBuilder("AnalyticsEvent{");
        sb.append("mAnalyticsVersion=").append(mAnalyticsVersion);
        sb.append(", mEventType=").append(mEventType);
        sb.append(", mTime=").append(mTimeStampMillis);
        sb.append(", mComponent='").append(mComponent).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
