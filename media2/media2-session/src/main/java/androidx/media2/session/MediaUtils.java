/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static android.support.v4.media.MediaDescriptionCompat.EXTRA_BT_FOLDER_TYPE;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.media2.common.MediaMetadata.BROWSABLE_TYPE_MIXED;
import static androidx.media2.common.MediaMetadata.BROWSABLE_TYPE_NONE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_ADVERTISEMENT;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_BROWSABLE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_DISPLAY_ICON;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_DOWNLOAD_STATUS;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_EXTRAS;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_MEDIA_URI;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_PLAYABLE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_TITLE;
import static androidx.media2.common.MediaMetadata.METADATA_KEY_USER_RATING;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_DESELECT_TRACK;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SELECT_TRACK;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_SPEED;
import static androidx.media2.session.SessionCommand.COMMAND_CODE_PLAYER_SET_SURFACE;
import static androidx.media2.session.SessionCommand.COMMAND_VERSION_1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.CustomAction;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;
import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.ParcelImplListSlice;
import androidx.media2.common.Rating;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaSession.CommandButton;
import androidx.versionedparcelable.ParcelImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 */
@RestrictTo(LIBRARY)
public class MediaUtils {
    public static final String TAG = "MediaUtils";
    public static final int TRANSACTION_SIZE_LIMIT_IN_BYTES = 256 * 1024; // 256KB

    // Stub BrowserRoot for accepting any connection here.
    public static final BrowserRoot sDefaultBrowserRoot =
            new BrowserRoot(MediaLibraryService.SERVICE_INTERFACE, null);

    public static final Executor DIRECT_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    // UNKNOWN version for legacy support
    public static final int VERSION_UNKNOWN = -1;

    // Initial version for all Media2 APIs.
    public static final int VERSION_0 = 0;

    // Current version for all Media2 APIs.
    public static final int CURRENT_VERSION = VERSION_0;

    private static final Map<String, String> METADATA_COMPAT_KEY_TO_METADATA_KEY = new HashMap<>();
    private static final Map<String, String> METADATA_KEY_TO_METADATA_COMPAT_KEY = new HashMap<>();
    static {
        METADATA_COMPAT_KEY_TO_METADATA_KEY.put(
                MediaMetadataCompat.METADATA_KEY_ADVERTISEMENT, METADATA_KEY_ADVERTISEMENT);
        METADATA_COMPAT_KEY_TO_METADATA_KEY.put(MediaMetadataCompat.METADATA_KEY_BT_FOLDER_TYPE,
                METADATA_KEY_BROWSABLE);
        METADATA_COMPAT_KEY_TO_METADATA_KEY.put(MediaMetadataCompat.METADATA_KEY_DOWNLOAD_STATUS,
                METADATA_KEY_DOWNLOAD_STATUS);

        // Invert METADATA_COMPAT_KEY_TO_METADATA_KEY to create METADATA_KEY_TO_METADATA_COMPAT_KEY.
        for (Map.Entry<String, String> entry : METADATA_COMPAT_KEY_TO_METADATA_KEY.entrySet()) {
            if (METADATA_KEY_TO_METADATA_COMPAT_KEY.containsKey(entry.getValue())) {
                throw new RuntimeException("Shouldn't map to the same value");
            }
            METADATA_KEY_TO_METADATA_COMPAT_KEY.put(entry.getValue(), entry.getKey());
        }
    }

    private MediaUtils() {
    }

    /**
     * Upcasts a {@link MediaItem} to the {@link MediaItem} type for pre-parceling. Note that
     * {@link MediaItem}'s subclass object cannot be parceled due to the security issue.
     *
     * @param item an item
     * @return upcasted item
     */
    @Nullable
    public static MediaItem upcastForPreparceling(@Nullable MediaItem item) {
        if (item == null || item.getClass() == MediaItem.class) {
            return item;
        }
        return new MediaItem.Builder()
                .setStartPosition(item.getStartPosition())
                .setEndPosition(item.getEndPosition())
                .setMetadata(item.getMetadata()).build();
    }

    /**
     * Upcasts a {@link VideoSize} subclass to the {@link MediaItem} type for pre-parceling.
     * Note that {@link VideoSize}'s subclass object cannot be parceled due the issue that remote
     * apps may not have the subclass.
     *
     * @param size a size
     * @return upcasted size
     */
    @Nullable
    public static VideoSize upcastForPreparceling(@Nullable VideoSize size) {
        if (size == null || size.getClass() == VideoSize.class) {
            return size;
        }
        return new VideoSize(size.getWidth(), size.getHeight());
    }

    /**
     * Upcasts a {@link TrackInfo} subclass to the {@link TrackInfo} type for pre-parceling.
     * Note that {@link TrackInfo}'s subclass object cannot be parceled due to the issue that remote
     * apps may not have the subclass.
     *
     * @param track a track
     * @return upcasted track
     */
    @Nullable
    public static TrackInfo upcastForPreparceling(@Nullable TrackInfo track) {
        if (track == null || track.getClass() == TrackInfo.class) {
            return track;
        }
        return new TrackInfo(track.getId(), track.getTrackType(), track.getFormat(),
                track.isSelectable());
    }

    /**
     * Upcasts a list of {@link TrackInfo} subclass objects to a List of {@link TrackInfo} type
     * for pre-parceling. Note that {@link TrackInfo}'s subclass object cannot be parceled due
     * to the issue that remote apps may not have the subclass.
     *
     * @param tracks a list of tracks
     * @return list of upcasted tracks
     */
    @Nullable
    public static List<TrackInfo> upcastForPreparceling(@Nullable List<TrackInfo> tracks) {
        if (tracks == null) {
            return tracks;
        }
        List<TrackInfo> upcastTracks = new ArrayList<>();
        for (int i = 0; i < tracks.size(); i++) {
            upcastTracks.add(upcastForPreparceling(tracks.get(i)));
        }
        return upcastTracks;
    }

    /**
     * Creates a {@link MediaBrowserCompat.MediaItem} from the {@link MediaItem}.
     *
     * @param item2 an item.
     * @return The newly created media item.
     */
    @Nullable
    public static MediaBrowserCompat.MediaItem convertToMediaItem(@Nullable MediaItem item2) {
        if (item2 == null) {
            return null;
        }
        int flags = 0;
        MediaDescriptionCompat descCompat;
        MediaMetadata metadata = item2.getMetadata();
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
            flags = (browsable ? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE : 0)
                    | (playable ? MediaBrowserCompat.MediaItem.FLAG_PLAYABLE : 0);
        }
        return new MediaBrowserCompat.MediaItem(descCompat, flags);
    }

    /**
     * Convert a list of {@link MediaItem} to a list of {@link MediaBrowserCompat.MediaItem}.
     */
    @Nullable
    public static List<MediaBrowserCompat.MediaItem> convertToMediaItemList(
            @Nullable List<MediaItem> items) {
        if (items == null) {
            return null;
        }
        List<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(convertToMediaItem(items.get(i)));
        }
        return result;
    }

    /**
     * Creates a {@link MediaItem} from the {@link MediaBrowserCompat.MediaItem}.
     *
     * @param item an item.
     * @return The newly created media item.
     */
    public static MediaItem convertToMediaItem(MediaBrowserCompat.MediaItem item) {
        if (item == null) {
            return null;
        }
        MediaMetadata metadata = convertToMediaMetadata(item.getDescription(),
                item.isBrowsable(), item.isPlayable());
        return new MediaItem.Builder()
                .setMetadata(metadata)
                .build();
    }

    /**
     * Convert a {@link QueueItem} to a {@link MediaItem}.
     */
    public static MediaItem convertToMediaItem(QueueItem item) {
        if (item == null) {
            return null;
        }
        // descriptionCompat cannot be null
        MediaDescriptionCompat descriptionCompat = item.getDescription();
        MediaMetadata metadata = convertToMediaMetadata(descriptionCompat, false, true);
        return new MediaItem.Builder()
                .setMetadata(metadata)
                .build();
    }

    /**
     * Convert a {@link MediaMetadataCompat} from the {@link MediaControllerCompat#getMetadata()}
     * and rating type to a {@link MediaItem}.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static MediaItem convertToMediaItem(@Nullable MediaMetadataCompat metadataCompat,
            int ratingType) {
        if (metadataCompat == null) {
            return null;
        }
        // Item is from the MediaControllerCompat, so forcefully set the playable.
        MediaMetadata.Builder builder = new MediaMetadata.Builder()
                .putLong(METADATA_KEY_PLAYABLE, 1)
                .putRating(METADATA_KEY_USER_RATING,
                        MediaUtils.convertToRating(RatingCompat.newUnratedRating(ratingType)));
        for (String key : metadataCompat.keySet()) {
            Object value = metadataCompat.getBundle().get(key);
            String metadataKey = METADATA_COMPAT_KEY_TO_METADATA_KEY.containsKey(key)
                    ? METADATA_COMPAT_KEY_TO_METADATA_KEY.get(key) : key;
            if (value instanceof CharSequence) {
                builder.putText(metadataKey, (CharSequence) value);
            } else if (value instanceof Bitmap) {
                builder.putBitmap(metadataKey, (Bitmap) value);
            } else if (value instanceof Long) {
                builder.putLong(metadataKey, (Long) value);
            } else if (value instanceof RatingCompat || value instanceof android.media.Rating) {
                // Must be fwk Rating or RatingCompat according to SDK versions.
                // Use MediaMetadataCompat#getRating(key) to get a RatingCompat object.
                try {
                    RatingCompat rating = metadataCompat.getRating(key);
                    builder.putRating(metadataKey, MediaUtils.convertToRating(rating));
                } catch (Exception e) {
                    // Prevent from CastException in the getRating() due to the future changes.
                }
            }
        }
        return new MediaItem.Builder().setMetadata(builder.build()).build();
    }

    /**
     * Convert a {@link MediaDescriptionCompat} to a {@link MediaItem}.
     */
    @Nullable
    public static MediaItem convertToMediaItem(@Nullable MediaDescriptionCompat descriptionCompat) {
        MediaMetadata metadata = convertToMediaMetadata(descriptionCompat, false, true);
        if (metadata == null) {
            return null;
        }
        return new MediaItem.Builder().setMetadata(metadata).build();
    }

    /**
     * Convert a list of {@link MediaBrowserCompat.MediaItem} to a list of {@link MediaItem}.
     */
    public static List<MediaItem> convertMediaItemListToMediaItemList(
            List<MediaBrowserCompat.MediaItem> items) {
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
     * Convert a list of {@link QueueItem} to a list of {@link MediaItem}.
     */
    public static List<MediaItem> convertQueueItemListToMediaItemList(List<QueueItem> items) {
        if (items == null) {
            return null;
        }
        List<MediaItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MediaItem item = convertToMediaItem(items.get(i));
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
     * Convert a list of {@link MediaItem} to a list of {@link QueueItem}. The index of the item
     * would be used as the queue ID to match the behavior of {@link MediaController}.
     */
    public static List<QueueItem> convertToQueueItemList(List<MediaItem> items) {
        if (items == null) {
            return null;
        }
        List<QueueItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MediaItem item = items.get(i);
            MediaDescriptionCompat description = (item.getMetadata() == null)
                    ? new MediaDescriptionCompat.Builder().setMediaId(item.getMediaId()).build()
                    : convertToMediaMetadataCompat(item.getMetadata()).getDescription();
            long id = convertToQueueItemId(i);
            result.add(new QueueItem(description, id));
        }
        return result;
    }

    /**
     * Convert the index of a {@link MediaItem} in a playlist into id of {@link QueueItem}.
     *
     * @param mediaItemIndex index of a {@link MediaItem} in a playlist. It can be
     *        {@link SessionPlayer#INVALID_ITEM_INDEX}.
     * @return id of {@link QueueItem} or {@link QueueItem#UNKNOWN_ID} if the index is
     *         {@link SessionPlayer#INVALID_ITEM_INDEX}.
     */
    public static long convertToQueueItemId(int mediaItemIndex) {
        if (mediaItemIndex == SessionPlayer.INVALID_ITEM_INDEX) {
            return QueueItem.UNKNOWN_ID;
        }
        return mediaItemIndex;
    }

    /**
     * Convert a {@link ParcelImplListSlice} to a list of {@link MediaItem}.
     */
    public static List<MediaItem> convertParcelImplListSliceToMediaItemList(
            ParcelImplListSlice listSlice) {
        if (listSlice == null) {
            return null;
        }
        List<ParcelImpl> parcelImplList = listSlice.getList();
        List<MediaItem> mediaItemList = new ArrayList<>();
        for (int i = 0; i < parcelImplList.size(); i++) {
            final ParcelImpl itemParcelImpl = parcelImplList.get(i);
            if (itemParcelImpl != null) {
                mediaItemList.add((MediaItem) MediaParcelUtils.fromParcelable(itemParcelImpl));
            }
        }
        return mediaItemList;
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
        try {
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
            return result;
        } finally {
            parcel.recycle();
        }
    }

    /**
     * Creates a {@link MediaMetadata} from the {@link MediaDescriptionCompat}.
     *
     * @param descCompat A {@link MediaDescriptionCompat} object.
     * @param browsable {@code true} if it's from {@link MediaBrowserCompat.MediaItem} with
     *                  browsable flag.
     * @param playable {@code true} if it's from {@link MediaBrowserCompat.MediaItem} with
     *                  playable flag, or from {@link QueueItem}.
     * @return
     */
    private static MediaMetadata convertToMediaMetadata(MediaDescriptionCompat descCompat,
            boolean browsable, boolean playable) {
        if (descCompat == null) {
            return null;
        }

        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        metadataBuilder.putString(METADATA_KEY_MEDIA_ID, descCompat.getMediaId());

        CharSequence title = descCompat.getTitle();
        if (title != null) {
            metadataBuilder.putText(METADATA_KEY_DISPLAY_TITLE, title);
        }

        CharSequence description = descCompat.getDescription();
        if (description != null) {
            metadataBuilder.putText(METADATA_KEY_DISPLAY_DESCRIPTION, descCompat.getDescription());
        }

        CharSequence subtitle = descCompat.getSubtitle();
        if (subtitle != null) {
            metadataBuilder.putText(METADATA_KEY_DISPLAY_SUBTITLE, subtitle);
        }

        Bitmap icon = descCompat.getIconBitmap();
        if (icon != null) {
            metadataBuilder.putBitmap(METADATA_KEY_DISPLAY_ICON, icon);
        }

        Uri iconUri = descCompat.getIconUri();
        if (iconUri != null) {
            metadataBuilder.putText(METADATA_KEY_DISPLAY_ICON_URI, iconUri.toString());
        }

        Bundle bundle = descCompat.getExtras();
        if (bundle != null) {
            metadataBuilder.setExtras(bundle);
        }

        Uri mediaUri = descCompat.getMediaUri();
        if (mediaUri != null) {
            metadataBuilder.putText(METADATA_KEY_MEDIA_URI, mediaUri.toString());
        }

        if (bundle != null && bundle.containsKey(EXTRA_BT_FOLDER_TYPE)) {
            metadataBuilder.putLong(METADATA_KEY_BROWSABLE,
                    bundle.getLong(EXTRA_BT_FOLDER_TYPE));
        } else if (browsable) {
            metadataBuilder.putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_MIXED);
        } else {
            metadataBuilder.putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE);
        }

        metadataBuilder.putLong(METADATA_KEY_PLAYABLE, playable ? 1 : 0);

        return metadataBuilder.build();
    }

    /**
     * Creates a {@link MediaMetadata} from the {@link CharSequence}.
     */
    public static MediaMetadata convertToMediaMetadata(CharSequence queueTitle) {
        if (queueTitle == null) {
            return null;
        }
        return new MediaMetadata.Builder()
                .putString(METADATA_KEY_TITLE, queueTitle.toString())
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_MIXED)
                .putLong(METADATA_KEY_PLAYABLE, 1)
                .build();
    }

    /**
     * Creates a {@link MediaMetadataCompat} from the {@link MediaMetadata}.
     *
     * @param metadata A {@link MediaMetadata} object.
     * @return The newly created {@link MediaMetadataCompat} object.
     */
    public static MediaMetadataCompat convertToMediaMetadataCompat(MediaMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        for (String key : metadata.keySet()) {
            String compatKey = METADATA_KEY_TO_METADATA_COMPAT_KEY.containsKey(key)
                    ? METADATA_KEY_TO_METADATA_COMPAT_KEY.get(key) : key;
            Object value = metadata.getObject(key);
            if (value instanceof CharSequence) {
                builder.putText(compatKey, (CharSequence) value);
            } else if (value instanceof Bitmap) {
                builder.putBitmap(compatKey, (Bitmap) value);
            } else if (value instanceof Long) {
                builder.putLong(compatKey, (Long) value);
            } else if (value instanceof Bundle && !TextUtils.equals(key, METADATA_KEY_EXTRAS)) {
                // Must be Bundle which contains a Rating.
                // Use MediaMetadata#getRating(key) to get a Rating object.
                try {
                    Rating rating = metadata.getRating(key);
                    builder.putRating(compatKey, MediaUtils.convertToRatingCompat(rating));
                } catch (Exception e) {
                    // Prevent from CastException in the getRating() due to the future changes.
                }
            }
        }
        return builder.build();
    }

    /**
     * Creates a {@link Rating} from the {@link RatingCompat}.
     *
     * @param ratingCompat A {@link RatingCompat} object.
     * @return The newly created {@link Rating} object.
     */
    public static Rating convertToRating(RatingCompat ratingCompat) {
        if (ratingCompat == null) {
            return null;
        }
        switch (ratingCompat.getRatingStyle()) {
            case RatingCompat.RATING_3_STARS:
                return ratingCompat.isRated()
                        ? new StarRating(3, ratingCompat.getStarRating()) : new StarRating(3);
            case RatingCompat.RATING_4_STARS:
                return ratingCompat.isRated()
                        ? new StarRating(4, ratingCompat.getStarRating()) : new StarRating(4);
            case RatingCompat.RATING_5_STARS:
                return ratingCompat.isRated()
                        ? new StarRating(5, ratingCompat.getStarRating()) : new StarRating(5);
            case RatingCompat.RATING_HEART:
                return ratingCompat.isRated()
                        ? new HeartRating(ratingCompat.hasHeart()) : new HeartRating();
            case RatingCompat.RATING_THUMB_UP_DOWN:
                return ratingCompat.isRated()
                        ? new ThumbRating(ratingCompat.isThumbUp()) : new ThumbRating();
            case RatingCompat.RATING_PERCENTAGE:
                return ratingCompat.isRated()
                        ? new PercentageRating(ratingCompat.getPercentRating())
                        : new PercentageRating();
            default:
                return null;
        }
    }

    /**
     * Creates a {@link RatingCompat} from the {@link Rating}.
     *
     * @param rating A {@link Rating} object.
     * @return The newly created {@link RatingCompat} object.
     */
    @SuppressLint("WrongConstant") // for @StarStyle
    public static RatingCompat convertToRatingCompat(Rating rating) {
        if (rating == null) {
            return null;
        }
        int ratingCompatStyle = getRatingCompatStyle(rating);
        if (!rating.isRated()) {
            return RatingCompat.newUnratedRating(ratingCompatStyle);
        }

        switch (ratingCompatStyle) {
            case RatingCompat.RATING_3_STARS:
            case RatingCompat.RATING_4_STARS:
            case RatingCompat.RATING_5_STARS:
                return RatingCompat.newStarRating(
                        ratingCompatStyle, ((StarRating) rating).getStarRating());
            case RatingCompat.RATING_HEART:
                return RatingCompat.newHeartRating(((HeartRating) rating).hasHeart());
            case RatingCompat.RATING_THUMB_UP_DOWN:
                return RatingCompat.newThumbRating(((ThumbRating) rating).isThumbUp());
            case RatingCompat.RATING_PERCENTAGE:
                return RatingCompat.newPercentageRating(
                        ((PercentageRating) rating).getPercentRating());
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
            parcelImplList.add(MediaParcelUtils.toParcelable(commandButton));
        }
        return parcelImplList;
    }

    /**
     * Convert a list of {@link MediaItem} to a list of {@link ParcelImplListSlice}.
     */
    public static ParcelImplListSlice convertMediaItemListToParcelImplListSlice(
            List<MediaItem> mediaItemList) {
        if (mediaItemList == null) {
            return null;
        }
        List<ParcelImpl> itemParcelableList = new ArrayList<>();
        for (int i = 0; i < mediaItemList.size(); i++) {
            final MediaItem item = mediaItemList.get(i);
            if (item != null) {
                final ParcelImpl itemParcelImpl = MediaParcelUtils.toParcelable(item);
                itemParcelableList.add(itemParcelImpl);
            }
        }
        return new ParcelImplListSlice(itemParcelableList);
    }

    /**
     * Convert a {@link SessionPlayer.PlayerState} and
     * {@link SessionPlayer.BuffState} into {@link PlaybackStateCompat.State}.
     */
    public static int convertToPlaybackStateCompatState(int playerState, int bufferingState) {
        switch (playerState) {
            case SessionPlayer.PLAYER_STATE_PLAYING:
                switch (bufferingState) {
                    case SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED:
                        return PlaybackStateCompat.STATE_BUFFERING;
                }
                return PlaybackStateCompat.STATE_PLAYING;
            case SessionPlayer.PLAYER_STATE_PAUSED:
                return PlaybackStateCompat.STATE_PAUSED;
            case SessionPlayer.PLAYER_STATE_IDLE:
                return PlaybackStateCompat.STATE_NONE;
            case SessionPlayer.PLAYER_STATE_ERROR:
                return PlaybackStateCompat.STATE_ERROR;
        }
        // For unknown value
        return PlaybackStateCompat.STATE_ERROR;
    }

    /**
     * Convert a {@link PlaybackStateCompat} into {@link SessionPlayer.PlayerState}.
     */
    public static int convertToPlayerState(PlaybackStateCompat state) {
        if (state == null) {
            return SessionPlayer.PLAYER_STATE_IDLE;
        }
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_ERROR:
                return SessionPlayer.PLAYER_STATE_ERROR;
            case PlaybackStateCompat.STATE_NONE:
                return SessionPlayer.PLAYER_STATE_IDLE;
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
            case PlaybackStateCompat.STATE_BUFFERING: // means paused for buffering.
                return SessionPlayer.PLAYER_STATE_PAUSED;
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
            case PlaybackStateCompat.STATE_PLAYING:
            case PlaybackStateCompat.STATE_REWINDING:
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
            case PlaybackStateCompat.STATE_CONNECTING: // Note: there's no perfect match for this.
                return SessionPlayer.PLAYER_STATE_PLAYING;
        }
        return SessionPlayer.PLAYER_STATE_ERROR;
    }

    /**
     * Convert a {@link PlaybackStateCompat.State} into {@link SessionPlayer.BuffState}.
     */
    // Note: there's no perfect match for this.
    public static int toBufferingState(int playbackStateCompatState) {
        switch (playbackStateCompatState) {
            case PlaybackStateCompat.STATE_BUFFERING:
                return SessionPlayer.BUFFERING_STATE_BUFFERING_AND_STARVED;
            case PlaybackStateCompat.STATE_PLAYING:
                return SessionPlayer.BUFFERING_STATE_COMPLETE;
            default:
                return SessionPlayer.BUFFERING_STATE_UNKNOWN;
        }
    }

    /**
     * Convert a {@link MediaControllerCompat.PlaybackInfo} into
     * {@link MediaController.PlaybackInfo}.
     */
    public static MediaController.PlaybackInfo toPlaybackInfo2(
            MediaControllerCompat.PlaybackInfo info) {
        return MediaController.PlaybackInfo.createPlaybackInfo(info.getPlaybackType(),
                new AudioAttributesCompat.Builder().setLegacyStreamType(
                        info.getAudioAttributes().getLegacyStreamType()).build(),
                info.getVolumeControl(), info.getMaxVolume(), info.getCurrentVolume());
    }

    /**
     * Returns whether the bundle is not parcelable.
     */
    public static boolean isUnparcelableBundle(Bundle bundle) {
        if (bundle == null) {
            return false;
        }
        bundle.setClassLoader(MediaUtils.class.getClassLoader());
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

    private static @RatingCompat.Style int getRatingCompatStyle(Rating rating) {
        if (rating instanceof HeartRating) {
            return RatingCompat.RATING_HEART;
        } else if (rating instanceof ThumbRating) {
            return RatingCompat.RATING_THUMB_UP_DOWN;
        } else if (rating instanceof StarRating) {
            switch (((StarRating) rating).getMaxStars()) {
                case 3:
                    return RatingCompat.RATING_3_STARS;
                case 4:
                    return RatingCompat.RATING_4_STARS;
                case 5:
                    return RatingCompat.RATING_5_STARS;
            }
        } else if (rating instanceof PercentageRating) {
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
    @Nullable
    public static LibraryParams convertToLibraryParams(@NonNull Context context,
            @Nullable Bundle legacyBundle) {
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
    @Nullable
    public static Bundle convertToRootHints(@Nullable LibraryParams params) {
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
    @Nullable
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
     * {@link PlaybackStateCompat} to the {@link SessionCommandGroup}.
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
    public static SessionCommandGroup convertToSessionCommandGroup(long sessionFlags,
            @Nullable PlaybackStateCompat state) {
        SessionCommandGroup.Builder commandsBuilder = new SessionCommandGroup.Builder();

        // MediaSessionCompat only support COMMAND_VERSION_1.
        commandsBuilder.addAllPlayerBasicCommands(COMMAND_VERSION_1);
        boolean includePlaylistCommands = (sessionFlags & FLAG_HANDLES_QUEUE_COMMANDS) != 0;
        if (includePlaylistCommands) {
            commandsBuilder.addAllPlayerPlaylistCommands(COMMAND_VERSION_1);
        }
        commandsBuilder.addAllVolumeCommands(COMMAND_VERSION_1);
        commandsBuilder.addAllSessionCommands(COMMAND_VERSION_1);

        commandsBuilder.removeCommand(new SessionCommand(COMMAND_CODE_PLAYER_SET_SPEED));
        commandsBuilder.removeCommand(new SessionCommand(COMMAND_CODE_PLAYER_SET_SURFACE));
        commandsBuilder.removeCommand(new SessionCommand(COMMAND_CODE_PLAYER_SELECT_TRACK));
        commandsBuilder.removeCommand(new SessionCommand(COMMAND_CODE_PLAYER_DESELECT_TRACK));

        if (state != null && state.getCustomActions() != null) {
            for (CustomAction customAction : state.getCustomActions()) {
                commandsBuilder.addCommand(
                        new SessionCommand(customAction.getAction(), customAction.getExtras()));
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
    public static List<CommandButton> convertToCustomLayout(@Nullable PlaybackStateCompat state) {
        List<CommandButton> layout = new ArrayList<>();
        if (state == null) {
            return layout;
        }
        for (CustomAction action : state.getCustomActions()) {
            CommandButton button = new CommandButton.Builder()
                    .setCommand(new SessionCommand(action.getAction(), action.getExtras()))
                    .setDisplayName(action.getName())
                    .setEnabled(true)
                    .setIconResId(action.getIcon()).build();
            layout.add(button);
        }
        return layout;
    }

    /**
     * Gets the legacy stream type from {@link androidx.media.AudioAttributesCompat}.
     *
     * @param attrs audio attributes
     * @return int legacy stream type from {@link AudioManager}
     */
    public static int getLegacyStreamType(@Nullable AudioAttributesCompat attrs) {
        int stream;
        if (attrs == null) {
            stream = AudioManager.STREAM_MUSIC;
        } else {
            stream = attrs.getLegacyStreamType();
            if (stream == AudioManager.USE_DEFAULT_STREAM_TYPE) {
                // Usually, AudioAttributesCompat#getLegacyStreamType() does not return
                // USE_DEFAULT_STREAM_TYPE unless the developer sets it with
                // AudioAttributesCompat.Builder#setLegacyStreamType().
                // But for safety, let's convert USE_DEFAULT_STREAM_TYPE to STREAM_MUSIC here.
                stream = AudioManager.STREAM_MUSIC;
            }
        }
        return stream;
    }

    @SuppressWarnings({"ParcelClassLoader", "deprecation"})
    static boolean doesBundleHaveCustomParcelable(@NonNull Bundle bundle) {
        // Try writing the bundle to parcel, and read it with framework classloader.
        Parcel parcel = Parcel.obtain();
        try {
            parcel.writeBundle(bundle);
            parcel.setDataPosition(0);
            Bundle out = parcel.readBundle(null);
            for (String key : out.keySet()) {
                // Attempt to retrieve all Bundle values with the framework class loader.
                out.get(key);
            }
            return false;
        } catch (BadParcelableException e) {
            Log.d(TAG, "Custom parcelables are not allowed", e);
            return true;
        } finally {
            parcel.recycle();
        }
    }
}
