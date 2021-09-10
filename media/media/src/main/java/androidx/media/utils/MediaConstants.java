/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.media.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RestrictTo;
import androidx.media.MediaBrowserServiceCompat;

/** Media constants for sharing constants between media provider and consumer apps */
public final class MediaConstants {
    /**
     * Bundle key used for the account name in {@link MediaSessionCompat session} extras.
     *
     * <p>TYPE: String
     *
     * @see MediaControllerCompat#getExtras
     * @see MediaSessionCompat#setExtras
     */
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_ACCOUNT_NAME =
            "androidx.media.MediaSessionCompat.Extras.KEY_ACCOUNT_NAME";

    /**
     * Bundle key used for the account type in {@link MediaSessionCompat session} extras. The value
     * would vary across media applications.
     *
     * <p>TYPE: String
     *
     * @see MediaControllerCompat#getExtras
     * @see MediaSessionCompat#setExtras
     */
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_ACCOUNT_TYPE =
            "androidx.media.MediaSessionCompat.Extras.KEY_ACCOUNT_TYPE";

    /**
     * Bundle key used for the account auth token value in {@link MediaSessionCompat session}
     * extras. The value would vary across media applications.
     *
     * <p>TYPE: byte[]
     *
     * @see MediaControllerCompat#getExtras
     * @see MediaSessionCompat#setExtras
     * @hide
     */
    @RestrictTo(LIBRARY)
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_AUTHTOKEN =
            "androidx.media.MediaSessionCompat.Extras.KEY_AUTHTOKEN";

    /**
     * Bundle key passed from {@link MediaSessionCompat} to the hosting {@link
     * MediaControllerCompat} to indicate a preference that a region of space for the skip to next
     * control should always be blocked out in the UI, even when the {@link
     * PlaybackStateCompat#ACTION_SKIP_TO_NEXT skip to next standard action} is not supported. This
     * may be used when the session temporarily hides skip to next by design.
     *
     * <p>TYPE: boolean
     *
     * @see MediaControllerCompat#getExtras()
     * @see MediaSessionCompat#setExtras(Bundle)
     */
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_SLOT_RESERVATION_SKIP_TO_NEXT =
            "android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_NEXT";

    /**
     * Bundle key passed from {@link MediaSessionCompat} to the hosting {@link
     * MediaControllerCompat} to indicate a preference that a region of space for the skip to
     * previous control should always be blocked out in the UI, even when the {@link
     * PlaybackStateCompat#ACTION_SKIP_TO_PREVIOUS skip to previous standard action} is not
     * supported. This may be used when the session temporarily hides skip to previous by design.
     *
     * <p>TYPE: boolean
     *
     * @see MediaControllerCompat#getExtras()
     * @see MediaSessionCompat#setExtras(Bundle)
     */
    @SuppressLint("IntentName")
    public static final String SESSION_EXTRAS_KEY_SLOT_RESERVATION_SKIP_TO_PREV =
            "android.media.playback.ALWAYS_RESERVE_SPACE_FOR.ACTION_SKIP_TO_PREVIOUS";

    /**
     * Bundle key used for media content id in {@link MediaMetadataCompat metadata}, should contain
     * the same ID provided to
     * <a href="https://developers.google.com/actions/media">Media Actions Catalog</a> in reference
     * to this title (e.g., episode, movie). This key can contain the content ID of the currently
     * playing episode or movie and can be used to help users continue watching after this
     * session is paused or stopped.
     *
     * <p>TYPE: String
     *
     * @see MediaMetadataCompat
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_CONTENT_ID =
            "androidx.media.MediaMetadatCompat.METADATA_KEY_CONTENT_ID";

    /**
     * Bundle key used for next episode's media content ID in {@link MediaMetadataCompat metadata},
     * following the same ID and format provided to
     * <a href="https://developers.google.com/actions/media">Media Actions Catalog</a> in reference
     * to the next episode of the current title episode. This key can contain the content ID of
     * the episode immediately following the currently playing episode and can be used to help
     * users continue watching after this episode is over. This value is only valid for TV
     * Episode content type and should be left blank for other content.
     *
     * <p>TYPE: String
     *
     * @see MediaMetadataCompat
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_NEXT_EPISODE_CONTENT_ID =
            "androidx.media.MediaMetadatCompat.METADATA_KEY_NEXT_EPISODE_CONTENT_ID";

    /**
     * Bundle key used for the TV series's media content ID in {@link MediaMetadataCompat metadata},
     * following the same ID and format provided to
     * <a href="https://developers.google.com/actions/media">Media Actions Catalog</a> in reference
     * to the TV series of the current title episode. This value is only valid for TV Episode
     * content type and should be left blank for other content.
     *
     * <p>TYPE: String
     *
     * @see MediaMetadataCompat
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_SERIES_CONTENT_ID =
            "androidx.media.MediaMetadatCompat.METADATA_KEY_SERIES_CONTENT_ID";

    /**
     * Key sent through a key-value mapping in {@link MediaMetadataCompat#getLong(String)} or in the
     * {@link MediaDescriptionCompat#getExtras()} bundle to the hosting {@link MediaBrowserCompat}
     * to indicate that the corresponding {@link MediaMetadataCompat} or {@link
     * MediaBrowserCompat.MediaItem} has explicit content (i.e. user discretion is advised when
     * viewing or listening to this content).
     *
     * <p>TYPE: long (to enable, use value {@link #METADATA_VALUE_ATTRIBUTE_PRESENT})
     *
     * @see MediaMetadataCompat#getLong(String)
     * @see MediaMetadataCompat.Builder#putLong(String, long)
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_IS_EXPLICIT = "android.media.IS_EXPLICIT";

    /**
     * Key sent through a key-value mapping in {@link MediaMetadataCompat#getLong(String)} or in the
     * {@link MediaDescriptionCompat#getExtras()} bundle to the hosting {@link MediaBrowserCompat}
     * to indicate that the corresponding {@link MediaMetadataCompat} or {@link
     * MediaBrowserCompat.MediaItem} is an advertisement.
     *
     * <p>TYPE: long (to enable, use value {@link #METADATA_VALUE_ATTRIBUTE_PRESENT})
     *
     * @see MediaMetadataCompat#getLong(String)
     * @see MediaMetadataCompat.Builder#putLong(String, long)
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     */
    @SuppressLint("IntentName")
    public static final String METADATA_KEY_IS_ADVERTISEMENT =
            "android.media.metadata.ADVERTISEMENT";

    /**
     * Value sent through a key-value mapping of {@link MediaMetadataCompat}, or through {@link
     * Bundle} extras on a different data type, to indicate the presence of an attribute described
     * by its corresponding key.
     *
     * @see MediaMetadataCompat#getLong(String)
     * @see MediaMetadataCompat.Builder#putLong(String, long)
     */
    public static final long METADATA_VALUE_ATTRIBUTE_PRESENT = 1L;

    /**
     * Bundle key passed through root hints to the {@link MediaBrowserServiceCompat} to indicate the
     * maximum number of children of the root node that can be supported by the hosting {@link
     * MediaBrowserCompat}. Excess root children may be omitted or made less discoverable by the
     * host.
     *
     * <p>TYPE: int
     *
     * @see MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)
     * @see MediaBrowserServiceCompat#getBrowserRootHints()
     * @see MediaBrowserCompat#MediaBrowserCompat(Context,ComponentName,ConnectionCallback,Bundle)
     */
    @SuppressLint("IntentName")
    public static final String BROWSER_ROOT_HINTS_KEY_ROOT_CHILDREN_LIMIT =
            "androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_LIMIT";

    /**
     * Bundle key passed through root hints to the {@link MediaBrowserServiceCompat} to indicate
     * which flags exposed by {@link MediaBrowserCompat.MediaItem#getFlags()} from children of the
     * root node are supported by the hosting {@link MediaBrowserCompat}. Root children with
     * unsupported flags may be omitted or made less discoverable by the host.
     *
     * <p>TYPE: int, a bit field which can be used as a mask. For example, if the value masked
     * (using bitwise AND) with {@link MediaBrowserCompat.MediaItem#FLAG_BROWSABLE} is nonzero, then
     * the host supports browsable root children. Conversely, if the masked result is zero, then the
     * host does not support them.
     *
     * @see MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)
     * @see MediaBrowserServiceCompat#getBrowserRootHints()
     * @see MediaBrowserCompat#MediaBrowserCompat(Context,ComponentName,ConnectionCallback,Bundle)
     */
    @SuppressLint("IntentName")
    public static final String BROWSER_ROOT_HINTS_KEY_ROOT_CHILDREN_SUPPORTED_FLAGS =
            "androidx.media.MediaBrowserCompat.Extras.KEY_ROOT_CHILDREN_SUPPORTED_FLAGS";

    /**
     * Bundle key passed through root hints to the {@link MediaBrowserServiceCompat} to indicate the
     * recommended size, in pixels, for media art bitmaps. Much smaller images may not render well,
     * and much larger images may cause inefficient resource consumption.
     *
     * @see MediaBrowserServiceCompat#onGetRoot(String, int, Bundle)
     * @see MediaBrowserServiceCompat#getBrowserRootHints()
     * @see MediaBrowserCompat#MediaBrowserCompat(Context,ComponentName,ConnectionCallback,Bundle)
     * @see MediaDescriptionCompat#getIconUri()
     * @see MediaDescriptionCompat.Builder#setIconUri(Uri)
     * @see MediaDescriptionCompat#getIconBitmap()
     * @see MediaDescriptionCompat.Builder#setIconBitmap(Bitmap)
     */
    @SuppressLint("IntentName")
    public static final String BROWSER_ROOT_HINTS_KEY_MEDIA_ART_SIZE_PIXELS =
            "android.media.extras.MEDIA_ART_SIZE_HINT_PIXELS";

    /**
     * Bundle key sent through {@link MediaBrowserCompat#getExtras()} to the hosting {@link
     * MediaBrowserCompat} to indicate that the {@link MediaBrowserServiceCompat} supports the
     * method {@link MediaBrowserServiceCompat#onSearch(String, Bundle,
     * MediaBrowserServiceCompat.Result)}. If sent as {@code true}, the host may expose affordances
     * which call the search method.
     *
     * <p>TYPE: boolean
     *
     * @see MediaBrowserCompat#getExtras()
     * @see MediaBrowserServiceCompat.BrowserRoot#BrowserRoot(String, Bundle)
     */
    @SuppressLint("IntentName")
    public static final String BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED =
            "android.media.browse.SEARCH_SUPPORTED";

    /**
     * Bundle key passed from the {@link MediaBrowserServiceCompat} to the hosting {@link
     * MediaBrowserCompat} to indicate a preference about how playable instances of {@link
     * MediaBrowserCompat.MediaItem} are presented.
     *
     * <p>If exposed through {@link MediaBrowserCompat#getExtras()}, the preference applies to all
     * playable items within the browse tree.
     *
     * <p>If exposed through {@link MediaDescriptionCompat#getExtras()}, the preference applies to
     * only the immediate playable children of the corresponding browsable item. It takes precedence
     * over preferences sent through {@link MediaBrowserCompat#getExtras()}.
     *
     * <p>TYPE: int. Possible values are separate constants.
     *
     * @see MediaBrowserCompat#getExtras()
     * @see MediaBrowserServiceCompat.BrowserRoot#BrowserRoot(String, Bundle)
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     * @see #DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
     * @see #DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
     */
    @SuppressLint("IntentName")
    public static final String DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE =
            "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT";

    /**
     * Bundle key passed from the {@link MediaBrowserServiceCompat} to the hosting {@link
     * MediaBrowserCompat} to indicate a preference about how browsable instances of {@link
     * MediaBrowserCompat.MediaItem} are presented.
     *
     * <p>If exposed through {@link MediaBrowserCompat#getExtras()}, the preference applies to all
     * browsable items within the browse tree.
     *
     * <p>If exposed through {@link MediaDescriptionCompat#getExtras()}, the preference applies to
     * only the immediate browsable children of the corresponding browsable item. It takes
     * precedence over preferences sent through {@link MediaBrowserCompat#getExtras()}.
     *
     * <p>TYPE: int. Possible values are separate constants.
     *
     * @see MediaBrowserCompat#getExtras()
     * @see MediaBrowserServiceCompat.BrowserRoot#BrowserRoot(String, Bundle)
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     * @see #DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
     * @see #DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
     * @see #DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM
     * @see #DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM
     */
    @SuppressLint("IntentName")
    public static final String DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE =
            "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT";

    /**
     * Bundle value passed from the {@link MediaBrowserServiceCompat} to the hosting {@link
     * MediaBrowserCompat} to indicate a preference that certain instances of {@link
     * MediaBrowserCompat.MediaItem} should be presented as list items.
     *
     * @see #DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
     * @see #DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM = 1;

    /**
     * Bundle value passed from the {@link MediaBrowserServiceCompat} to the hosting {@link
     * MediaBrowserCompat} to indicate a preference that certain instances of {@link
     * MediaBrowserCompat.MediaItem} should be presented as grid items.
     *
     * @see #DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
     * @see #DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM = 2;

    /**
     * Bundle value passed from the {@link MediaBrowserServiceCompat} to the hosting {@link
     * MediaBrowserCompat} to indicate a preference that browsable instances of {@link
     * MediaBrowserCompat.MediaItem} should be presented as "category" list items. This means the
     * items provide icons that render well when they do <strong>not</strong> fill all of the
     * available area.
     *
     * @see #DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM = 3;

    /**
     * Bundle value passed from the {@link MediaBrowserServiceCompat} to the hosting {@link
     * MediaBrowserCompat} to indicate a preference that browsable instances of {@link
     * MediaBrowserCompat.MediaItem} should be presented as "category" grid items. This means the
     * items provide icons that render well when they do <strong>not</strong> fill all of the
     * available area.
     *
     * @see #DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_GRID_ITEM = 4;

    /**
     * Bundle key sent through {@link MediaDescriptionCompat#getExtras()} to the hosting {@link
     * MediaBrowserCompat} to indicate that certain instances of {@link
     * MediaBrowserCompat.MediaItem} are related as a group, with a title that is specified through
     * the bundle value. Items that are children of the same browsable node and have the same title
     * are members of the same group. The host may present a group's items as a contiguous block and
     * display the title alongside the group.
     *
     * <p>TYPE: String. Should be human readable and localized.
     *
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     */
    @SuppressLint("IntentName")
    public static final String DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE =
            "android.media.browse.CONTENT_STYLE_GROUP_TITLE_HINT";

    /**
     * Bundle key sent through {@link MediaDescriptionCompat#getExtras()} to the hosting {@link
     * MediaBrowserCompat} to indicate the playback completion status of the corresponding {@link
     * MediaBrowserCompat.MediaItem}.
     *
     * <p>TYPE: int. Possible values are separate constants.
     *
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     * @see #DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
     * @see #DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
     * @see #DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
     */
    @SuppressLint("IntentName")
    public static final String DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS =
            "android.media.extra.PLAYBACK_STATUS";

    /**
     * Bundle value sent through {@link MediaDescriptionCompat#getExtras()} to the hosting {@link
     * MediaBrowserCompat} to indicate that the corresponding {@link MediaBrowserCompat.MediaItem}
     * has not been played by the user.
     *
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     * @see #DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED = 0;

    /**
     * Bundle value sent through {@link MediaDescriptionCompat#getExtras()} to the hosting {@link
     * MediaBrowserCompat} to indicate that the corresponding {@link MediaBrowserCompat.MediaItem}
     * has been partially played by the user.
     *
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     * @see #DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED = 1;

    /**
     * Bundle value sent through {@link MediaDescriptionCompat#getExtras()} to the hosting {@link
     * MediaBrowserCompat} to indicate that the corresponding {@link MediaBrowserCompat.MediaItem}
     * has been fully played by the user.
     *
     * @see MediaDescriptionCompat#getExtras()
     * @see MediaDescriptionCompat.Builder#setExtras(Bundle)
     * @see #DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS
     */
    public static final int DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED = 2;

    /**
     * Bundle key used for the media id in {@link PlaybackStateCompat playback state} extras. It's
     * for associating the playback state with the media being played so the value is expected to be
     * same with {@link MediaMetadataCompat#METADATA_KEY_MEDIA_ID media id} of the current metadata.
     *
     * <p>TYPE: String
     *
     * @see PlaybackStateCompat#getExtras
     * @see PlaybackStateCompat.Builder#setExtras
     */
    @SuppressLint("IntentName")
    public static final String PLAYBACK_STATE_EXTRAS_KEY_MEDIA_ID =
            "androidx.media.PlaybackStateCompat.Extras.KEY_MEDIA_ID";

    /**
     * Bundle key passed through {@link PlaybackStateCompat#getExtras()} to the hosting {@link
     * MediaControllerCompat} which maps to a label. The label is associated with {@link
     * #PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT the action} that allow users to
     * resolve the current playback state error.
     *
     * <p>The label should be short; a more detailed explanation can be provided to the user via
     * {@link PlaybackStateCompat#getErrorMessage()}.
     *
     * <p>TYPE: String. Should be human readable and localized.
     *
     * @see PlaybackStateCompat#getExtras()
     * @see PlaybackStateCompat.Builder#setExtras(Bundle)
     * @see #PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT
     */
    @SuppressLint("IntentName")
    public static final String PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL =
            "android.media.extras.ERROR_RESOLUTION_ACTION_LABEL";

    /**
     * Bundle key passed through {@link PlaybackStateCompat#getExtras()} to the hosting {@link
     * MediaControllerCompat} which maps to a pending intent. When launched, the intent should allow
     * users to resolve the current playback state error. {@link
     * #PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL A label} should be included in the
     * same Bundle.
     *
     * <p>TYPE: PendingIntent. Should be inserted into the Bundle {@link
     * Bundle#putParcelable(String, Parcelable) as a Parcelable}.
     *
     * @see PlaybackStateCompat#getExtras()
     * @see PlaybackStateCompat.Builder#setExtras(Bundle)
     * @see #PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_LABEL
     */
    @SuppressLint("IntentName")
    public static final String PLAYBACK_STATE_EXTRAS_KEY_ERROR_RESOLUTION_ACTION_INTENT =
            "android.media.extras.ERROR_RESOLUTION_ACTION_INTENT";

    /**
     * Bundle key passed through the {@code extras} of
     * {@link MediaControllerCompat.TransportControls#prepareFromMediaId(String, Bundle)},
     * {@link MediaControllerCompat.TransportControls#prepareFromSearch(String, Bundle)},
     * {@link MediaControllerCompat.TransportControls#prepareFromUri(Uri, Bundle)},
     * {@link MediaControllerCompat.TransportControls#playFromMediaId(String, Bundle)},
     * {@link MediaControllerCompat.TransportControls#playFromSearch(String, Bundle)}, or
     * {@link MediaControllerCompat.TransportControls#playFromUri(Uri, Bundle)} to indicate the
     * stream type to be used by the session when playing or preparing the media.
     *
     * <p>TYPE: int
     *
     * @see MediaControllerCompat.TransportControls#prepareFromMediaId(String, Bundle)
     * @see MediaControllerCompat.TransportControls#prepareFromSearch(String, Bundle)
     * @see MediaControllerCompat.TransportControls#prepareFromUri(Uri, Bundle)
     * @see MediaControllerCompat.TransportControls#playFromMediaId(String, Bundle)
     * @see MediaControllerCompat.TransportControls#playFromSearch(String, Bundle)
     * @see MediaControllerCompat.TransportControls#playFromUri(Uri, Bundle)
     */
    @SuppressLint("IntentName")
    public static final String TRANSPORT_CONTROLS_EXTRAS_KEY_LEGACY_STREAM_TYPE =
            "android.media.session.extra.LEGACY_STREAM_TYPE";

    /**
     * Bundle key passed through the {@code extras} of
     * {@link MediaControllerCompat.TransportControls#prepareFromMediaId(String, Bundle)},
     * {@link MediaControllerCompat.TransportControls#prepareFromSearch(String, Bundle)},
     * {@link MediaControllerCompat.TransportControls#prepareFromUri(Uri, Bundle)},
     * {@link MediaControllerCompat.TransportControls#playFromMediaId(String, Bundle)},
     * {@link MediaControllerCompat.TransportControls#playFromSearch(String, Bundle)}, or
     * {@link MediaControllerCompat.TransportControls#playFromUri(Uri, Bundle)} to indicate whether
     * the session should shuffle the media to be played or not. The extra parameter is limited to
     * the current request and doesn't affect the {@link MediaSessionCompat#setShuffleMode(int)
     * shuffle mode}.
     *
     * <p>TYPE: boolean
     *
     * @see MediaControllerCompat.TransportControls#prepareFromMediaId(String, Bundle)
     * @see MediaControllerCompat.TransportControls#prepareFromSearch(String, Bundle)
     * @see MediaControllerCompat.TransportControls#prepareFromUri(Uri, Bundle)
     * @see MediaControllerCompat.TransportControls#playFromMediaId(String, Bundle)
     * @see MediaControllerCompat.TransportControls#playFromSearch(String, Bundle)
     * @see MediaControllerCompat.TransportControls#playFromUri(Uri, Bundle)
     */
    @SuppressLint("IntentName")
    public static final String TRANSPORT_CONTROLS_EXTRAS_KEY_SHUFFLE =
            "androidx.media.MediaControllerCompat.TransportControls.extras.KEY_SHUFFLE";

    private MediaConstants() {}
}
