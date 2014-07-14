/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.media.protocols;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.accessibility.CaptioningManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * Media route protocol for managing a queue of media to be played remotely
 * by a media device.
 */
public class MediaPlayerProtocol extends MediaRouteProtocol {
    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = { RESUME_STATE_UNCHANGED, RESUME_STATE_PLAY,
            RESUME_STATE_PAUSE })
    public @interface ResumeState { }

    /**
     * A resume state indicating that the player state should be left unchanged.
     */
    public static final int RESUME_STATE_UNCHANGED = 0;

    /**
     * A resume state indicating that the player should be playing,
     * regardless of its current state.
     */
    public static final int RESUME_STATE_PLAY = 1;

    /**
     * A resume state indicating that the player should be paused,
     * regardless of its current state.
     */
    public static final int RESUME_STATE_PAUSE = 2;

    /**
     * Creates the protocol client object for an application to use to send
     * messages to a media route.
     * <p>
     * This constructor is called automatically if you use
     * {@link android.media.routing.MediaRouter.ConnectionInfo#getProtocolObject getProtocolObject}
     * to obtain a protocol object from a media route connection.
     * </p>
     *
     * @param binder The remote binder supplied by the media route service.  May be
     * obtained using {@link android.media.routing.MediaRouter.ConnectionInfo#getProtocolBinder}
     * on a route connection.
     */
    public MediaPlayerProtocol(@NonNull IBinder binder) {
        super(binder);
    }

    /**
     * Loads and optionally starts playback of a new media item.
     * The media item starts playback at playPosition.
     *
     * @param mediaInfo An object describing the media item to load.
     * @param autoplay Whether playback should start immediately.
     * @param playPosition The initial playback position, in milliseconds from the
     * beginning of the stream.
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void load(@NonNull MediaInfo mediaInfo, boolean autoplay,
            long playPosition, @Nullable Bundle extras) {
        if (mediaInfo == null) {
            throw new IllegalArgumentException("mediaInfo must not be null");
        }
        Bundle args = new Bundle();
        args.putBundle("mediaInfo", mediaInfo.toBundle());
        args.putBoolean("autoplay", autoplay);
        args.putLong("playPosition", playPosition);
        args.putBundle("extras", extras);
        sendRequest("load", args);
    }

    /**
     * Begins or resumes playback of the current media item.
     *
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void play(@Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putBundle("extras", extras);
        sendRequest("play", args);
    }

    /**
     * Pauses playback of the current media item.
     *
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void pause(@Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putBundle("extras", extras);
        sendRequest("pause", args);
    }

    /**
     * Requests updated media status information from the receiver.
     *
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void requestStatus(@Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putBundle("extras", extras);
        sendRequest("requestStatus", args);
    }

    /**
     * Seeks to a new position within the current media item.
     *
     * @param position The new position, in milliseconds from the beginning of the stream.
     * @param resumeState The action to take after the seek operation has finished.
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void seek(long position, @ResumeState int resumeState, @Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putLong("position", position);
        args.putInt("resumeState", resumeState);
        args.putBundle("extras", extras);
        sendRequest("seek", args);
    }

    /**
     * Sets the active media tracks.
     *
     * @param trackIds The media track IDs.
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void setActiveMediaTracks(@NonNull long[] trackIds, @Nullable Bundle extras) {
        if (trackIds == null) {
            throw new IllegalArgumentException("trackIds must not be null");
        }
        Bundle args = new Bundle();
        args.putLongArray("trackIds", trackIds);
        args.putBundle("extras", extras);
        sendRequest("setActiveMediaTracks", args);
    }

    /**
     * Toggles the stream muting.
     *
     * @param muteState Whether the stream should be muted or unmuted.
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void setStreamMute(boolean muteState, @Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putBoolean("muteState", muteState);
        args.putBundle("extras", extras);
        sendRequest("setStreamMute", args);
    }

    /**
     * Sets the stream volume.
     * If volume is outside of the range [0.0, 1.0], then the value will be clipped.
     *
     * @param volume The new volume, in the range [0.0 - 1.0].
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void setStreamVolume(int volume, @Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putInt("volume", volume);
        args.putBundle("extras", extras);
        sendRequest("setStreamVolume", args);
    }

    /**
     * Sets the text track style.
     *
     * @param trackStyle The track style.
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void setTextTrackStyle(@NonNull TextTrackStyle trackStyle, @Nullable Bundle extras) {
        if (trackStyle == null) {
            throw new IllegalArgumentException("trackStyle must not be null");
        }
        Bundle args = new Bundle();
        args.putBundle("trackStyle", trackStyle.toBundle());
        args.putBundle("extras", extras);
        sendRequest("setTextTrackStyle", args);
    }

    /**
     * Stops playback of the current media item.
     *
     * @param extras Custom application-specific data to pass along with the request.
     */
    public void stop(@Nullable Bundle extras) {
        Bundle args = new Bundle();
        args.putBundle("extras", extras);
        sendRequest("stop", args);
    }

    /**
     * Media player callbacks.
     */
    public static abstract class Callback extends MediaRouteProtocol.Callback {
        /**
         * Called when updated player status information is received.
         *
         * @param status The updated status, or null if none.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onStatusUpdated(@Nullable MediaStatus status,
                @Nullable Bundle extras) { }

        @Override
        public void onEvent(String event, Bundle args) {
            switch (event) {
                case "statusUpdated":
                    onStatusUpdated(MediaStatus.fromBundle(args.getBundle("status")),
                            args.getBundle("extras"));
                    return;
            }
            super.onEvent(event, args);
        }
    }

    /**
     * Media player stubs.
     */
    public static abstract class Stub extends MediaRouteProtocol.Stub {
        /**
         * Creates an implementation of a media route protocol.
         *
         * @param handler The handler on which to receive requests, or null to use
         * the current looper thread.
         */
        public Stub(@Nullable Handler handler) {
            super(handler);
        }

        /**
         * Loads and optionally starts playback of a new media item.
         * The media item starts playback at playPosition.
         *
         * @param mediaInfo An object describing the media item to load.
         * @param autoplay Whether playback should start immediately.
         * @param playPosition The initial playback position, in milliseconds from the
         * beginning of the stream.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onLoad(@NonNull MediaInfo mediaInfo, boolean autoplay,
                long playPosition, @Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Begins or resumes playback of the current media item.
         *
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onPlay(@Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Pauses playback of the current media item.
         *
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onPause(@Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Requests updated media status information from the receiver.
         *
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onRequestStatus(@Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Seeks to a new position within the current media item.
         *
         * @param position The new position, in milliseconds from the beginning of the stream.
         * @param resumeState The action to take after the seek operation has finished.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onSeek(long position, @ResumeState int resumeState, @Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Sets the active media tracks.
         *
         * @param trackIds The media track IDs.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onSetActiveMediaTracks(long[] trackIds, @Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Toggles the stream muting.
         *
         * @param muteState Whether the stream should be muted or unmuted.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onSetStreamMute(boolean muteState, @Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Sets the stream volume.
         * If volume is outside of the range [0.0, 1.0], then the value will be clipped.
         *
         * @param volume The new volume, in the range [0.0 - 1.0].
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onSetStreamVolume(int volume, @Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Sets the text track style.
         *
         * @param trackStyle The track style.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onSetTextTrackStyle(@NonNull TextTrackStyle trackStyle,
                @Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Stops playback of the current media item.
         *
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void onStop(@Nullable Bundle extras) {
            throw new UnsupportedOperationException();
        }

        /**
         * Sends a status updated event.
         *
         * @param status The updated media status, or null if none.
         * @param extras Custom application-specific data to pass along with the request.
         */
        public void sendStatusUpdatedEvent(@Nullable MediaStatus status,
                @Nullable Bundle extras) {
            Bundle args = new Bundle();
            args.putBundle("status", status.toBundle());
            args.putBundle("extras", extras);
            sendEvent("statusUpdated", args);
        }

        @Override
        public void onRequest(String request, Bundle args)
                throws UnsupportedOperationException {
            switch (request) {
                case "load":
                    onLoad(MediaInfo.fromBundle(args.getBundle("mediaInfo")),
                            args.getBoolean("autoplay"), args.getLong("playPosition"),
                            args.getBundle("extras"));
                    return;
                case "play":
                    onPlay(args.getBundle("extras"));
                    return;
                case "pause":
                    onPause(args.getBundle("extras"));
                    return;
                case "requestStatus":
                    onRequestStatus(args.getBundle("extras"));
                    return;
                case "seek":
                    onSeek(args.getLong("position"), args.getInt("resumeState"),
                            args.getBundle("extras"));
                    return;
                case "setActiveMediaTracks":
                    onSetActiveMediaTracks(args.getLongArray("trackIds"), args.getBundle("extras"));
                    return;
                case "setStreamMute":
                    onSetStreamMute(args.getBoolean("muteState"), args.getBundle("extras"));
                    return;
                case "setStreamVolume":
                    onSetStreamVolume(args.getInt("volume"), args.getBundle("extras"));
                    return;
                case "setTextTrackStyle":
                    onSetTextTrackStyle(TextTrackStyle.fromBundle(args.getBundle("trackStyle")),
                            args.getBundle("extras"));
                    return;
                case "stop":
                    onStop(args.getBundle("extras"));
                    return;
            }
            super.onRequest(request, args);
        }
    }

    /**
     * A class that aggregates information about a media item.
     */
    public static final class MediaInfo {
        /** A stream type of "none". */
        public static final int STREAM_TYPE_NONE = 0;

        /** A buffered stream type. */
        public static final int STREAM_TYPE_BUFFERED = 1;

        /** A live stream type. */
        public static final int STREAM_TYPE_LIVE = 2;

        /** An invalid (unknown) stream type. */
        public static final int STREAM_TYPE_INVALID = -1;

        private static final int STREAM_TYPE_MAX = STREAM_TYPE_LIVE;

        private static final String KEY_CONTENT_ID = "contentId";
        private static final String KEY_CONTENT_TYPE = "contentType";
        private static final String KEY_EXTRAS = "extras";
        private static final String KEY_DURATION = "duration";
        private static final String KEY_METADATA = "metadata";
        private static final String KEY_STREAM_TYPE = "streamType";
        private static final String KEY_TEXT_TRACK_STYLE = "textTrackStyle";
        private static final String KEY_TRACKS = "tracks";

        private final String mContentId;
        private final int mStreamType;
        private final String mContentType;
        private MediaMetadata mMediaMetadata;
        private long mStreamDuration;
        private final ArrayList<MediaTrack> mMediaTracks = new ArrayList<MediaTrack>();
        private TextTrackStyle mTextTrackStyle;
        private Bundle mExtras;

        /**
         * Constructs a new MediaInfo with the given content ID.
         *
         * @throws IllegalArgumentException If the content ID or content type
         * is {@code null} or empty, or if the stream type is invalid.
         */
        public MediaInfo(@NonNull String contentId, int streamType,
                @NonNull String contentType) {
            if (TextUtils.isEmpty(contentId)) {
                throw new IllegalArgumentException("content ID cannot be null or empty");
            }
            if ((streamType < STREAM_TYPE_INVALID) || (streamType > STREAM_TYPE_MAX)) {
                throw new IllegalArgumentException("invalid stream type");
            }
            if (TextUtils.isEmpty(contentType)) {
                throw new IllegalArgumentException("content type cannot be null or empty");
            }
            mContentId = contentId;
            mStreamType = streamType;
            mContentType = contentType;
        }

        /**
         * Returns the content ID.
         */
        public @NonNull String getContentId() {
            return mContentId;
        }

        /**
         * Returns the stream type.
         */
        public int getStreamType() {
            return mStreamType;
        }

        /**
         * Returns the content (MIME) type.
         */
        public @NonNull String getContentType() {
            return mContentType;
        }

        /**
         * Sets the media item metadata.
         */
        public void setMetadata(@Nullable MediaMetadata metadata) {
            mMediaMetadata = metadata;
        }

        /**
         * Returns the media item metadata.
         */
        public @Nullable MediaMetadata getMetadata() {
            return mMediaMetadata;
        }

        /**
         * Sets the stream duration, in milliseconds.
         *
         * @throws IllegalArgumentException If the duration is negative.
         */
        public void setStreamDuration(long streamDuration) {
            if (streamDuration < 0) {
                throw new IllegalArgumentException("Stream duration cannot be negative");
            }
            mStreamDuration = streamDuration;
        }

        /**
         * Returns the stream duration, in milliseconds.
         */
        public long getStreamDuration() {
            return mStreamDuration;
        }

        /**
         * Sets the media tracks.
         */
        public void setMediaTracks(@NonNull List<MediaTrack> mediaTracks) {
            mMediaTracks.clear();
            mMediaTracks.addAll(mediaTracks);
        }

        /**
         * Returns the list of media tracks, or {@code null} if none have been specified.
         */
        public @NonNull List<MediaTrack> getMediaTracks() {
            return mMediaTracks;
        }

        /**
         * Sets the text track style.
         */
        public void setTextTrackStyle(@Nullable TextTrackStyle textTrackStyle) {
            mTextTrackStyle = textTrackStyle;
        }

        /**
         * Returns the text track style, or {@code null} if none has been specified.
         */
        public @Nullable TextTrackStyle getTextTrackStyle() {
            return mTextTrackStyle;
        }

        /**
         * Sets the custom application-specific data.
         */
        public void setExtras(@Nullable Bundle extras) {
            mExtras = extras;
        }

        /**
         * Returns the extras, if any.
         */
        public @Nullable Bundle getExtras() {
            return mExtras;
        }

        /**
         * Creates a bundle representation of the object.
         */
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_CONTENT_ID, mContentId);
            bundle.putInt(KEY_STREAM_TYPE, mStreamType);
            bundle.putString(KEY_CONTENT_TYPE, mContentType);
            if (mMediaMetadata != null) {
                bundle.putBundle(KEY_METADATA, mMediaMetadata.toBundle());
            }
            bundle.putLong(KEY_DURATION, mStreamDuration);
            if (mTextTrackStyle != null) {
                bundle.putBundle(KEY_TEXT_TRACK_STYLE, mTextTrackStyle.toBundle());
            }
            if (mExtras != null) {
                bundle.putBundle(KEY_EXTRAS, mExtras);
            }
            if (!mMediaTracks.isEmpty()) {
                Parcelable[] trackBundles = new Parcelable[mMediaTracks.size()];
                for (int i = 0; i < trackBundles.length; i++) {
                    trackBundles[i] = mMediaTracks.get(i).toBundle();
                }
                bundle.putParcelableArray(KEY_TRACKS, trackBundles);
            }
            return bundle;
        }

        /**
         * Constructs a new {@link MediaInfo} object from a bundle.
         */
        public static @Nullable MediaInfo fromBundle(@Nullable Bundle bundle) {
            if (bundle == null) {
                return null;
            }

            String contentId = bundle.getString(KEY_CONTENT_ID);
            int streamType = bundle.getInt(KEY_STREAM_TYPE, STREAM_TYPE_INVALID);
            String contentType = bundle.getString(KEY_CONTENT_TYPE);

            MediaInfo info = new MediaInfo(contentId, streamType, contentType);
            info.setMetadata(MediaMetadata.fromBundle(bundle.getBundle(KEY_METADATA)));
            info.setStreamDuration(bundle.getLong(KEY_DURATION));
            info.setTextTrackStyle(TextTrackStyle.fromBundle(
                    bundle.getBundle(KEY_TEXT_TRACK_STYLE)));
            info.setExtras(bundle.getBundle("extras"));

            Parcelable[] trackBundles = bundle.getParcelableArray(KEY_TRACKS);
            if (trackBundles != null) {
                for (int i = 0; i < trackBundles.length; ++i) {
                    info.mMediaTracks.add(MediaTrack.fromBundle((Bundle)trackBundles[i]));
                }
            }

            return info;
        }
    }

    /**
     * Container class for media metadata. Metadata has a media type, an optional
     * list of images, and a collection of metadata fields. Keys for common
     * metadata fields are predefined as constants, but the application is free to
     * define and use additional fields of its own.
     * <p>
     * The values of the predefined fields have predefined types. For example, a track number is
     * an <code>int</code> and a creation date is a <code>Calendar</code>. Attempting to
     * store a value of an incorrect type in a field will result in a
     * {@link IllegalArgumentException}.
     */
    public static final class MediaMetadata {
        /** A media type representing generic media content. */
        public static final int MEDIA_TYPE_GENERIC = 0;
        /** A media type representing a movie. */
        public static final int MEDIA_TYPE_MOVIE = 1;
        /** A media type representing an TV show. */
        public static final int MEDIA_TYPE_TV_SHOW = 2;
        /** A media type representing a music track. */
        public static final int MEDIA_TYPE_MUSIC_TRACK = 3;
        /** A media type representing a photo. */
        public static final int MEDIA_TYPE_PHOTO = 4;
        /** The smallest media type value that can be assigned for application-defined media types. */
        public static final int MEDIA_TYPE_USER = 100;

        // Field types.
        private static final int TYPE_NONE = 0;
        private static final int TYPE_STRING = 1;
        private static final int TYPE_INT = 2;
        private static final int TYPE_DOUBLE = 3;
        private static final int TYPE_DATE = 4;

        // Field type names. Used when constructing exceptions.
        private static final String[] sTypeNames = { null, "String", "int", "double", "Calendar" };

        private final int mMediaType;
        private final Bundle mFields;
        private final ArrayList<WebImage> mImages;

        private static final String BUNDLE_KEY_MEDIA_TYPE = "mediaType";
        private static final String BUNDLE_KEY_FIELDS = "fields";
        private static final String BUNDLE_KEY_IMAGES = "images";

        /**
         * String key: Creation date.
         * <p>
         * The value is the date and/or time at which the media was created.
         * For example, this could be the date and time at which a photograph was taken or a piece of
         * music was recorded.
         */
        public static final String KEY_CREATION_DATE =
                "android.support.media.protocols.metadata.CREATION_DATE";

        /**
         * String key: Release date.
         * <p>
         * The value is the date and/or time at which the media was released.
         * For example, this could be the date that a movie or music album was released.
         */
        public static final String KEY_RELEASE_DATE =
                "android.support.media.protocols.metadata.RELEASE_DATE";

        /**
         * String key: Broadcast date.
         * <p>
         * The value is the date and/or time at which the media was first broadcast.
         * For example, this could be the date that a TV show episode was first aired.
         */
        public static final String KEY_BROADCAST_DATE =
                "android.support.media.protocols.metadata.BROADCAST_DATE";

        /**
         * String key: Title.
         * <p>
         * The title of the media. For example, this could be the title of a song, movie, or TV show
         * episode. This value is suitable for display purposes.
         */
        public static final String KEY_TITLE =
                "android.support.media.protocols.metadata.TITLE";

        /**
         * String key: Subtitle.
         * <p>
         * The subtitle of the media. This value is suitable for display purposes.
         */
        public static final String KEY_SUBTITLE =
                "android.support.media.protocols.metadata.SUBTITLE";

        /**
         * String key: Artist.
         * <p>
         * The name of the artist who created the media. For example, this could be the name of a
         * musician, performer, or photographer. This value is suitable for display purposes.
         */
        public static final String KEY_ARTIST =
                "android.support.media.protocols.metadata.ARTIST";

        /**
         * String key: Album artist.
         * <p>
         * The name of the artist who produced an album. For example, in compilation albums such as DJ
         * mixes, the album artist is not necessarily the same as the artist(s) of the individual songs
         * on the album. This value is suitable for display purposes.
         */
        public static final String KEY_ALBUM_ARTIST =
                "android.support.media.protocols.metadata.ALBUM_ARTIST";

        /**
         * String key: Album title.
         * <p>
         * The title of the album that a music track belongs to. This value is suitable for display
         * purposes.
         */
        public static final String KEY_ALBUM_TITLE =
                "android.support.media.protocols.metadata.ALBUM_TITLE";

        /**
         * String key: Composer.
         * <p>
         * The name of the composer of a music track. This value is suitable for display purposes.
         */
        public static final String KEY_COMPOSER =
                "android.support.media.protocols.metadata.COMPOSER";

        /**
         * Integer key: Disc number.
         * <p>
         * The disc number (counting from 1) that a music track belongs to in a multi-disc album.
         */
        public static final String KEY_DISC_NUMBER =
                "android.support.media.protocols.metadata.DISC_NUMBER";

        /**
         * Integer key: Track number.
         * <p>
         * The track number of a music track on an album disc. Typically track numbers are counted
         * starting from 1, however this value may be 0 if it is a "hidden track" at the beginning of
         * an album.
         */
        public static final String KEY_TRACK_NUMBER =
                "android.support.media.protocols.metadata.TRACK_NUMBER";

        /**
         * Integer key: Season number.
         * <p>
         * The season number that a TV show episode belongs to. Typically season numbers are counted
         * starting from 1, however this value may be 0 if it is a "pilot" episode that predates the
         * official start of a TV series.
         */
        public static final String KEY_SEASON_NUMBER =
                "android.support.media.protocols.metadata.SEASON_NUMBER";

        /**
         * Integer key: Episode number.
         * <p>
         * The number of an episode in a given season of a TV show. Typically episode numbers are
         * counted starting from 1, however this value may be 0 if it is a "pilot" episode that is not
         * considered to be an official episode of the first season.
         */
        public static final String KEY_EPISODE_NUMBER =
                "android.support.media.protocols.metadata.EPISODE_NUMBER";

        /**
         * String key: Series title.
         * <p>
         * The name of a series. For example, this could be the name of a TV show or series of related
         * music albums. This value is suitable for display purposes.
         */
        public static final String KEY_SERIES_TITLE =
                "android.support.media.protocols.metadata.SERIES_TITLE";

        /**
         * String key: Studio.
         * <p>
         * The name of a recording studio that produced a piece of media. For example, this could be
         * the name of a movie studio or music label. This value is suitable for display purposes.
         */
        public static final String KEY_STUDIO =
                "android.support.media.protocols.metadata.STUDIO";

        /**
         * Integer key: Width.
         *
         * The width of a piece of media, in pixels. This would typically be used for providing the
         * dimensions of a photograph.
         */
        public static final String KEY_WIDTH =
                "android.support.media.protocols.metadata.WIDTH";

        /**
         * Integer key: Height.
         *
         * The height of a piece of media, in pixels. This would typically be used for providing the
         * dimensions of a photograph.
         */
        public static final String KEY_HEIGHT =
                "android.support.media.protocols.metadata.HEIGHT";

        /**
         * String key: Location name.
         * <p>
         * The name of a location where a piece of media was created. For example, this could be the
         * location of a photograph or the principal filming location of a movie. This value is
         * suitable for display purposes.
         */
        public static final String KEY_LOCATION_NAME =
                "android.support.media.protocols.metadata.LOCATION_NAME";

        /**
         * Double key: Location latitude.
         * <p>
         * The latitude component of the geographical location where a piece of media was created.
         * For example, this could be the location of a photograph or the principal filming location of
         * a movie.
         */
        public static final String KEY_LOCATION_LATITUDE =
                "android.support.media.protocols.metadata.LOCATION_LATITUDE";

        /**
         * Double key: Location longitude.
         * <p>
         * The longitude component of the geographical location where a piece of media was created.
         * For example, this could be the location of a photograph or the principal filming location of
         * a movie.
         */
        public static final String KEY_LOCATION_LONGITUDE =
                "android.support.media.protocols.metadata.LOCATION_LONGITUDE";

        /**
         * Constructs a new, empty, MediaMetadata with a media type of {@link #MEDIA_TYPE_GENERIC}.
         */
        public MediaMetadata() {
            this(MEDIA_TYPE_GENERIC);
        }

        /**
         * Constructs a new, empty, MediaMetadata with the given media type.
         *
         * @param mediaType The media type; one of the {@code MEDIA_TYPE_*} constants, or a value
         * greater than or equal to {@link #MEDIA_TYPE_USER} for custom media types.
         */
        public MediaMetadata(int mediaType) {
            this(mediaType, null);
        }

        private MediaMetadata(int mediaType, Bundle fields) {
            mMediaType = mediaType;
            mFields = fields != null ? fields : new Bundle();
            mImages = new ArrayList<WebImage>();
        }

        /**
         * Gets the media type.
         */
        public int getMediaType() {
            return mMediaType;
        }

        /**
         * Clears this object. The media type is left unchanged.
         */
        public void clear() {
            mFields.clear();
            mImages.clear();
        }

        /**
         * Tests if the object contains a field with the given key.
         */
        public boolean containsKey(@NonNull String key) {
            return mFields.containsKey(key);
        }

        /**
         * Returns a set of keys for all fields that are present in the object.
         */
        public @NonNull Set<String> keySet() {
            return mFields.keySet();
        }

        /**
         * Stores a value in a String field.
         *
         * @param key The key for the field.
         * @param value The new value for the field.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not a {@code String} field.
         */
        public void putString(@NonNull String key, String value) {
            throwIfWrongType(key, TYPE_STRING);
            mFields.putString(key, value);
        }

        /**
         * Reads the value of a String field.
         *
         * @return The value of the field, or {@code null} if the field has not been set.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not a {@code String} field.
         */
        public @Nullable String getString(@NonNull String key) {
            throwIfWrongType(key, TYPE_STRING);
            return mFields.getString(key);
        }

        /**
         * Stores a value in an int field.
         *
         * @param key The key for the field.
         * @param value The new value for the field.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not an {@code int} field.
         */
        public void putInt(@NonNull String key, int value) {
            throwIfWrongType(key, TYPE_INT);
            mFields.putInt(key, value);
        }

        /**
         * Reads the value of an {@code int} field.
         *
         * @return The value of the field, or {@code null} if the field has not been set.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not an {@code int} field.
         */
        public int getInt(@NonNull String key) {
            throwIfWrongType(key, TYPE_INT);
            return mFields.getInt(key);
        }

        /**
         * Stores a value in a {@code double} field.
         *
         * @param key The key for the field.
         * @param value The new value for the field.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not a {@code double} field.
         */
        public void putDouble(@NonNull String key, double value) {
            throwIfWrongType(key, TYPE_DOUBLE);
            mFields.putDouble(key, value);
        }

        /**
         * Reads the value of a {@code double} field.
         *
         * @return The value of the field, or {@code null} if the field has not been set.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not a {@code double} field.
         */
        public double getDouble(@NonNull String key) {
            throwIfWrongType(key, TYPE_DOUBLE);
            return mFields.getDouble(key);
        }

        /**
         * Stores a value in a date field.
         *
         * @param key The key for the field.
         * @param value The new value for the field.
         * @throws IllegalArgumentException If the key is {@code null} or empty or refers to a
         * predefined field which is not a date field.
         */
        public void putDate(@NonNull String key, @Nullable Calendar value) {
            throwIfWrongType(key, TYPE_DATE);
            if (value != null) {
                mFields.putLong(key, value.getTimeInMillis());
            } else {
                mFields.remove(key);
            }
        }

        /**
         * Reads the value of a date field.
         *
         * @param key The field name.
         * @return The date, as a {@link Calendar}, or {@code null} if this field has not been set.
         * @throws IllegalArgumentException If the key is {@code null} or empty or the specified field's
         * predefined type is not a date.
         */
        public @Nullable Calendar getDate(String key) {
            throwIfWrongType(key, TYPE_DATE);
            if (mFields.containsKey(key)) {
                Calendar date = Calendar.getInstance();
                date.setTimeInMillis(mFields.getLong(key));
                return date;
            }
            return null;
        }

        /**
         * Returns the list of images. If there are no images, returns an empty list.
         */
        public List<WebImage> getImages() {
            return mImages;
        }

        /**
         * Checks if the metadata includes any images.
         */
        public boolean hasImages() {
            return (mImages != null) && !mImages.isEmpty();
        }

        /**
         * Clears the list of images.
         */
        public void clearImages() {
            mImages.clear();
        }

        /**
         * Adds an image to the list of images.
         */
        public void addImage(WebImage image) {
            mImages.add(image);
        }

        /*
         * Verifies that a key is not empty, and if the key is a predefined key, verifies that it has
         * the specified type.
         */
        private void throwIfWrongType(String key, int type) {
            if (TextUtils.isEmpty(key)) {
                throw new IllegalArgumentException("null and empty keys are not allowed");
            }
            int actualType = getFieldType(key);
            if ((actualType != type) && (actualType != TYPE_NONE))
                throw new IllegalArgumentException("Value for " + key + " must be a "
                        + sTypeNames[type]);
        }

        /**
         * Creates a bundle representation of the object.
         */
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putInt(BUNDLE_KEY_MEDIA_TYPE, mMediaType);
            bundle.putBundle(BUNDLE_KEY_FIELDS, mFields);

            if (mImages.isEmpty()) {
                Parcelable[] imageBundles = new Parcelable[mImages.size()];
                for (int i = 0; i < imageBundles.length; i++) {
                    imageBundles[i] = mImages.get(i).toBundle();
                }
                bundle.putParcelableArray(BUNDLE_KEY_IMAGES, imageBundles);
            }

            return bundle;
        }

        /**
         * Constructs a new {@link MediaMetadata} object from a bundle.
         */
        public static MediaMetadata fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }

            int mediaType = bundle.getInt(BUNDLE_KEY_MEDIA_TYPE);
            Bundle fields = bundle.getBundle(BUNDLE_KEY_FIELDS);
            MediaMetadata metadata = new MediaMetadata(mediaType, fields);

            Parcelable[] imageBundles = bundle.getParcelableArray(BUNDLE_KEY_IMAGES);
            if (imageBundles != null) {
                for (Parcelable imageBundle : imageBundles) {
                    metadata.addImage(WebImage.fromBundle((Bundle)imageBundle));
                }
            }

            return metadata;
        }

        private static int getFieldType(String key) {
            switch (key) {
                case KEY_CREATION_DATE: return TYPE_DATE;
                case KEY_RELEASE_DATE: return TYPE_DATE;
                case KEY_BROADCAST_DATE: return TYPE_DATE;
                case KEY_TITLE: return TYPE_STRING;
                case KEY_SUBTITLE: return TYPE_STRING;
                case KEY_ARTIST: return TYPE_STRING;
                case KEY_ALBUM_ARTIST: return TYPE_STRING;
                case KEY_ALBUM_TITLE: return TYPE_STRING;
                case KEY_COMPOSER: return TYPE_STRING;
                case KEY_DISC_NUMBER: return TYPE_INT;
                case KEY_TRACK_NUMBER: return TYPE_INT;
                case KEY_SEASON_NUMBER: return TYPE_INT;
                case KEY_EPISODE_NUMBER: return TYPE_INT;
                case KEY_SERIES_TITLE: return TYPE_STRING;
                case KEY_STUDIO: return TYPE_STRING;
                case KEY_WIDTH: return TYPE_INT;
                case KEY_HEIGHT: return TYPE_INT;
                case KEY_LOCATION_NAME: return TYPE_STRING;
                case KEY_LOCATION_LATITUDE: return TYPE_DOUBLE;
                case KEY_LOCATION_LONGITUDE: return TYPE_DOUBLE;
                default: return TYPE_NONE;
            }
        }
    }

    /**
     * A class that holds status information about some media.
     */
    public static final class MediaStatus {
        private static final String KEY_ACTIVE_TRACK_IDS = "activeTrackIds";
        private static final String KEY_CURRENT_TIME = "currentTime";
        private static final String KEY_EXTRAS = "extras";
        private static final String KEY_IDLE_REASON = "idleReason";
        private static final String KEY_MEDIA = "media";
        private static final String KEY_MEDIA_SESSION_ID = "mediaSessionId";
        private static final String KEY_MUTED = "muted";
        private static final String KEY_PLAYBACK_RATE = "playbackRate";
        private static final String KEY_PLAYER_STATE = "playerState";
        private static final String KEY_SUPPORTED_MEDIA_COMMANDS = "supportedMediaCommands";
        private static final String KEY_VOLUME = "volume";

        /** A flag (bitmask) indicating that a media item can be paused. */
        public static final long COMMAND_PAUSE = 1 << 0;

        /** A flag (bitmask) indicating that a media item supports seeking. */
        public static final long COMMAND_SEEK = 1 << 1;

        /** A flag (bitmask) indicating that a media item's audio volume can be changed. */
        public static final long COMMAND_SET_VOLUME = 1 << 2;

        /** A flag (bitmask) indicating that a media item's audio can be muted. */
        public static final long COMMAND_TOGGLE_MUTE = 1 << 3;

        /** A flag (bitmask) indicating that a media item supports skipping forward. */
        public static final long COMMAND_SKIP_FORWARD = 1 << 4;

        /** A flag (bitmask) indicating that a media item supports skipping backward. */
        public static final long COMMAND_SKIP_BACKWARD = 1 << 5;

        /** Constant indicating unknown player state. */
        public static final int PLAYER_STATE_UNKNOWN = 0;

        /** Constant indicating that the media player is idle. */
        public static final int PLAYER_STATE_IDLE = 1;

        /** Constant indicating that the media player is playing. */
        public static final int PLAYER_STATE_PLAYING = 2;

        /** Constant indicating that the media player is paused. */
        public static final int PLAYER_STATE_PAUSED = 3;

        /** Constant indicating that the media player is buffering. */
        public static final int PLAYER_STATE_BUFFERING = 4;

        /** Constant indicating that the player currently has no idle reason. */
        public static final int IDLE_REASON_NONE = 0;

        /** Constant indicating that the player is idle because playback has finished. */
        public static final int IDLE_REASON_FINISHED = 1;

        /**
         * Constant indicating that the player is idle because playback has been canceled in
         * response to a STOP command.
         */
        public static final int IDLE_REASON_CANCELED = 2;

        /**
         * Constant indicating that the player is idle because playback has been interrupted by
         * a LOAD command.
         */
        public static final int IDLE_REASON_INTERRUPTED = 3;

        /** Constant indicating that the player is idle because a playback error has occurred. */
        public static final int IDLE_REASON_ERROR = 4;

        private final long mMediaSessionId;
        private final MediaInfo mMediaInfo;
        private double mPlaybackRate;
        private int mPlayerState;
        private int mIdleReason;
        private long mStreamPosition;
        private long mSupportedMediaCommands;
        private double mVolume;
        private boolean mMuteState;
        private long mActiveTrackIds[];
        private Bundle mExtras;

        /**
         * Constructs a new {@link MediaStatus} object with the given properties.
         */
        public MediaStatus(long mediaSessionId, @NonNull MediaInfo mediaInfo) {
            if (mediaInfo == null) {
                throw new IllegalArgumentException("mediaInfo must not be null");
            }

            mMediaSessionId = mediaSessionId;
            mMediaInfo = mediaInfo;
            mPlayerState = PLAYER_STATE_UNKNOWN;
            mIdleReason = IDLE_REASON_NONE;
        }

        /**
         * Returns the media session ID for this item.
         */
        public long getMediaSessionId() {
            return mMediaSessionId;
        }

        /**
         * Returns the {@link MediaInfo} for this item.
         */
        public @NonNull MediaInfo getMediaInfo() {
            return mMediaInfo;
        }

        /**
         * Gets the current media player state.
         */
        public int getPlayerState() {
            return mPlayerState;
        }

        /**
         * Sets the current media player state.
         */
        public void setPlayerState(int playerState) {
            mPlayerState = playerState;
        }

        /**
         * Gets the player state idle reason. This value is only meaningful if the player state is
         * in fact {@link #PLAYER_STATE_IDLE}.
         */
        public int getIdleReason() {
            return mIdleReason;
        }

        /**
         * Sets the player state idle reason. This value is only meaningful if the player state is
         * in fact {@link #PLAYER_STATE_IDLE}.
         */
        public void setIdleReason(int idleReason) {
            mIdleReason = idleReason;
        }

        /**
         * Gets the current stream playback rate. This will be negative if the stream is seeking
         * backwards, 0 if the stream is paused, 1 if the stream is playing normally, and some other
         * positive value if the stream is seeking forwards.
         */
        public double getPlaybackRate() {
            return mPlaybackRate;
        }

        /**
         * Sets the current stream playback rate. This will be negative if the stream is seeking
         * backwards, 0 if the stream is paused, 1 if the stream is playing normally, and some other
         * positive value if the stream is seeking forwards.
         */
        public void setPlaybackRate(double playbackRate) {
            mPlaybackRate = playbackRate;
        }

        /**
         * Returns the current stream position, in milliseconds.
         */
        public long getStreamPosition() {
            return mStreamPosition;
        }

        /**
         * Sets the current stream position, in milliseconds.
         */
        public void setStreamPosition(long streamPosition) {
            mStreamPosition = streamPosition;
        }

        /**
         * Tests if the stream supports a given control command.
         *
         * @param mediaCommand The media command.
         * @return {@code true} if the command is supported, {@code false} otherwise.
         */
        public boolean isMediaCommandSupported(long mediaCommand) {
            return (mSupportedMediaCommands & mediaCommand) != 0;
        }

        /**
         * Sets whether the stream supports a given control command.
         */
        public void setSupportedMediaCommands(long supportedMediaCommands) {
            mSupportedMediaCommands = supportedMediaCommands;
        }

        /**
         * Returns the stream's volume.
         */
        public double getStreamVolume() {
            return mVolume;
        }

        /**
         * Sets the stream's volume.
         */
        public void setStreamVolume(double volume) {
            mVolume = volume;
        }

        /**
         * Returns the stream's mute state.
         */
        public boolean isMute() {
            return mMuteState;
        }

        /**
         * Sets the stream's mute state.
         */
        public void setMute(boolean muteState) {
            mMuteState = muteState;
        }

        /**
         * Returns the list of active track IDs, if any, otherwise {@code null}.
         */
        public @Nullable long[] getActiveTrackIds() {
            return mActiveTrackIds;
        }

        /**
         * Sets the list of active track IDs, if any, otherwise {@code null}.
         */
        public void setActiveTrackIds(@Nullable long[] trackIds) {
            mActiveTrackIds = trackIds;
        }

        /**
         * Returns any extras that are is associated with the media item.
         */
        public @Nullable Bundle getExtras() {
            return mExtras;
        }

        /**
         * Sets any extras that are associated with the media item.
         */
        public void setExtras(@Nullable Bundle extras) {
            mExtras = extras;
        }

        /**
         * Creates a bundle representation of the object.
         */
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putLong(KEY_MEDIA_SESSION_ID, mMediaSessionId);
            bundle.putBundle(KEY_MEDIA, mMediaInfo.toBundle());
            bundle.putLongArray(KEY_ACTIVE_TRACK_IDS, mActiveTrackIds);
            bundle.putLong(KEY_CURRENT_TIME, mStreamPosition);
            bundle.putInt(KEY_IDLE_REASON, mIdleReason);
            bundle.putBoolean(KEY_MUTED, mMuteState);
            bundle.putDouble(KEY_PLAYBACK_RATE, mPlaybackRate);
            bundle.putInt(KEY_PLAYER_STATE, mPlayerState);
            bundle.putLong(KEY_SUPPORTED_MEDIA_COMMANDS, mSupportedMediaCommands);
            bundle.putDouble(KEY_VOLUME, mVolume);
            return bundle;
        }

        /**
         * Constructs a new {@link MediaStatus} object from a bundle.
         */
        public static MediaStatus fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }

            long mediaSessionId = bundle.getLong(KEY_MEDIA_SESSION_ID);
            MediaInfo mediaInfo = MediaInfo.fromBundle(bundle.getBundle(KEY_MEDIA));
            MediaStatus status = new MediaStatus(mediaSessionId, mediaInfo);

            status.setActiveTrackIds(bundle.getLongArray(KEY_ACTIVE_TRACK_IDS));
            status.setStreamPosition(bundle.getLong(KEY_CURRENT_TIME));
            status.setIdleReason(bundle.getInt(KEY_IDLE_REASON));
            status.setMute(bundle.getBoolean(KEY_MUTED));
            status.setPlaybackRate(bundle.getDouble(KEY_PLAYBACK_RATE));
            status.setPlayerState(bundle.getInt(KEY_PLAYER_STATE));
            status.setSupportedMediaCommands(bundle.getLong(KEY_SUPPORTED_MEDIA_COMMANDS));
            status.setStreamVolume(bundle.getDouble(KEY_VOLUME));
            status.setExtras(bundle.getBundle(KEY_EXTRAS));
            return status;
        }
    }

    /**
     * A class that represents a media track, such as a language track or closed caption text track
     * in a video.
     */
    public static final class MediaTrack {
        private static final String KEY_TRACK_ID = "trackId";
        private static final String KEY_TYPE = "type";
        private static final String KEY_TRACK_CONTENT_ID = "trackContentId";
        private static final String KEY_TRACK_CONTENT_TYPE = "trackContentType";
        private static final String KEY_NAME = "name";
        private static final String KEY_LANGUAGE = "language";
        private static final String KEY_SUBTYPE = "subtype";
        private static final String KEY_EXTRAS = "extras";

        /** A media track type indicating an unknown track type. */
        public static final int TYPE_UNKNOWN = 0;
        /** A media track type indicating a text track. */
        public static final int TYPE_TEXT = 1;
        /** A media track type indicating an audio track. */
        public static final int TYPE_AUDIO = 2;
        /** A media track type indicating a video track. */
        public static final int TYPE_VIDEO = 3;

        /** A media track subtype indicating an unknown subtype. */
        public static final int SUBTYPE_UNKNOWN = -1;
        /** A media track subtype indicating no subtype. */
        public static final int SUBTYPE_NONE = 0;
        /** A media track subtype indicating subtitles. */
        public static final int SUBTYPE_SUBTITLES = 1;
        /** A media track subtype indicating closed captions. */
        public static final int SUBTYPE_CAPTIONS = 2;
        /** A media track subtype indicating descriptions. */
        public static final int SUBTYPE_DESCRIPTIONS = 3;
        /** A media track subtype indicating chapters. */
        public static final int SUBTYPE_CHAPTERS = 4;
        /** A media track subtype indicating metadata. */
        public static final int SUBTYPE_METADATA = 5;

        private long mId;
        private int mType;
        private String mContentId;
        private String mContentType;
        private String mName;
        private String mLanguage;
        private int mSubtype;
        private Bundle mExtras;

        /**
         * Constructs a new track with the given track ID and type.
         *
         * @throws IllegalArgumentException If the track type is invalid.
         */
        public MediaTrack(long id, int type) {
            clear();
            mId = id;
            if ((type <= TYPE_UNKNOWN) || (type > TYPE_VIDEO)) {
                throw new IllegalArgumentException("invalid type " + type);
            }
            mType = type;
        }

        /**
         * Returns the unique ID of the media track.
         */
        public long getId() {
            return mId;
        }

        /**
         * Returns the type of the track; one of the {@code TYPE_} constants defined above.
         */
        public int getType() {
            return mType;
        }

        /**
         * Returns the content ID of the media track.
         */
        public String getContentId() {
            return mContentId;
        }

        /**
         * Sets the content ID for the media track.
         */
        public void setContentId(String contentId) {
            mContentId = contentId;
        }

        /**
         * Returns the content type (MIME type) of the media track, or {@code null} if none was
         * specified.
         */
        public String getContentType() {
            return mContentType;
        }

        /**
         * Sets the content type (MIME type) of the media track.
         */
        public void setContentType(String contentType) {
            mContentType = contentType;
        }

        /**
         * Returns the name of the media track, or {@code null} if none was specified.
         */
        public String getName() {
            return mName;
        }

        /**
         * Sets the track name.
         */
        public void setName(String name) {
            mName = name;
        }

        /**
         * Returns the language of this media track, or {@code null} if none was specified.
         */
        public String getLanguage() {
            return mLanguage;
        }

        /**
         * Sets the track language.
         */
        public void setLanguage(String language) {
            mLanguage = language;
        }

        /**
         * Returns the subtype of this media track; one of the {@code SUBTYPE_}
         * constants defined above.
         */
        public int getSubtype() {
            return mSubtype;
        }

        /**
         * Sets the track subtype.
         *
         * @throws IllegalArgumentException If the subtype is invalid.
         */
        public void setSubtype(int subtype) {
            if ((subtype <= SUBTYPE_UNKNOWN) || (subtype > SUBTYPE_METADATA)) {
                throw new IllegalArgumentException("invalid subtype " + subtype);
            }
            if ((subtype != SUBTYPE_NONE) && (mType != TYPE_TEXT)) {
                throw new IllegalArgumentException("subtypes are only valid for text tracks");
            }

            mSubtype = subtype;
        }

        /**
         * Returns the extras object for this media track, or {@code null} if none was
         * specified.
         */
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Sets the track's extras object.
         */
        public void setExtras(Bundle extras) {
            mExtras = extras;
        }

        private void clear() {
            mId = 0;
            mType = TYPE_UNKNOWN;
            mContentId = null;
            mName = null;
            mLanguage = null;
            mSubtype = SUBTYPE_UNKNOWN;
            mExtras = null;
        }

        /**
         * Creates a bundle representation of the object.
         */
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putLong(KEY_TRACK_ID, mId);
            bundle.putInt(KEY_TYPE, mType);
            bundle.putString(KEY_TRACK_CONTENT_ID, mContentId);
            bundle.putString(KEY_TRACK_CONTENT_TYPE, mContentType);
            bundle.putString(KEY_NAME, mName);
            bundle.putString(KEY_LANGUAGE, mLanguage);
            bundle.putInt(KEY_SUBTYPE, mSubtype);
            bundle.putBundle(KEY_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Constructs a new {@link MediaTrack} object from a bundle.
         */
        public static MediaTrack fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }

            long trackId = bundle.getLong(KEY_TRACK_ID);
            int type = bundle.getInt(KEY_TYPE);
            MediaTrack track = new MediaTrack(trackId, type);

            track.setContentId(bundle.getString(KEY_TRACK_CONTENT_ID));
            track.setContentType(bundle.getString(KEY_TRACK_CONTENT_TYPE));
            track.setName(bundle.getString(KEY_NAME));
            track.setLanguage(bundle.getString(KEY_LANGUAGE));
            track.setSubtype(bundle.getInt(KEY_SUBTYPE));
            track.setExtras(bundle.getBundle(KEY_EXTRAS));
            return track;
        }
    }

    /**
     * A class that specifies how a text track's text will be displayed on-screen. The text is
     * displayed inside a rectangular "window". The appearance of both the text and the window are
     * configurable.
     * <p>
     * With the exception of the font scale, which has a predefined default value, any attribute that
     * is not explicitly set will remain "unspecified", and the player will select an appropriate
     * value.
     */
    public static final class TextTrackStyle {
        /** The default font scale. */
        public static final float DEFAULT_FONT_SCALE = 1.0f;

        /** A color value that indicates an unspecified (unset) color. */
        public static final int COLOR_UNSPECIFIED = 0;

        /** An edge type indicating an unspecified edge type. */
        public static final int EDGE_TYPE_UNSPECIFIED = -1;
        /** An edge type indicating no edge. */
        public static final int EDGE_TYPE_NONE = 0;
        /** An edge type indicating an outline edge. */
        public static final int EDGE_TYPE_OUTLINE = 1;
        /** An edge type indicating a drop shadow edge. */
        public static final int EDGE_TYPE_DROP_SHADOW = 2;
        /** An edge type indicating a raised edge. */
        public static final int EDGE_TYPE_RAISED = 3;
        /** An edge type indicating a depressed edge. */
        public static final int EDGE_TYPE_DEPRESSED = 4;

        /** A window type indicating an unspecified window type. */
        public static final int WINDOW_TYPE_UNSPECIFIED = -1;
        /** A window type indicating no window type. */
        public static final int WINDOW_TYPE_NONE = 0;
        /** A window type indicating a normal window. */
        public static final int WINDOW_TYPE_NORMAL = 1;
        /** A window type indicating a window with rounded corners. */
        public static final int WINDOW_TYPE_ROUNDED = 2;

        /** A font family indicating an unspecified font family. */
        public static final int FONT_FAMILY_UNSPECIFIED = -1;
        /** A font family indicating Sans Serif. */
        public static final int FONT_FAMILY_SANS_SERIF = 0;
        /** A font family indicating Monospaced Sans Serif. */
        public static final int FONT_FAMILY_MONOSPACED_SANS_SERIF = 1;
        /** A font family indicating Serif. */
        public static final int FONT_FAMILY_SERIF = 2;
        /** A font family indicating Monospaced Serif. */
        public static final int FONT_FAMILY_MONOSPACED_SERIF = 3;
        /** A font family indicating Casual. */
        public static final int FONT_FAMILY_CASUAL = 4;
        /** A font family indicating Cursive. */
        public static final int FONT_FAMILY_CURSIVE = 5;
        /** A font family indicating Small Capitals. */
        public static final int FONT_FAMILY_SMALL_CAPITALS = 6;

        /** A font style indicating an unspecified style. */
        public static final int FONT_STYLE_UNSPECIFIED = -1;
        /** A font style indicating a normal style. */
        public static final int FONT_STYLE_NORMAL = 0;
        /** A font style indicating a bold style. */
        public static final int FONT_STYLE_BOLD = 1;
        /** A font style indicating an italic style. */
        public static final int FONT_STYLE_ITALIC = 2;
        /** A font style indicating a bold and italic style. */
        public static final int FONT_STYLE_BOLD_ITALIC = 3;

        private static final String KEY_FONT_SCALE = "fontScale";
        private static final String KEY_FOREGROUND_COLOR = "foregroundColor";
        private static final String KEY_BACKGROUND_COLOR = "backgroundColor";
        private static final String KEY_EDGE_TYPE = "edgeType";
        private static final String KEY_EDGE_COLOR = "edgeColor";
        private static final String KEY_WINDOW_TYPE = "windowType";
        private static final String KEY_WINDOW_COLOR = "windowColor";
        private static final String KEY_WINDOW_CORNER_RADIUS = "windowRoundedCornerRadius";
        private static final String KEY_FONT_FAMILY = "fontFamily";
        private static final String KEY_FONT_GENERIC_FAMILY = "fontGenericFamily";
        private static final String KEY_FONT_STYLE = "fontStyle";
        private static final String KEY_EXTRAS = "extras";

        private float mFontScale;
        private int mForegroundColor;
        private int mBackgroundColor;
        private int mEdgeType;
        private int mEdgeColor;
        private int mWindowType;
        private int mWindowColor;
        private int mWindowCornerRadius;
        private String mFontFamily;
        private int mFontGenericFamily;
        private int mFontStyle;
        private Bundle mExtras;

        /**
         * Constructs a new TextTrackStyle.
         */
        public TextTrackStyle() {
            clear();
        }

        /**
         * Sets the font scale factor. The default is {@link #DEFAULT_FONT_SCALE}.
         */
        public void setFontScale(float fontScale) {
            mFontScale = fontScale;
        }

        /**
         * Gets the font scale factor.
         */
        public float getFontScale() {
            return mFontScale;
        }

        /**
         * Sets the text's foreground color.
         *
         * @param foregroundColor The color, as an ARGB value.
         */
        public void setForegroundColor(int foregroundColor) {
            mForegroundColor = foregroundColor;
        }

        /**
         * Gets the text's foreground color.
         */
        public int getForegroundColor() {
            return mForegroundColor;
        }

        /**
         * Sets the text's background color.
         *
         * @param backgroundColor The color, as an ARGB value.
         */
        public void setBackgroundColor(int backgroundColor) {
            mBackgroundColor = backgroundColor;
        }

        /**
         * Gets the text's background color.
         */
        public int getBackgroundColor() {
            return mBackgroundColor;
        }

        /**
         * Sets the caption window's edge type.
         *
         * @param edgeType The edge type; one of the {@code EDGE_TYPE_} constants defined above.
         */
        public void setEdgeType(int edgeType) {
            if ((edgeType < EDGE_TYPE_NONE) || (edgeType > EDGE_TYPE_DEPRESSED)) {
                throw new IllegalArgumentException("invalid edgeType");
            }
            mEdgeType = edgeType;
        }

        /**
         * Gets the caption window's edge type.
         */
        public int getEdgeType() {
            return mEdgeType;
        }

        /**
         * Sets the window's edge color.
         *
         * @param edgeColor The color, as an ARGB value.
         */
        public void setEdgeColor(int edgeColor) {
            mEdgeColor = edgeColor;
        }

        /**
         * Gets the window's edge color.
         */
        public int getEdgeColor() {
            return mEdgeColor;
        }

        /**
         * Sets the window type.
         *
         * @param windowType The window type; one of the {@code WINDOW_TYPE_} constants defined above.
         */
        public void setWindowType(int windowType) {
            if ((windowType < WINDOW_TYPE_NONE) || (windowType > WINDOW_TYPE_ROUNDED)) {
                throw new IllegalArgumentException("invalid windowType");
            }
            mWindowType = windowType;
        }

        /**
         * Gets the caption window type.
         */
        public int getWindowType() {
            return mWindowType;
        }

        /**
         * Sets the window's color.
         *
         * @param windowColor The color, as an ARGB value.
         */
        public void setWindowColor(int windowColor) {
            mWindowColor = windowColor;
        }

        /**
         * Gets the window's color.
         */
        public int getWindowColor() {
            return mWindowColor;
        }

        /**
         * If the window type is {@link #WINDOW_TYPE_ROUNDED}, sets the radius for the window's
         * corners.
         *
         * @param windowCornerRadius The radius, in pixels. Must be a positive value.
         */
        public void setWindowCornerRadius(int windowCornerRadius) {
            if (windowCornerRadius < 0) {
                throw new IllegalArgumentException("invalid windowCornerRadius");
            }
            mWindowCornerRadius = windowCornerRadius;
        }

        /**
         * Gets the window corner radius.
         */
        public int getWindowCornerRadius() {
            return mWindowCornerRadius;
        }

        /**
         * Sets the text's font family.
         *
         * @param fontFamily The text font family.
         */
        public void setFontFamily(String fontFamily) {
            mFontFamily = fontFamily;
        }

        /**
         * Gets the text's font family.
         */
        public String getFontFamily() {
            return mFontFamily;
        }

        /**
         * Sets the text's generic font family. This will be used if the font family specified with
         * {@link #setFontFamily} (if any) is unavailable.
         *
         * @param fontGenericFamily The generic family; one of the {@code FONT_FAMILY_} constants
         * defined above.
         */
        public void setFontGenericFamily(int fontGenericFamily) {
            if ((fontGenericFamily < FONT_FAMILY_SANS_SERIF)
                    || (fontGenericFamily > FONT_FAMILY_SMALL_CAPITALS)) {
                throw new IllegalArgumentException("invalid fontGenericFamily");
            }
            mFontGenericFamily = fontGenericFamily;
        }

        /**
         * Gets the text's generic font family.
         */
        public int getFontGenericFamily() {
            return mFontGenericFamily;
        }

        /**
         * Sets the text font style.
         *
         * @param fontStyle The font style; one of the {@code FONT_STYLE_} constants defined above.
         */
        public void setFontStyle(int fontStyle) {
            if ((fontStyle < FONT_STYLE_NORMAL) || (fontStyle > FONT_STYLE_BOLD_ITALIC)) {
                throw new IllegalArgumentException("invalid fontStyle");
            }
            mFontStyle = fontStyle;
        }

        /**
         * Gets the text font style.
         */
        public int getFontStyle() {
            return mFontStyle;
        }

        /**
         * Sets the extras object.
         */
        public void setExtras(Bundle extras) {
            mExtras = extras;
        }

        /**
         * Gets the extras object.
         */
        public Bundle getExtras() {
            return mExtras;
        }

        private void clear() {
            mFontScale = DEFAULT_FONT_SCALE;
            mForegroundColor = COLOR_UNSPECIFIED;
            mBackgroundColor = COLOR_UNSPECIFIED;
            mEdgeType = EDGE_TYPE_UNSPECIFIED;
            mEdgeColor = COLOR_UNSPECIFIED;
            mWindowType = WINDOW_TYPE_UNSPECIFIED;
            mWindowColor = COLOR_UNSPECIFIED;
            mWindowCornerRadius = 0;
            mFontFamily = null;
            mFontGenericFamily = FONT_FAMILY_UNSPECIFIED;
            mFontStyle = FONT_STYLE_UNSPECIFIED;
            mExtras = null;
        }

        /**
         * Constructs a new TextTrackStyle based on the systems' current closed caption style settings.
         * On platform levels below 19, this returns an object with "unspecified" values for all
         * fields.
         *
         * @param context The calling context.
         * @return The new TextTrackStyle.
         */
        public static TextTrackStyle fromSystemSettings(Context context) {
            TextTrackStyle style = new TextTrackStyle();
            if (Build.VERSION.SDK_INT >= 19) {
                Impl19.loadSystemSettings(style, context);
            }
            return style;
        }

        /**
         * Creates a bundle representation of the object.
         */
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putFloat(KEY_FONT_SCALE, mFontScale);
            bundle.putInt(KEY_FOREGROUND_COLOR, mForegroundColor);
            bundle.putInt(KEY_BACKGROUND_COLOR, mBackgroundColor);
            bundle.putInt(KEY_EDGE_TYPE, mEdgeType);
            bundle.putInt(KEY_EDGE_COLOR, mEdgeColor);
            bundle.putInt(KEY_WINDOW_TYPE, mWindowType);
            bundle.putInt(KEY_WINDOW_COLOR, mWindowColor);
            bundle.putInt(KEY_WINDOW_CORNER_RADIUS, mWindowCornerRadius);
            bundle.putString(KEY_FONT_FAMILY, mFontFamily);
            bundle.putInt(KEY_FONT_GENERIC_FAMILY, mFontGenericFamily);
            bundle.putInt(KEY_FONT_STYLE, mFontStyle);
            bundle.putBundle(KEY_EXTRAS, mExtras);
            return bundle;
        }

        /**
         * Constructs a new {@link MediaTrack} object from a bundle.
         */
        public static TextTrackStyle fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }

            TextTrackStyle style = new TextTrackStyle();
            style.setFontScale(bundle.getFloat(KEY_FONT_SCALE));
            style.setForegroundColor(bundle.getInt(KEY_FOREGROUND_COLOR));
            style.setBackgroundColor(bundle.getInt(KEY_BACKGROUND_COLOR));
            style.setEdgeType(bundle.getInt(KEY_EDGE_TYPE));
            style.setEdgeColor(bundle.getInt(KEY_EDGE_COLOR));
            style.setWindowType(bundle.getInt(KEY_WINDOW_TYPE));
            style.setWindowColor(bundle.getInt(KEY_WINDOW_COLOR));
            style.setWindowCornerRadius(bundle.getInt(KEY_WINDOW_CORNER_RADIUS));
            style.setFontFamily(bundle.getString(KEY_FONT_FAMILY));
            style.setFontGenericFamily(bundle.getInt(KEY_FONT_GENERIC_FAMILY));
            style.setFontStyle(bundle.getInt(KEY_FONT_STYLE));
            style.setExtras(bundle.getBundle(KEY_EXTRAS));
            return style;
        }

        // Compatibility for new platform features introduced in KitKat.
        private static final class Impl19 {
            public static void loadSystemSettings(TextTrackStyle style, Context context) {
                CaptioningManager captioningManager =
                        (CaptioningManager)context.getSystemService(Context.CAPTIONING_SERVICE);
                style.setFontScale(captioningManager.getFontScale());

                CaptioningManager.CaptionStyle userStyle = captioningManager.getUserStyle();
                style.setBackgroundColor(userStyle.backgroundColor);
                style.setForegroundColor(userStyle.foregroundColor);

                switch (userStyle.edgeType) {
                    case CaptioningManager.CaptionStyle.EDGE_TYPE_OUTLINE:
                        style.setEdgeType(EDGE_TYPE_OUTLINE);
                        break;

                    case CaptioningManager.CaptionStyle.EDGE_TYPE_DROP_SHADOW:
                        style.setEdgeType(EDGE_TYPE_DROP_SHADOW);
                        break;

                    case CaptioningManager.CaptionStyle.EDGE_TYPE_NONE:  // Fall through
                    default:
                        style.setEdgeType(EDGE_TYPE_NONE);
                }

                style.setEdgeColor(userStyle.edgeColor);

                Typeface typeface = userStyle.getTypeface();
                if (typeface != null) {
                    if (Typeface.MONOSPACE.equals(typeface)) {
                        style.setFontGenericFamily(FONT_FAMILY_MONOSPACED_SANS_SERIF);
                    } else if (Typeface.SANS_SERIF.equals(typeface)) {
                        style.setFontGenericFamily(FONT_FAMILY_SANS_SERIF);
                    } else if (Typeface.SERIF.equals(typeface)) {
                        style.setFontGenericFamily(FONT_FAMILY_SERIF);
                    } else {
                        // Otherwise, assume sans-serif.
                        style.setFontGenericFamily(FONT_FAMILY_SANS_SERIF);
                    }

                    boolean bold = typeface.isBold();
                    boolean italic = typeface.isItalic();

                    if (bold && italic) {
                        style.setFontStyle(FONT_STYLE_BOLD_ITALIC);
                    } else if (bold) {
                        style.setFontStyle(FONT_STYLE_BOLD);
                    } else if (italic) {
                        style.setFontStyle(FONT_STYLE_ITALIC);
                    } else {
                        style.setFontStyle(FONT_STYLE_NORMAL);
                    }
                }
            }
        }
    }

    /**
     * A class that represents an image that is located on a web server.
     */
    public static final class WebImage {
        private final Uri mUrl;
        private final int mWidth;
        private final int mHeight;

        private static final String KEY_URL = "url";
        private static final String KEY_HEIGHT = "height";
        private static final String KEY_WIDTH = "width";

        /**
         * Constructs a new {@link WebImage} with the given URL.
         *
         * @param url The URL of the image.
         * @throws IllegalArgumentException If the URL is null or empty.
         */
        public WebImage(@NonNull Uri url) throws IllegalArgumentException {
            this(url, 0, 0);
        }

        /**
         * Constructs a new {@link WebImage} with the given URL and dimensions.
         *
         * @param url The URL of the image.
         * @param width The width of the image, in pixels.
         * @param height The height of the image, in pixels.
         * @throws IllegalArgumentException If the URL is null or empty,
         * or the dimensions are invalid.
         */
        public WebImage(@NonNull Uri url, int width, int height) throws IllegalArgumentException {
            if (url == null) {
                throw new IllegalArgumentException("url cannot be null");
            }

            if ((width < 0) || (height < 0)) {
                throw new IllegalArgumentException("width and height must not be negative");
            }

            mUrl = url;
            mWidth = width;
            mHeight = height;
        }

        /**
         * Gets the image URL.
         */
        public @NonNull Uri getUrl() {
            return mUrl;
        }

        /**
         * Gets the image width, in pixels.
         */
        public int getWidth() {
            return mWidth;
        }

        /**
         * Gets the image height, in pixels.
         */
        public int getHeight() {
            return mHeight;
        }

        /**
         * Returns a string representation of this object.
         */
        @Override
        public @NonNull String toString() {
            return String.format("Image %dx%d %s", mWidth, mHeight, mUrl.toString());
        }

        /**
         * Creates a bundle representation of this object.
         */
        public @NonNull Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_URL, mUrl.toString());
            bundle.putInt(KEY_WIDTH, mWidth);
            bundle.putInt(KEY_HEIGHT, mHeight);
            return bundle;
        }

        /**
         * Creates a {@link WebImage} from a bundle.
         */
        public static @Nullable WebImage fromBundle(@Nullable Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            return new WebImage(Uri.parse(bundle.getString(KEY_URL)),
                    bundle.getInt(KEY_WIDTH), bundle.getInt(KEY_HEIGHT));
        }
    }
}
