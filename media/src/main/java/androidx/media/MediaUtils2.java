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

package androidx.media;

import static androidx.media.MediaMetadata2.METADATA_KEY_DISPLAY_DESCRIPTION;
import static androidx.media.MediaMetadata2.METADATA_KEY_DISPLAY_ICON;
import static androidx.media.MediaMetadata2.METADATA_KEY_DISPLAY_ICON_URI;
import static androidx.media.MediaMetadata2.METADATA_KEY_DISPLAY_SUBTITLE;
import static androidx.media.MediaMetadata2.METADATA_KEY_DISPLAY_TITLE;
import static androidx.media.MediaMetadata2.METADATA_KEY_EXTRAS;
import static androidx.media.MediaMetadata2.METADATA_KEY_MEDIA_ID;
import static androidx.media.MediaMetadata2.METADATA_KEY_MEDIA_URI;
import static androidx.media.MediaMetadata2.METADATA_KEY_TITLE;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.MediaSession2.CommandButton;

import java.util.ArrayList;
import java.util.List;

class MediaUtils2 {
    static final String TAG = "MediaUtils2";

    private MediaUtils2() {
    }

    /**
     * Creates a {@link MediaItem} from the {@link MediaItem2}.
     *
     * @param item2 an item.
     * @return The newly created media item.
     */
    static MediaItem createMediaItem(MediaItem2 item2) {
        if (item2 == null) {
            return null;
        }
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
        }
        return new MediaItem(descCompat, item2.getFlags());
    }

    /**
     * Creates a {@link MediaItem2} from the {@link MediaItem}.
     *
     * @param item an item.
     * @return The newly created media item.
     */
    static MediaItem2 createMediaItem2(MediaItem item) {
        if (item == null || item.getMediaId() == null) {
            return null;
        }

        MediaMetadata2 metadata2 = createMediaMetadata2(item.getDescription());
        return new MediaItem2.Builder(item.getFlags())
                .setMediaId(item.getMediaId())
                .setMetadata(metadata2)
                .build();
    }

    static List<MediaItem> fromMediaItem2List(List<MediaItem2> items) {
        if (items == null) {
            return null;
        }
        List<MediaItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(createMediaItem(items.get(i)));
        }
        return result;
    }

    static List<MediaItem2> toMediaItem2List(List<MediaItem> items) {
        if (items == null) {
            return null;
        }
        List<MediaItem2> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            result.add(createMediaItem2(items.get(i)));
        }
        return result;
    }

    /**
     * Creates a {@link MediaMetadata2} from the {@link MediaDescriptionCompat}.
     *
     * @param descCompat A {@link MediaDescriptionCompat} object.
     * @return The newly created {@link MediaMetadata2} object.
     */
    static MediaMetadata2 createMediaMetadata2(MediaDescriptionCompat descCompat) {
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
            metadata2Builder.setExtras(descCompat.getExtras());
        }

        Uri mediaUri = descCompat.getMediaUri();
        if (mediaUri != null) {
            metadata2Builder.putText(METADATA_KEY_MEDIA_URI, mediaUri.toString());
        }

        return metadata2Builder.build();
    }

    /**
     * Creates a {@link MediaMetadata2} from the {@link MediaMetadataCompat}.
     *
     * @param metadataCompat A {@link MediaMetadataCompat} object.
     * @return The newly created {@link MediaMetadata2} object.
     */
    MediaMetadata2 createMediaMetadata2(MediaMetadataCompat metadataCompat) {
        if (metadataCompat == null) {
            return null;
        }
        return new MediaMetadata2(metadataCompat.getBundle());
    }

    /**
     * Creates a {@link MediaMetadataCompat} from the {@link MediaMetadata2}.
     *
     * @param metadata2 A {@link MediaMetadata2} object.
     * @return The newly created {@link MediaMetadataCompat} object.
     */
    MediaMetadataCompat createMediaMetadataCompat(MediaMetadata2 metadata2) {
        if (metadata2 == null) {
            return null;
        }

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

        List<String> skippedKeys = new ArrayList<>();
        Bundle bundle = metadata2.toBundle();
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            if (value instanceof CharSequence) {
                builder.putText(key, (CharSequence) value);
            } else if (value instanceof Rating2) {
                builder.putRating(key, createRatingCompat((Rating2) value));
            } else if (value instanceof Bitmap) {
                builder.putBitmap(key, (Bitmap) value);
            } else if (value instanceof Long) {
                builder.putLong(key, (Long) value);
            } else {
                // There is no 'float' or 'bundle' type in MediaMetadataCompat.
                skippedKeys.add(key);
            }
        }

        MediaMetadataCompat result = builder.build();
        for (String key : skippedKeys) {
            Object value = bundle.get(key);
            if (value instanceof Float) {
                // Compatibility for MediaMetadata2.Builder.putFloat()
                result.getBundle().putFloat(key, (Float) value);
            } else if (METADATA_KEY_EXTRAS.equals(value)) {
                // Compatibility for MediaMetadata2.Builder.setExtras()
                result.getBundle().putBundle(key, (Bundle) value);
            }
        }
        return result;
    }

    /**
     * Creates a {@link Rating2} from the {@link RatingCompat}.
     *
     * @param ratingCompat A {@link RatingCompat} object.
     * @return The newly created {@link Rating2} object.
     */
    Rating2 createRating2(RatingCompat ratingCompat) {
        if (ratingCompat == null) {
            return null;
        }
        if (!ratingCompat.isRated()) {
            return Rating2.newUnratedRating(ratingCompat.getRatingStyle());
        }

        switch (ratingCompat.getRatingStyle()) {
            case RatingCompat.RATING_3_STARS:
            case RatingCompat.RATING_4_STARS:
            case RatingCompat.RATING_5_STARS:
                return Rating2.newStarRating(
                        ratingCompat.getRatingStyle(), ratingCompat.getStarRating());
            case RatingCompat.RATING_HEART:
                return Rating2.newHeartRating(ratingCompat.hasHeart());
            case RatingCompat.RATING_THUMB_UP_DOWN:
                return Rating2.newThumbRating(ratingCompat.isThumbUp());
            case RatingCompat.RATING_PERCENTAGE:
                return Rating2.newPercentageRating(ratingCompat.getPercentRating());
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
    RatingCompat createRatingCompat(Rating2 rating2) {
        if (rating2 == null) {
            return null;
        }
        if (!rating2.isRated()) {
            return RatingCompat.newUnratedRating(rating2.getRatingStyle());
        }

        switch (rating2.getRatingStyle()) {
            case Rating2.RATING_3_STARS:
            case Rating2.RATING_4_STARS:
            case Rating2.RATING_5_STARS:
                return RatingCompat.newStarRating(
                        rating2.getRatingStyle(), rating2.getStarRating());
            case Rating2.RATING_HEART:
                return RatingCompat.newHeartRating(rating2.hasHeart());
            case Rating2.RATING_THUMB_UP_DOWN:
                return RatingCompat.newThumbRating(rating2.isThumbUp());
            case Rating2.RATING_PERCENTAGE:
                return RatingCompat.newPercentageRating(rating2.getPercentRating());
            default:
                return null;
        }
    }

    static Parcelable[] toMediaItem2ParcelableArray(List<MediaItem2> playlist) {
        if (playlist == null) {
            return null;
        }
        List<Parcelable> parcelableList = new ArrayList<>();
        for (int i = 0; i < playlist.size(); i++) {
            final MediaItem2 item = playlist.get(i);
            if (item != null) {
                final Parcelable itemBundle = item.toBundle();
                if (itemBundle != null) {
                    parcelableList.add(itemBundle);
                }
            }
        }
        return parcelableList.toArray(new Parcelable[0]);
    }

    static List<MediaItem2> fromMediaItem2ParcelableArray(Parcelable[] itemParcelableList) {
        List<MediaItem2> playlist = new ArrayList<>();
        if (itemParcelableList != null) {
            for (int i = 0; i < itemParcelableList.length; i++) {
                if (!(itemParcelableList[i] instanceof Bundle)) {
                    continue;
                }
                MediaItem2 item = MediaItem2.fromBundle((Bundle) itemParcelableList[i]);
                if (item != null) {
                    playlist.add(item);
                }
            }
        }
        return playlist;
    }

    static Parcelable[] toCommandButtonParcelableArray(List<CommandButton> layout) {
        if (layout == null) {
            return null;
        }
        List<Bundle> layoutBundles = new ArrayList<>();
        for (int i = 0; i < layout.size(); i++) {
            Bundle bundle = layout.get(i).toBundle();
            if (bundle != null) {
                layoutBundles.add(bundle);
            }
        }
        return layoutBundles.toArray(new Parcelable[0]);
    }

    static List<CommandButton> fromCommandButtonParcelableArray(Parcelable[] list) {
        List<CommandButton> layout = new ArrayList<>();
        if (layout != null) {
            for (int i = 0; i < list.length; i++) {
                if (!(list[i] instanceof Bundle)) {
                    continue;
                }
                CommandButton button = CommandButton.fromBundle((Bundle) list[i]);
                if (button != null) {
                    layout.add(button);
                }
            }
        }
        return layout;
    }

    static List<Bundle> toBundleList(Parcelable[] array) {
        if (array == null) {
            return null;
        }
        List<Bundle> bundleList = new ArrayList<>();
        for (Parcelable p : array) {
            bundleList.add((Bundle) p);
        }
        return bundleList;
    }

    static int createPlaybackStateCompatState(int playerState, int bufferingState) {
        switch (playerState) {
            case MediaPlayerInterface.PLAYER_STATE_PLAYING:
                switch (bufferingState) {
                    case MediaPlayerInterface.BUFFERING_STATE_BUFFERING_AND_STARVED:
                        return PlaybackStateCompat.STATE_BUFFERING;
                }
                return PlaybackStateCompat.STATE_PLAYING;
            case MediaPlayerInterface.PLAYER_STATE_PAUSED:
                return PlaybackStateCompat.STATE_PAUSED;
            case MediaPlayerInterface.PLAYER_STATE_IDLE:
                return PlaybackStateCompat.STATE_NONE;
            case MediaPlayerInterface.PLAYER_STATE_ERROR:
                return PlaybackStateCompat.STATE_ERROR;
        }
        // For unknown value
        return PlaybackStateCompat.STATE_ERROR;
    }

    static int toPlayerState(int playbackStateCompatState) {
        switch (playbackStateCompatState) {
            case PlaybackStateCompat.STATE_ERROR:
                return MediaPlayerInterface.PLAYER_STATE_ERROR;
            case PlaybackStateCompat.STATE_NONE:
                return MediaPlayerInterface.PLAYER_STATE_IDLE;
            case PlaybackStateCompat.STATE_PAUSED:
            case PlaybackStateCompat.STATE_STOPPED:
            case PlaybackStateCompat.STATE_BUFFERING: // means paused for buffering.
                return MediaPlayerInterface.PLAYER_STATE_PAUSED;
            case PlaybackStateCompat.STATE_FAST_FORWARDING:
            case PlaybackStateCompat.STATE_PLAYING:
            case PlaybackStateCompat.STATE_REWINDING:
            case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
            case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
            case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
            case PlaybackStateCompat.STATE_CONNECTING: // Note: there's no perfect match for this.
                return MediaPlayerInterface.PLAYER_STATE_PLAYING;
        }
        return MediaPlayerInterface.PLAYER_STATE_ERROR;
    }

    static boolean isDefaultLibraryRootHint(Bundle bundle) {
        return bundle != null && bundle.getBoolean(MediaConstants2.ROOT_EXTRA_DEFAULT, false);
    }

    static Bundle createBundle(Bundle bundle) {
        return (bundle == null) ? new Bundle() : new Bundle(bundle);
    }
}
