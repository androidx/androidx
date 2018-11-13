/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import static android.support.v4.media.MediaDescriptionCompat.EXTRA_BT_FOLDER_TYPE;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

import static androidx.media2.MediaMetadata2.BROWSABLE_TYPE_MIXED;
import static androidx.media2.MediaMetadata2.BROWSABLE_TYPE_NONE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_BROWSABLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_DESCRIPTION;
import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_ICON;
import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_ICON_URI;
import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_SUBTITLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_MEDIA_ID;
import static androidx.media2.MediaMetadata2.METADATA_KEY_MEDIA_URI;
import static androidx.media2.MediaMetadata2.METADATA_KEY_PLAYABLE;
import static androidx.media2.MediaMetadata2.METADATA_KEY_TITLE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_PLAYER_SET_SPEED;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SELECT_ROUTE;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO;
import static androidx.media2.SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO;
import static androidx.media2.SessionCommand2.COMMAND_VERSION_CURRENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.CustomAction;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaSession2.CommandButton;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class MediaUtils2 {
    public static final String TAG = "MediaUtils2";
    public static final int TRANSACTION_SIZE_LIMIT_IN_BYTES = 256 * 1024; // 256KB

    // Stub BrowserRoot for accepting any connection here.
    public static final BrowserRoot sDefaultBrowserRoot =
            new BrowserRoot(MediaLibraryService2.SERVICE_INTERFACE, null);

    public static final Executor DIRECT_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    private MediaUtils2() {
    }

    /**
     * Creates a {@link MediaItem} from the {@link MediaItem2}.
     *
     * @param item2 an item.
     * @return The newly created media item.
     */
    public static MediaItem convertToMediaItem(MediaItem2 item2) {
        if (item2 == null) {
            return null;
        }
        int flags = 0;
        MediaDescriptionCompat descCompat;
        MediaMetadata2 metadata = item2.getMetadata();
        if (metadata == null) {
            descCompat = new MediaDescriptionCompat.Builder()
                    .setMediaId(item2.getMediaId())
                    .build();
        } else {
            MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                    .setMediaId(item2.getMediaId())
                    .setSubtitle(metadata.getText(METADATA_KEY_DISPLAY_SUBTITLE))
                    .setDescription(metadata.getText(METADATA_KEY_DISPLAY_DESCRIPTION))
                    .setIconBitmap(metadata.getBitmap(METADATA_KEY_DISPLAY_ICON))
                    .setExtras(metadata.getExtras());

            String title = metadata.getString(METADATA_KEY_TITLE);
            if (title != null) {
                builder.setTitle(title);
            } else {
                builder.setTitle(metadata.getString(METADATA_KEY_DISPLAY_TITLE));
            }

            String displayIconUri = metadata.getString(METADATA_KEY_DISPLAY_ICON_URI);
            if (displayIconUri != null) {
                builder.setIconUri(Uri.parse(displayIconUri));
            }

            String mediaUri = metadata.getString(METADATA_KEY_MEDIA_URI);
            if (mediaUri != null) {
                builder.setMediaUri(Uri.parse(mediaUri));
            }

            descCompat = builder.build();

            boolean browsable = metadata.containsKey(METADATA_KEY_BROWSABLE)
                    && metadata.getLong(METADATA_KEY_BROWSABLE) != BROWSABLE_TYPE_NONE;
            boolean playable = metadata.getLong(METADATA_KEY_PLAYABLE) != 0;
            flags = (browsable ? MediaItem.FLAG_BROWSABLE : 0)
                    | (playable ? MediaItem.FLAG_PLAYABLE : 0);
        }
        return new MediaItem(descCompat, flags);
    }

    /**
     * Convert a list of {@link MediaItem2} to a list of {@link MediaItem}.
     */
    public static List<MediaItem> convertToMediaItemList(List<MediaItem2> items) {
        if (items == null) {
            return null;
        }
        List<MediaItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(convertToMediaItem(items.get(i)));
        }
        return result;
    }

    /**
     * Creates a {@link MediaItem2} from the {@link MediaItem}.
     *
     * @param item an item.
     * @return The newly created media item.
     */
    public static MediaItem2 convertToMediaItem2(MediaItem item) {
        if (item == null) {
            return null;
        }
        MediaMetadata2 metadata2 = convertToMediaMetadata2(item.getDescription(),
                item.isBrowsable(), item.isPlayable());
        return new MediaItem2.Builder()
                .setMetadata(metadata2)
                .build();
    }

    /**
     * Convert a {@link QueueItem} to a {@link MediaItem2}.
     */
    public static MediaItem2 convertToMediaItem2(QueueItem item) {
        if (item == null) {
            return null;
        }
        // descriptionCompat cannot be null
        MediaDescriptionCompat descriptionCompat = item.getDescription();
        MediaMetadata2 metadata2 = convertToMediaMetadata2(descriptionCompat, false, true);
        return new MediaItem2.Builder()
                .setMetadata(metadata2)
                .build();
    }

    /**
     * Convert a {@link MediaMetadataCompat} from the {@link MediaControllerCompat#getMetadata()}
     * to a {@link MediaItem2}.
     */
    public static MediaItem2 convertToMediaItem2(MediaMetadataCompat metadataCompat) {
        if (metadataCompat == null) {
            return null;
        }
        // Item is from the MediaControllerCompat, so forcefully set the playable.
        MediaMetadata2 metadata2 = new MediaMetadata2.Builder(metadataCompat.getBundle())
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                .putLong(METADATA_KEY_PLAYABLE, 1).build();
        return new MediaItem2.Builder().setMetadata(metadata2).build();
    }

    /**
     * Convert a {@link MediaDescriptionCompat} to a {@link MediaItem2}.
     */
    public static MediaItem2 convertToMediaItem2(MediaDescriptionCompat descriptionCompat) {
        MediaMetadata2 metadata2 = convertToMediaMetadata2(descriptionCompat, false, true);
        if (metadata2 == null) {
            return null;
        }
        return new MediaItem2.Builder().setMetadata(metadata2).build();
    }

    /**
     * Convert a list of {@link MediaItem} to a list of {@link MediaItem2}.
     */
    public static List<MediaItem2> convertMediaItemListToMediaItem2List(List<MediaItem> items) {
        if (items == null) {
            return null;
        }
        List<MediaItem2> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(convertToMediaItem2(items.get(i)));
        }
        return result;
    }

    /**
     * Convert a list of {@link QueueItem} to a list of {@link MediaItem2}.
     */
    public static List<MediaItem2> convertQueueItemListToMediaItem2List(List<QueueItem> items) {
        if (items == null) {
            return null;
        }
        List<MediaItem2> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MediaItem2 item = convertToMediaItem2(items.get(i));
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Creates {@link MediaDescriptionCompat} with the id
     */
    public static MediaDescriptionCompat createMediaDescriptionCompat(String mediaId) {
        if (TextUtils.isEmpty(mediaId)) {
            return null;
        }
        return new MediaDescriptionCompat.Builder().setMediaId(mediaId).build();
    }

    /**
     * Convert a list of {@link MediaItem2} to a list of {@link QueueItem}. The index of the item
     * would be used as the queue ID to match the behavior of {@link MediaController2}.
     */
    public static List<QueueItem> convertToQueueItemList(List<MediaItem2> items) {
        if (items == null) {
            return null;
        }
        List<QueueItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MediaItem2 item = items.get(i);
            MediaDescriptionCompat description = (item.getMetadata() == null)
                    ? new MediaDescriptionCompat.Builder().setMediaId(item.getMediaId()).build()
                    : convertToMediaMetadataCompat(item.getMetadata()).getDescription();
            result.add(new QueueItem(description, i));
        }
        return result;
    }

    /**
     * Convert a {@link ParcelImplListSlice} to a list of {@link MediaItem2}.
     */
    public static List<MediaItem2> convertParcelImplListSliceToMediaItem2List(
            ParcelImplListSlice listSlice) {
        if (listSlice == null) {
            return null;
        }
        List<ParcelImpl> parcelImplList = listSlice.getList();
        List<MediaItem2> mediaItem2List = new ArrayList<>();
        for (int i = 0; i < parcelImplList.size(); i++) {
            final ParcelImpl itemParcelImpl = parcelImplList.get(i);
            if (itemParcelImpl != null) {
                mediaItem2List.add((MediaItem2) fromParcelable(itemParcelImpl));
            }
        }
        return mediaItem2List;
    }

    /**
     * Return a list which consists of first {@code N} items of the given list with the same order.
     * {@code N} is determined as the maximum number of items whose total parcelled size is less
     * than {@param sizeLimitInBytes}.
     */
    public static <T extends Parcelable> List<T> truncateListBySize(final List<T> list,
            final int sizeLimitInBytes) {
        if (list == null) {
            return null;
        }
        List<T> result = new ArrayList<>();
        Parcel parcel = Parcel.obtain();
        for (int i = 0; i < list.size(); i++) {
            // Calculate the size.
            T item = list.get(i);
            parcel.writeParcelable(item, 0);
            if (parcel.dataSize() < sizeLimitInBytes) {
                result.add(item);
            } else {
                break;
            }
        }
        parcel.recycle();
        return result;
    }

    /**
     * Creates a {@link MediaMetadata2} from the {@link MediaDescriptionCompat}.
     *
     * @param descCompat A {@link MediaDescriptionCompat} object.
     * @param browsable {@code true} if it's from {@link MediaItem} with browable flag.
     * @param playable {@code true} if it's from {@link MediaItem} with playable flag, or from
     *                 {@link QueueItem}.
     * @return
     */
    private static MediaMetadata2 convertToMediaMetadata2(MediaDescriptionCompat descCompat,
            boolean browsable, boolean playable) {
        if (descCompat == null) {
            return null;
        }

        MediaMetadata2.Builder metadata2Builder = new MediaMetadata2.Builder();
        metadata2Builder.putString(METADATA_KEY_MEDIA_ID, descCompat.getMediaId());

        CharSequence title = descCompat.getTitle();
        if (title != null) {
            metadata2Builder.putText(METADATA_KEY_DISPLAY_TITLE, title);
        }

        CharSequence description = descCompat.getDescription();
        if (description != null) {
            metadata2Builder.putText(METADATA_KEY_DISPLAY_DESCRIPTION, descCompat.getDescription());
        }

        CharSequence subtitle = descCompat.getSubtitle();
        if (subtitle != null) {
            metadata2Builder.putText(METADATA_KEY_DISPLAY_SUBTITLE, subtitle);
        }

        Bitmap icon = descCompat.getIconBitmap();
        if (icon != null) {
            metadata2Builder.putBitmap(METADATA_KEY_DISPLAY_ICON, icon);
        }

        Uri iconUri = descCompat.getIconUri();
        if (iconUri != null) {
            metadata2Builder.putText(METADATA_KEY_DISPLAY_ICON_URI, iconUri.toString());
        }

        Bundle bundle = descCompat.getExtras();
        if (bundle != null) {
            metadata2Builder.setExtras(bundle);
        }

        Uri mediaUri = descCompat.getMediaUri();
        if (mediaUri != null) {
            metadata2Builder.putText(METADATA_KEY_MEDIA_URI, mediaUri.toString());
        }

        if (bundle != null && bundle.containsKey(EXTRA_BT_FOLDER_TYPE)) {
            metadata2Builder.putLong(METADATA_KEY_BROWSABLE,
                    bundle.getLong(EXTRA_BT_FOLDER_TYPE));
        } else if (browsable) {
            metadata2Builder.putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_MIXED);
        } else {
            metadata2Builder.putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE);
        }

        metadata2Builder.putLong(METADATA_KEY_PLAYABLE, playable ? 1 : 0);

        return metadata2Builder.build();
    }

    /**
     * Creates a {@link MediaMetadata2} from the {@link CharSequence}.
     */
    public static MediaMetadata2 convertToMediaMetadata2(CharSequence queueTitle) {
        if (queueTitle == null) {
            return null;
        }
        return new MediaMetadata2.Builder()
                .putString(METADATA_KEY_TITLE, queueTitle.toString())
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_MIXED)
                .putLong(METADATA_KEY_PLAYABLE, 1)
                .build();
    }

    /**
     * Creates a {@link MediaMetadataCompat} from the {@link MediaMetadata2}.
     *
     * @param metadata2 A {@link MediaMetadata2} object.
     * @return The newly created {@link MediaMetadataCompat} object.
     */
    public static MediaMetadataCompat convertToMediaMetadataCompat(MediaMetadata2 metadata2) {
        if (metadata2 == null) {
            return null;
        }

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        for (String key : metadata2.keySet()) {
            Object value = metadata2.getObject(key);
            if (value instanceof CharSequence) {
                builder.putText(key, (CharSequence) value);
            } else if (value instanceof Rating2) {
                builder.putRating(key, convertToRatingCompat((Rating2) value));
            } else if (value instanceof Bitmap) {
                builder.putBitmap(key, (Bitmap) value);
            } else if (value instanceof Long) {
                builder.putLong(key, (Long) value);
            }
        }
        return builder.build();
    }

    /**
     * Creates a {@link Rating2} from the {@link RatingCompat}.
     *
     * @param ratingCompat A {@link RatingCompat} object.
     * @return The newly created {@link Rating2} object.
     */
    public static Rating2 convertToRating2(RatingCompat ratingCompat) {
        if (ratingCompat == null) {
            return null;
        }
        switch (ratingCompat.getRatingStyle()) {
            case RatingCompat.RATING_3_STARS:
                return ratingCompat.isRated()
                        ? new StarRating2(3, ratingCompat.getStarRating()) : new StarRating2(3);
            case RatingCompat.RATING_4_STARS:
                return ratingCompat.isRated()
                        ? new StarRating2(4, ratingCompat.getStarRating()) : new StarRating2(4);
            case RatingCompat.RATING_5_STARS:
                return ratingCompat.isRated()
                        ? new StarRating2(5, ratingCompat.getStarRating()) : new StarRating2(5);
            case RatingCompat.RATING_HEART:
                return ratingCompat.isRated()
                        ? new HeartRating2(ratingCompat.hasHeart()) : new HeartRating2();
            case RatingCompat.RATING_THUMB_UP_DOWN:
                return ratingCompat.isRated()
                        ? new ThumbRating2(ratingCompat.isThumbUp()) : new ThumbRating2();
            case RatingCompat.RATING_PERCENTAGE:
                return ratingCompat.isRated()
                        ? new PercentageRating2(ratingCompat.getPercentRating())
                        : new PercentageRating2();
            default:
                return null;
        }
    }

    /**
     * Creates a {@link RatingCompat} from the {@link Rating2}.
     *
     * @param rating2 A {@link Rating2} object.
     * @return The newly created {@link RatingCompat} object.
     */
    @SuppressLint("WrongConstant") // for @StarStyle
    public static RatingCompat convertToRatingCompat(Rating2 rating2) {
        if (rating2 == null) {
            return null;
        }
        int ratingCompatStyle = getRatingCompatStyle(rating2);
        if (!rating2.isRated()) {
            return RatingCompat.newUnratedRating(ratingCompatStyle);
        }

        switch (ratingCompatStyle) {
            case RatingCompat.RATING_3_STARS:
            case RatingCompat.RATING_4_STARS:
            case RatingCompat.RATING_5_STARS:
                return RatingCompat.newStarRating(
                        ratingCompatStyle, ((StarRating2) rating2).getStarRating());
            case RatingCompat.RATING_HEART:
                return RatingCompat.newHeartRating(((HeartRating2) rating2).hasHeart());
            case RatingCompat.RATING_THUMB_UP_DOWN:
                return RatingCompat.newThumbRating(((ThumbRating2) rating2).isThumbUp());
            case RatingCompat.RATING_PERCENTAGE:
                return RatingCompat.newPercentageRating(
                        ((PercentageRating2) rating2).getPercentRating());
            default:
                return null;
        }
    }

    /**
     * Convert a list of {@link CommandButton} to a list of {@link ParcelImpl}.
     */
    public static List<ParcelImpl> convertCommandButtonListToParcelImplList(
            List<CommandButton> commandButtonList) {
        if (commandButtonList == null) {
            return null;
        }
        List<ParcelImpl> parcelImplList = new ArrayList<>();
        for (int i = 0; i < commandButtonList.size(); i++) {
            final CommandButton commandButton = commandButtonList.get(i);
            parcelImplList.add(toParcelable(commandButton));
        }
        return parcelImplList;
    }

    /**
     * Convert a list of {@link MediaItem2} to a list of {@link ParcelImplListSlice}.
     */
    public static ParcelImplListSlice convertMediaItem2ListToParcelImplListSlice(
            List<MediaItem2> mediaItem2List) {
        if (mediaItem2List == null) {
            return null;
        }
        List<ParcelImpl> itemParcelableList = new ArrayList<>();
        for (int i = 0; i < mediaItem2List.size(); i++) {
            final MediaItem2 item = mediaItem2List.get(i);
            if (item != null) {
                final ParcelImpl itemParcelImpl = toParcelable(item);
                itemParcelableList.add(itemParcelImpl);
            }
        }
        return new ParcelImplListSlice(itemParcelableList);
    }

    /**
     * Convert a {@link SessionPlayer2.PlayerState} and
     * {@link SessionPlayer2.BuffState} into {@link PlaybackStateCompat.State}.
     */
    public static int convertToPlaybackStateCompatState(int playerState, int bufferingState) {
        switch (playerState) {
            case SessionPlayer2.PLAYER_STATE_PLAYING:
                switch (bufferingState) {
                    case SessionPlayer2.BUFFERING_STATE_BUFFERING_AND_STARVED:
                        return PlaybackStateCompat.STATE_BUFFERING;
                }
                return PlaybackStateCompat.STATE_PLAYING;
            case SessionPlayer2.PLAYER_STATE_PAUSED:
                return PlaybackStateCompat.STATE_PAUSED;
            case SessionPlayer2.PLAYER_STATE_IDLE:
                return PlaybackStateCompat.STATE_NONE;
            case SessionPlayer2.PLAYER_STATE_ERROR:
                return PlaybackStateCompat.STATE_ERROR;
        }
        // For unknown value
        return PlaybackStateCompat.STATE_ERROR;
    }

    /**
     * Convert a {@link PlaybackStateCompat} into {@link SessionPlayer2.PlayerState}.
     */
    public static int convertToPlayerState(PlaybackStateCompat state) {
        if (state == null) {
            return SessionPlayer2.PLAYER_STATE_IDLE;
        }
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_ERROR:
                return SessionPlayer2.PLAYER_STATE_ERROR;
            case PlaybackStateCompat.STATE_NONE:
                return SessionPlayer2.PLAYER_STATE_IDLE;
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
            case PlaybackStateCompat.STATE_BUFFERING: // means paused for buffering.
                return SessionPlayer2.PLAYER_STATE_PAUSED;
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
            case PlaybackStateCompat.STATE_PLAYING:
            case PlaybackStateCompat.STATE_REWINDING:
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
            case PlaybackStateCompat.STATE_CONNECTING: // Note: there's no perfect match for this.
                return SessionPlayer2.PLAYER_STATE_PLAYING;
        }
        return SessionPlayer2.PLAYER_STATE_ERROR;
    }

    /**
     * Convert a {@link PlaybackStateCompat.State} into {@link SessionPlayer2.BuffState}.
     */
    // Note: there's no perfect match for this.
    public static int toBufferingState(int playbackStateCompatState) {
        switch (playbackStateCompatState) {
            case PlaybackStateCompat.STATE_BUFFERING:
                return SessionPlayer2.BUFFERING_STATE_BUFFERING_AND_STARVED;
            case PlaybackStateCompat.STATE_PLAYING:
                return SessionPlayer2.BUFFERING_STATE_COMPLETE;
            default:
                return SessionPlayer2.BUFFERING_STATE_UNKNOWN;
        }
    }

    /**
     * Convert a {@link MediaControllerCompat.PlaybackInfo} into
     * {@link MediaController2.PlaybackInfo}.
     */
    public static MediaController2.PlaybackInfo toPlaybackInfo2(
            MediaControllerCompat.PlaybackInfo info) {
        return MediaController2.PlaybackInfo.createPlaybackInfo(info.getPlaybackType(),
                new AudioAttributesCompat.Builder()
                        .setLegacyStreamType(info.getAudioStream()).build(),
                info.getVolumeControl(), info.getMaxVolume(), info.getCurrentVolume());
    }

    /**
     * Returns whether the bundle is not parcelable.
     */
    public static boolean isUnparcelableBundle(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        bundle.setClassLoader(MediaUtils2.class.getClassLoader());
        try {
            bundle.size();
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    /**
     * Removes unparcelable bundles in the given list.
     */
    public static void keepUnparcelableBundlesOnly(final List<Bundle> bundles) {
        if (bundles == null) {
            return;
        }
        for (int i = bundles.size() - 1; i >= 0; --i) {
            Bundle bundle = bundles.get(i);
            if (isUnparcelableBundle(bundle)) {
                bundles.remove(i);
            }
        }
    }

    private static @RatingCompat.Style int getRatingCompatStyle(Rating2 rating) {
        if (rating instanceof HeartRating2) {
            return RatingCompat.RATING_HEART;
        } else if (rating instanceof ThumbRating2) {
            return RatingCompat.RATING_THUMB_UP_DOWN;
        } else if (rating instanceof StarRating2) {
            switch (((StarRating2) rating).getMaxStars()) {
                case 3:
                    return RatingCompat.RATING_3_STARS;
                case 4:
                    return RatingCompat.RATING_4_STARS;
                case 5:
                    return RatingCompat.RATING_5_STARS;
            }
        } else if (rating instanceof PercentageRating2) {
            return RatingCompat.RATING_PERCENTAGE;
        }
        return RatingCompat.RATING_NONE;
    }

    /**
     * Converts the rootHints, option, and extra to the {@link LibraryParams}.
     *
     * @param legacyBundle
     * @return new LibraryParams
     */
    public static LibraryParams convertToLibraryParams(Context context, Bundle legacyBundle) {
        if (legacyBundle == null) {
            return null;
        }
        try {
            legacyBundle.setClassLoader(context.getClassLoader());
            return new LibraryParams.Builder().setExtras(legacyBundle)
                    .setRecent(legacyBundle.getBoolean(BrowserRoot.EXTRA_RECENT))
                    .setOffline(legacyBundle.getBoolean(BrowserRoot.EXTRA_OFFLINE))
                    .setSuggested(legacyBundle.getBoolean(BrowserRoot.EXTRA_SUGGESTED))
                    .build();
        } catch (Exception e) {
            // Failure when unpacking the legacy bundle.
            return new LibraryParams.Builder().setExtras(legacyBundle).build();
        }
    }

    /**
     * Converts {@link LibraryParams} to the root hints.
     *
     * @param params
     * @return new root hints
     */
    public static Bundle convertToRootHints(LibraryParams params) {
        if (params == null) {
            return null;
        }
        Bundle rootHints = (params.getExtras() == null)
                ? new Bundle() : new Bundle(params.getExtras());
        rootHints.putBoolean(BrowserRoot.EXTRA_RECENT, params.isRecent());
        rootHints.putBoolean(BrowserRoot.EXTRA_OFFLINE, params.isOffline());
        rootHints.putBoolean(BrowserRoot.EXTRA_SUGGESTED, params.isSuggested());
        return rootHints;
    }

    /**
     * Removes all null elements from the list and returns it.
     *
     * @param list
     * @return
     */
    public static <T> List<T> removeNullElements(@Nullable List<T> list) {
        if (list == null) {
            return null;
        }
        List<T> newList = new ArrayList<>();
        for (T item : list) {
            if (item != null) {
                newList.add(item);
            }
        }
        return newList;
    }

    /**
     * Converts {@link MediaControllerCompat#getFlags() session flags} and
     * {@link PlaybackStateCompat} to the {@link SessionCommandGroup2}.
     * <p>
     * This ignores {@link PlaybackStateCompat#getActions() actions} in the
     * {@link PlaybackStateCompat} to workaround media apps' issues that they don't set playback
     * state correctly.
     *
     * @param sessionFlags session flag
     * @param state playback state
     * @return the converted session command group
     */
    @NonNull
    public static SessionCommandGroup2 convertToSessionCommandGroup(long sessionFlags,
            PlaybackStateCompat state) {
        SessionCommandGroup2.Builder commandsBuilder = new SessionCommandGroup2.Builder();
        boolean includePlaylistCommands = (sessionFlags & FLAG_HANDLES_QUEUE_COMMANDS) != 0;
        commandsBuilder.addAllPlayerCommands(COMMAND_VERSION_CURRENT, includePlaylistCommands);
        commandsBuilder.addAllVolumeCommands(COMMAND_VERSION_CURRENT);
        commandsBuilder.addAllSessionCommands(COMMAND_VERSION_CURRENT);

        commandsBuilder.removeCommand(COMMAND_CODE_PLAYER_SET_SPEED);
        commandsBuilder.removeCommand(COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO);
        commandsBuilder.removeCommand(COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO);
        commandsBuilder.removeCommand(COMMAND_CODE_SESSION_SELECT_ROUTE);

        if (state != null && state.getCustomActions() != null) {
            for (CustomAction customAction : state.getCustomActions()) {
                commandsBuilder.addCommand(
                        new SessionCommand2(customAction.getAction(), customAction.getExtras()));
            }
        }
        return commandsBuilder.build();
    }

    /**
     * Converts {@link CustomAction} in the {@link PlaybackStateCompat} to the custom layout which
     * is the list of the {@link CommandButton}.
     *
     * @param state playback state
     * @return custom layout. Always non-null.
     */
    @NonNull
    public static List<CommandButton> convertToCustomLayout(PlaybackStateCompat state) {
        List<CommandButton> layout = new ArrayList<>();
        if (state == null) {
            return layout;
        }
        for (CustomAction action : state.getCustomActions()) {
            CommandButton button = new CommandButton.Builder()
                    .setCommand(new SessionCommand2(action.getAction(), action.getExtras()))
                    .setDisplayName(action.getName())
                    .setEnabled(true)
                    .setIconResId(action.getIcon()).build();
            layout.add(button);
        }
        return layout;
    }

    /**
     * Media2 version of {@link ParcelUtils#toParcelable(VersionedParcelable)}.
     * <p>
     * This sanitizes {@link MediaItem2}'s subclass information.
     *
     * @param item
     * @return
     */
    public static ParcelImpl toParcelable(VersionedParcelable item) {
        if (item instanceof MediaItem2) {
            return new MediaItemParcelImpl((MediaItem2) item);
        }
        return (ParcelImpl) ParcelUtils.toParcelable(item);
    }

    /**
     * Media2 version of {@link ParcelUtils#fromParcelable(Parcelable)}.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T extends VersionedParcelable> T fromParcelable(ParcelImpl p) {
        return ParcelUtils.<T>fromParcelable(p);
    }

    private static class MediaItemParcelImpl extends ParcelImpl {
        private final MediaItem2 mItem;

        MediaItemParcelImpl(MediaItem2 item) {
            // Up-cast (possibly MediaItem2's subclass object) item to MediaItem2 for the
            // writeToParcel(). The copied media item will be only used when it's sent across the
            // process.
            super(new MediaItem2(item));

            // Keeps the original copy for local binder to send the original item.
            // When local binder is used (i.e. binder call happens in a single process),
            // writeToParcel() wouldn't happen for the Parcelable object and the same object will
            // be sent through the binder call.
            mItem = item;
        }

        @Override
        public MediaItem2 getVersionedParcel() {
            return mItem;
        }
    }
}
