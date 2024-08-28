/*
 * Copyright (C) 2024 The Android Open Source Project
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

/**
 * Defines constants for action and extra keys for CarMediaApp.
 */
public class MediaIntentExtras {

    // Do not instantiate
    private MediaIntentExtras() {
    }

    /**
     * Activity Action: Provide media playing through a media template app. The usage is the same as
     * <a href="https://developer.android.com/reference/android/car/media/CarMediaIntents#ACTION_MEDIA_TEMPLATE">ACTION_MEDIA_TEMPLATE</a>
     * A V2 is provided so that the media apps can know whether the system they run on supports the
     * new parameters.
     * <p> Input: these optional extras
     * <ul>
     * <li> {@link #EXTRA_KEY_MEDIA_COMPONENT} </li>
     * <li> {@link #EXTRA_KEY_MEDIA_ID} </li>
     * <li> {@link #EXTRA_KEY_SEARCH_QUERY} </li>
     * <li> {@link #EXTRA_KEY_SEARCH_ACTION} </li>
     * </ul>
     * If no extra is specified, the current media source is opened.
     */
    public static final String ACTION_MEDIA_TEMPLATE_V2 =
            "androidx.car.app.mediaextensions.action.MEDIA_TEMPLATE_V2";

    /**
     * {@link Bundle} key used as a string extra field with {@link #ACTION_MEDIA_TEMPLATE_V2} to
     * specify the MediaBrowserService that user wants to start the media on.
     * <p>TYPE: String.
     * The value of this extra is the same as the
     * <a href="https://developer.android.com/reference/android/car/media/CarMediaIntents#EXTRA_MEDIA_COMPONENT">EXTRA_MEDIA_COMPONENT</a>
     * for easy access for 3P developers.
     */
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_KEY_MEDIA_COMPONENT =
            "android.car.intent.extra.MEDIA_COMPONENT";

    /**
     * {@link Bundle} key used as a string extra field with {@link #ACTION_MEDIA_TEMPLATE_V2} to
     * specify the media item that should be displayed in the browse view. Must match the ids used
     * in the MediaBrowserServiceCompat api.
     * <p>TYPE: String.
     */
    public static final String EXTRA_KEY_MEDIA_ID =
            "androidx.car.app.mediaextensions.extra.KEY_MEDIA_ID";

    /**
     * {@link Bundle} key used as a string extra field with {@link #ACTION_MEDIA_TEMPLATE_V2} to
     * specify the search query to send either to the current MediaBrowserService or the one
     * specified with {@link #EXTRA_KEY_MEDIA_COMPONENT}.
     * <p>TYPE: String.
     * The value of this extra is the same as the
     * <a href="https://developer.android.com/reference/android/car/media/CarMediaIntents#EXTRA_SEARCH_QUERY">EXTRA_SEARCH_QUERY</a>
     * for easy access for 3P developers.
     */
    @SuppressWarnings("ActionValue")
    public static final String EXTRA_KEY_SEARCH_QUERY =
            "android.car.media.extra.SEARCH_QUERY";

    /**
     * {@link Bundle} key used as an int extra field with {@link #ACTION_MEDIA_TEMPLATE_V2} to
     * specify the action for the Media Center to do after the search query is loaded.
     * <p>TYPE: int.
     * The value will be one of the following:
     * {@link #EXTRA_VALUE_NO_SEARCH_ACTION},
     * {@link #EXTRA_VALUE_PLAY_FIRST_ITEM_FROM_SEARCH},
     * This extra should only be used together with {@link #EXTRA_KEY_SEARCH_QUERY}. If this extra
     * is not specified, then no further action will be taken after the search results are loaded.
     */
    public static final String EXTRA_KEY_SEARCH_ACTION =
            "androidx.car.app.mediaextensions.extra.KEY_SEARCH_ACTION";

    /**
     * The extra value to indicate that no further action will be taken after the search results are
     * loaded from a search query. Used with {@link #EXTRA_KEY_SEARCH_QUERY}.
     */
    public static final int EXTRA_VALUE_NO_SEARCH_ACTION = 0;

    /**
     * The extra value to indicate that the first playable item will automatically be played from
     * the displayed search results after a search query is done. Used with
     * {@link #EXTRA_KEY_SEARCH_QUERY}.
     */
    public static final int EXTRA_VALUE_PLAY_FIRST_ITEM_FROM_SEARCH = 1;
}
