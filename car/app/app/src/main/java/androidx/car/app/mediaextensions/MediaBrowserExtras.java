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

package androidx.car.app.mediaextensions;

import android.os.Bundle;

import androidx.media.MediaBrowserServiceCompat;

/**
 * Defines constants for extra keys in {@link androidx.media.MediaBrowserServiceCompat}.
 *
 * <p>Media apps can use these extras to enhance their analytics.
 *
 * <p>They can also take them into account to decide how many media items to return. For example,
 * providing a number of recommendation items that is a multiple of the maximum number of items
 * shown in a grid row will use the screen space more efficiently (avoiding blanks).
 */
public final class MediaBrowserExtras {

    // Do not instantiate
    private MediaBrowserExtras() {
    }

    /**
     * {@link Bundle} key used in the rootHints bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)}
     * to indicate the version of the caller. Note that this should only be used for analytics and
     * is different than {@link #KEY_ROOT_HINT_MEDIA_SESSION_API}.
     *
     * <p>TYPE: string - the version info.
     */
    public static final String KEY_ROOT_HINT_MEDIA_HOST_VERSION =
            "androidx.car.app.mediaextensions.KEY_ROOT_HINT_MEDIA_HOST_VERSION";

    /**
     * {@link Bundle} key used in the rootHints bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)} to indicate
     * which version of the media api is used by the caller
     *
     * <p>TYPE: int - the media api level (1, 2, 3).
     */
    public static final String KEY_ROOT_HINT_MEDIA_SESSION_API =
            "androidx.car.app.mediaextensions.KEY_ROOT_HINT_MEDIA_SESSION_API";

    /**
     * {@link Bundle} key used in the rootHints bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)} to indicate
     * the maximum number of queue items reachable under driving restrictions. This sublist is
     * centered around the currently playing item.
     *
     * <p>TYPE: int - the maximum number of queue items when restricted, -1 when unlimited
     */
    public static final String KEY_ROOT_HINT_MAX_QUEUE_ITEMS_WHILE_RESTRICTED =
            "androidx.car.app.mediaextensions.KEY_ROOT_HINT_MAX_QUEUE_ITEMS_WHILE_RESTRICTED";

    /**
     * {@link Bundle} key used in the options bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadChildren(String,
     * MediaBrowserServiceCompat.Result, Bundle)} or to
     * {@link androidx.media.MediaBrowserServiceCompat#onSearch(String, Bundle,
     * MediaBrowserServiceCompat.Result)} to indicate the maximum number of returned items
     * reachable under driving restrictions.
     *
     * <p>TYPE: int - the maximum number of items when restricted, -1 when unlimited
     */
    public static final String KEY_HINT_VIEW_MAX_ITEMS_WHILE_RESTRICTED =
            "androidx.car.app.mediaextensions.KEY_HINT_VIEW_MAX_ITEMS_WHILE_RESTRICTED";

    /**
     * {@link Bundle} key used in the options bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadChildren(String,
     * MediaBrowserServiceCompat.Result, Bundle)} or to
     * {@link androidx.media.MediaBrowserServiceCompat#onSearch(String, Bundle,
     * MediaBrowserServiceCompat.Result)} to indicate how many media items tagged with
     * {@link androidx.media.utils.MediaConstants#DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM}
     * are displayed on a single row.
     *
     * <p>TYPE: int - maximum number of list items per row.
     */
    public static final String KEY_HINT_VIEW_MAX_LIST_ITEMS_COUNT_PER_ROW =
            "androidx.car.app.mediaextensions.KEY_HINT_VIEW_MAX_LIST_ITEMS_COUNT_PER_ROW";

    /**
     * {@link Bundle} key used in the options bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadChildren(String,
     * MediaBrowserServiceCompat.Result, Bundle)} or to
     * {@link androidx.media.MediaBrowserServiceCompat#onSearch(String, Bundle,
     * MediaBrowserServiceCompat.Result)} to indicate how many media items tagged with
     * {@link androidx.media.utils.MediaConstants#DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM}
     * are displayed on a single row.
     *
     * <p>TYPE: int - maximum number of grid items per row.
     */
    public static final String KEY_HINT_VIEW_MAX_GRID_ITEMS_COUNT_PER_ROW =
            "androidx.car.app.mediaextensions.KEY_HINT_VIEW_MAX_GRID_ITEMS_COUNT_PER_ROW";

    /**
     * {@link Bundle} key used in the options bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadChildren(String,
     * MediaBrowserServiceCompat.Result, Bundle)} or to
     * {@link androidx.media.MediaBrowserServiceCompat#onSearch(String, Bundle,
     * MediaBrowserServiceCompat.Result)} to indicate how many media items tagged with
     * {@link androidx.media.utils.MediaConstants#DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM}
     * are displayed on a single row.
     *
     * <p>TYPE: int - maximum number of category list items per row.
     */
    public static final String KEY_HINT_VIEW_MAX_CATEGORY_LIST_ITEMS_COUNT_PER_ROW =
            "androidx.car.app.mediaextensions.KEY_HINT_VIEW_MAX_CATEGORY_LIST_ITEMS_COUNT_PER_ROW";

    /**
     * {@link Bundle} key used in the options bundle passed to
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadChildren(String,
     * MediaBrowserServiceCompat.Result, Bundle)} or to
     * {@link androidx.media.MediaBrowserServiceCompat#onSearch(String, Bundle,
     * MediaBrowserServiceCompat.Result)} to indicate how many media items tagged with
     * {@link androidx.media.utils.MediaConstants#DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM}
     * are displayed on a single row.
     *
     * <p>TYPE: int - maximum number of category grid items per row.
     */
    public static final String KEY_HINT_VIEW_MAX_CATEGORY_GRID_ITEMS_COUNT_PER_ROW =
            "androidx.car.app.mediaextensions.KEY_HINT_VIEW_MAX_CATEGORY_GRID_ITEMS_COUNT_PER_ROW";
}
