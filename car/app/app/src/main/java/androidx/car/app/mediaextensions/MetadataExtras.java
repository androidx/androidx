/*
 * Copyright 2022 The Android Open Source Project
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

/**
 * Defines constants for extra keys in {@link android.support.v4.media.MediaMetadataCompat} or
 * {@link androidx.media3.common.MediaMetadata}.
 */
public final class MetadataExtras {

    // Do not instantiate
    private MetadataExtras() {
    }

    /**
     * {@link Bundle} key used in the extras of a media item to indicate that the subtitle of the
     * corresponding media item can be linked to another media item ID.
     * <p>The value of the extra is set to the media ID of this other item.
     *
     * <p>NOTE: media1 and media3 apps setting this extra <b>must implement</b>
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadItem} or
     * {@link  androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback#onGetItem}
     * respectively.
     * <p>NOTE: media apps setting this extra <b>must explicitly set the subtitle property</b>.
     * <p>See {@link android.support.v4.media.MediaMetadataCompat#METADATA_KEY_DISPLAY_SUBTITLE}
     * <p>See {@link androidx.media3.common.MediaMetadata#subtitle}
     *
     * <p>TYPE: String.
     * <p>
     * <p> Example:
     * <pre>
     *   "Source" MediaItem
     *      + mediaId:                  “Beethoven-9th-symphony”    // ID
     *      + title:                    “9th symphony”              // Track
     *      + subtitle:                 “The best of Beethoven”     // Album
     * ╔════+ subtitleLinkMediaId:      “Beethoven-best-of”         // Album ID
     * ║    + description:              “Beethoven”                 // Artist
     * ║    + descriptionLinkMediaId:   “artist:Beethoven”          // Artist ID
     * ║
     * ║ "Destination" MediaItem
     * ╚════+ mediaId:                  “Beethoven-best-of”         // ID
     *      + title:                    “The best of Beethoven”     // Album
     *      + subtitle:                 “Beethoven”                 // Artist
     *      + subtitleLinkMediaId:      “artist:Beethoven”          // Artist ID
     * </pre>
     */
    public static final String KEY_SUBTITLE_LINK_MEDIA_ID =
            "androidx.car.app.mediaextensions.KEY_SUBTITLE_LINK_MEDIA_ID";

    /**
     * {@link Bundle} key used in the extras of a media item to indicate that the description of the
     * corresponding media item can be linked to another media item ID.
     * <p>The value of the extra is set to the media ID of this other item.
     *
     * <p>NOTE: media1 and media3 apps setting this extra <b>must implement</b>
     * {@link androidx.media.MediaBrowserServiceCompat#onLoadItem} or
     * {@link androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback#onGetItem}
     * respectively.
     * <p>NOTE: media apps setting this extra <b>must explicitly set the description property</b>.
     * <p>See {@link android.support.v4.media.MediaMetadataCompat#METADATA_KEY_DISPLAY_DESCRIPTION}
     * <p>See {@link androidx.media3.common.MediaMetadata#description}
     *
     * <p>TYPE: String.
     * <p>
     * <p> Example:
     * <pre>
     *   "Source" MediaItem
     *      + mediaId:                  “Beethoven-9th-symphony”    // ID
     *      + title:                    “9th symphony”              // Track
     *      + subtitle:                 “The best of Beethoven”     // Album
     *      + subtitleLinkMediaId:      “Beethoven-best-of”         // Album ID
     *      + description:              “Beethoven”                 // Artist
     * ╔════+ descriptionLinkMediaId:   “artist:Beethoven”          // Artist ID
     * ║
     * ║ "Destination" MediaItem
     * ╚════+ mediaId:                  “artist:Beethoven”          // ID
     *      + title:                    “Beethoven”                 // Artist
     * </pre>
     */
    public static final String KEY_DESCRIPTION_LINK_MEDIA_ID =
            "androidx.car.app.mediaextensions.KEY_DESCRIPTION_LINK_MEDIA_ID";

    /**
     * {@link Bundle} key used in the extras of a media item to indicate an immersive audio
     * experience. Car OEMs should carefully consider which audio effects should be enabled for
     * such content.
     *
     * <p>TYPE: long - to enable, use value
     * {@link androidx.media.utils.MediaConstants#METADATA_VALUE_ATTRIBUTE_PRESENT}</p>
     */
    public static final String KEY_IMMERSIVE_AUDIO =
            "androidx.car.app.mediaextensions.KEY_IMMERSIVE_AUDIO";

    /**
     * {@link Bundle} key used in the extras of a media item to indicate that the metadata of this
     * media item should not be shown next to content from other applications
     *
     * <p>TYPE: long - to enable, use value
     * {@link androidx.media.utils.MediaConstants#METADATA_VALUE_ATTRIBUTE_PRESENT}</p>
     */
    public static final String KEY_EXCLUDE_MEDIA_ITEM_FROM_MIXED_APP_LIST =
            "androidx.car.app.mediaextensions.KEY_EXCLUDE_MEDIA_ITEM_FROM_MIXED_APP_LIST";

    /**
     * {@link Bundle} key used in the extras of a media item to indicate a tintable vector drawable
     * representing its content format. This drawable must be rendered in large views showing
     * information about the currently playing media item, in an area roughly equivalent to 15
     * characters of subtitle.
     *
     * <p>TYPE: String - a uri pointing to local content (ie not on the web) that can be parsed
     * into a android.graphics.drawable.Drawable</p>
     */
    public static final String KEY_CONTENT_FORMAT_TINTABLE_LARGE_ICON_URI =
            "androidx.car.app.mediaextensions.KEY_CONTENT_FORMAT_TINTABLE_LARGE_ICON_URI";

    /**
     * {@link Bundle} key used in the extras of a media item to indicate a tintable vector drawable
     * representing its content format. This drawable may be rendered in smaller views showing
     * information about a media item, in an area roughly equivalent to 2 characters of subtitle.
     *
     * <p>TYPE: String - a uri pointing to local content (ie not on the web) that can be parsed
     * into a android.graphics.drawable.Drawable</p>
     */
    public static final String KEY_CONTENT_FORMAT_TINTABLE_SMALL_ICON_URI =
            "androidx.car.app.mediaextensions.KEY_CONTENT_FORMAT_TINTABLE_SMALL_ICON_URI";

    /**
     * {@link Bundle} key used in the extras of a media item to indicate a list of tintable vector
     * drawables where each drawable represents a property or a state of the media item.
     * These drawables may be rendered in small views showing information about a media item,
     * in an area roughly equivalent to 2 characters of the media item's subtitle.<br/>
     *
     * <b>Note:</b> when specifying this extra, the "android.media.extra.DOWNLOAD_STATUS" and
     * "android.media.IS_EXPLICIT" extras will be ignored, so 3p apps should add their own
     * downloaded and explicit icon uris to this extra's list. This way all these icons can use
     * the same drawing style.
     *
     * <p>TYPE: {@code ArrayList<Uri>}, list of Uris - with each uri pointing to local content
     * using either ContentResolver.SCHEME_CONTENT or ContentResolver.SCHEME_ANDROID_RESOURCE
     * (ie not on the web) that can be parsed into a android.graphics.drawable.Drawable</p>
     */
    public static final String KEY_TINTABLE_INDICATOR_ICON_URI_LIST =
            "androidx.car.app.mediaextensions.KEY_TINTABLE_INDICATOR_ICON_URI_LIST";

    /**
     * {@link Bundle} key used in the metadata of the currently playing media item to request a
     * custom accent color for media playback controls (such as the play/pause button and the
     * progress bar) when the host displays dark surfaces, for example in a dark theme. Choose a
     * light color that contrasts with dark backgrounds, for example white.
     *
     * <p>The host uses its default colors if the contrast requirements are not met. It is
     * recommended to set both this key and {@link #KEY_MEDIA_ACCENT_COLOR_DARK}.
     *
     * <p>TYPE: long - an ARGB color, for example {@code 0xFFFFFFFFL}. The color is always drawn
     * fully opaque.
     *
     * @see #KEY_MEDIA_ACCENT_COLOR_DARK
     */
    public static final String KEY_MEDIA_ACCENT_COLOR =
            "androidx.car.app.mediaextensions.KEY_MEDIA_ACCENT_COLOR";

    /**
     * {@link Bundle} key used in the metadata of the currently playing media item to request a
     * custom accent color for media playback controls (such as the play/pause button and the
     * progress bar) when the host displays light surfaces, for example in a light theme. Choose a
     * dark color that contrasts with light backgrounds, for example black.
     *
     * <p>The host uses its default colors if the contrast requirements are not met. It is
     * recommended to set both this key and {@link #KEY_MEDIA_ACCENT_COLOR}.
     *
     * <p>TYPE: long - an ARGB color, for example {@code 0xFF000000L}. The color is always drawn
     * fully opaque.
     *
     * @see #KEY_MEDIA_ACCENT_COLOR
     */
    public static final String KEY_MEDIA_ACCENT_COLOR_DARK =
            "androidx.car.app.mediaextensions.KEY_MEDIA_ACCENT_COLOR_DARK";
}
