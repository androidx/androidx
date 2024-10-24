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
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_BROWSE_MODE;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_MEDIA_ID;
import static androidx.car.app.mediaextensions.analytics.Constants.ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEventsUtil.browseModeToString;
import static androidx.car.app.mediaextensions.analytics.event.AnalyticsEventsUtil.viewActionToString;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;

/** Describes a browse node change event.
 * @see
 * <a href="URL#https://developer.android.com/guide/topics/media/legacy/audio/mediabrowserservice">MediaBrowserService</a>
 */
public class BrowseChangeEvent extends AnalyticsEvent {

    /** Indicates an unknown browse mode type. */
    public static final int BROWSE_MODE_UNKNOWN = 0;
    /** Indicates browsing media items tree root*/
    public static final int BROWSE_MODE_TREE_ROOT = 1;
    /** Indicates browsing under media items tree root*/
    public static final int BROWSE_MODE_TREE_BROWSE = 2;
    /** Indicates browsing media items one level under root, i.e. tabs.*/
    public static final int BROWSE_MODE_TREE_TAB = 3;
    /** Indicates browsing linked media item*/
    public static final int BROWSE_MODE_LINK = 4;
    /** Indicates browsing media items under linked media item*/
    public static final int BROWSE_MODE_LINK_BROWSE = 5;
    /** Indicates browsing media items under a search result media item*/
    public static final int BROWSE_MODE_SEARCH_BROWSE = 6;
    /** Indicates browsing search results media items*/
    public static final int BROWSE_MODE_SEARCH_RESULTS = 7;


    @Retention(SOURCE)
    @IntDef(
            value = {BROWSE_MODE_UNKNOWN, BROWSE_MODE_TREE_ROOT, BROWSE_MODE_TREE_BROWSE,
                    BROWSE_MODE_TREE_TAB, BROWSE_MODE_LINK, BROWSE_MODE_LINK_BROWSE,
                    BROWSE_MODE_SEARCH_BROWSE, BROWSE_MODE_SEARCH_RESULTS}
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface BrowseMode {}

    @ViewAction
    private final int mViewAction;
    @BrowseMode
    private final int mBrowseMode;
    private final String mBrowseNodeId;

    @RestrictTo(LIBRARY)
    public BrowseChangeEvent(@NonNull Bundle eventBundle) {
        super(eventBundle, EVENT_TYPE_BROWSE_NODE_CHANGED_EVENT);
        mViewAction = eventBundle.getInt(ANALYTICS_EVENT_DATA_KEY_VIEW_ACTION);
        mBrowseMode = eventBundle.getInt(ANALYTICS_EVENT_DATA_KEY_BROWSE_MODE);
        mBrowseNodeId = eventBundle.getString(ANALYTICS_EVENT_DATA_KEY_MEDIA_ID);
    }

    /**
     * Returns the
     * {@link androidx.car.app.mediaextensions.analytics.event.AnalyticsEvent.ViewAction}
     * related to this Browse Change.
     */
    @ViewAction
    public int getViewAction() {
        return mViewAction;
    }

    /**
     * Returns the {@link android.media.browse.MediaBrowser.MediaItem} ID that is the new browse
     * node.
     * @return String MediaItem ID.
     */
    public @NonNull String getBrowseNodeId() {
        return mBrowseNodeId;
    }

    /**
     * Mode Platform Component is viewing Browse Nodes.
     */
    @BrowseMode
    public int getBrowseMode() {
        return mBrowseMode;
    }

    @Override
    public @NonNull String toString() {
        final StringBuilder sb = new StringBuilder("BrowseChangeEvent{");
        sb.append("mBrowseMode=").append(browseModeToString(mBrowseMode));
        sb.append(", mViewAction=").append(viewActionToString(mViewAction));
        sb.append(", mBrowseNodeId='").append(mBrowseNodeId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
