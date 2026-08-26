/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.mediarouter.media.MediaRouter.UnselectReason;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Information about a route selection.
 *
 * <p>This information is available in callbacks like {@link
 * MediaRouter.Callback#onRouteSelected(MediaRouter, MediaRouter.RouteInfo, MediaRouter.RouteInfo,
 * SelectionInfo)} for apps to know the reason and the source of the event. For example, you can
 * check if the route was selected by the app ({@link #SELECTION_SOURCE_APP}) (this includes in-app
 * UI interactions) or via the system output switcher ({@link #SELECTION_SOURCE_SYSTEM}). Similarly,
 * apps can know if the previous route was unselected because it was disconnected ({@link
 * MediaRouter#UNSELECT_REASON_DISCONNECTED}) or simply stopped ({@link
 * MediaRouter#UNSELECT_REASON_STOPPED}).
 *
 * @see #getSelectionSource()
 * @see #getUnselectReason()
 */
public final class SelectionInfo {

    /** Builder for {@link SelectionInfo}. */
    public static final class Builder {
        int mUnselectReason = MediaRouter.UNSELECT_REASON_UNKNOWN;
        int mSelectionSource = SELECTION_SOURCE_UNKNOWN;

        /** Creates an empty builder. */
        public Builder() {}

        /**
         * Sets the unselect reason of the previously selected route.
         *
         * <p>The default value is {@link MediaRouter#UNSELECT_REASON_UNKNOWN}.
         *
         * @param unselectReason The unselect reason.
         * @return The builder instance.
         */
        @NonNull
        public Builder setUnselectReason(@UnselectReason int unselectReason) {
            mUnselectReason = unselectReason;
            return this;
        }

        /**
         * Sets the source of the route selection.
         *
         * <p>The default value is {@link #SELECTION_SOURCE_UNKNOWN}.
         *
         * @param selectionSource The selection source.
         * @return The builder instance.
         */
        @NonNull
        public Builder setSelectionSource(@SelectionSource int selectionSource) {
            mSelectionSource = selectionSource;
            return this;
        }

        /**
         * Builds the {@link SelectionInfo} instance.
         *
         * @return The built {@link SelectionInfo} instance.
         */
        @NonNull
        public SelectionInfo build() {
            return new SelectionInfo(this);
        }
    }

    /** The source of a route selection. */
    @RestrictTo(LIBRARY)
    @IntDef({
        SELECTION_SOURCE_UNKNOWN,
        SELECTION_SOURCE_APP,
        SELECTION_SOURCE_SYSTEM,
        SELECTION_SOURCE_PROVIDER
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelectionSource {}

    /** Passed when the reason the route was selected is unknown. */
    public static final int SELECTION_SOURCE_UNKNOWN = 0;

    /**
     * The route was selected because the application explicitly requested it (for example, by
     * calling {@link MediaRouter#selectRoute(MediaRouter.RouteInfo)}).
     */
    public static final int SELECTION_SOURCE_APP = 1;

    /**
     * The route was selected because of a system/platform event (for example, System Output
     * Switcher selection).
     */
    public static final int SELECTION_SOURCE_SYSTEM = 2;

    /**
     * The route was selected because the provider changed the route (for example, fallback to
     * default route when the active route is disconnected by the provider).
     */
    public static final int SELECTION_SOURCE_PROVIDER = 3;

    private final int mUnselectReason;
    private final int mSelectionSource;

    private SelectionInfo(Builder builder) {
        mUnselectReason = builder.mUnselectReason;
        mSelectionSource = builder.mSelectionSource;
    }

    /**
     * Returns the unselect reason of the previously selected route.
     *
     * @return The unselect reason. One of {@link MediaRouter#UNSELECT_REASON_UNKNOWN}, {@link
     *     MediaRouter#UNSELECT_REASON_DISCONNECTED}, {@link MediaRouter#UNSELECT_REASON_STOPPED},
     *     or {@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}.
     */
    @UnselectReason
    public int getUnselectReason() {
        return mUnselectReason;
    }

    /**
     * Returns the source of the route selection.
     *
     * @return The selection source. One of {@link #SELECTION_SOURCE_UNKNOWN}, {@link
     *     #SELECTION_SOURCE_APP}, {@link #SELECTION_SOURCE_SYSTEM}, or {@link
     *     #SELECTION_SOURCE_PROVIDER}.
     */
    @SelectionSource
    public int getSelectionSource() {
        return mSelectionSource;
    }
}
