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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.media2.MediaPlayer2.MEDIA_ERROR_IO;
import static androidx.media2.MediaPlayer2.MEDIA_ERROR_MALFORMED;
import static androidx.media2.MediaPlayer2.MEDIA_ERROR_TIMED_OUT;
import static androidx.media2.MediaPlayer2.MEDIA_ERROR_UNKNOWN;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_AUDIO;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_METADATA;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_VIDEO;
import static androidx.media2.SubtitleData.MIMETYPE_TEXT_CEA_608;
import static androidx.media2.SubtitleData.MIMETYPE_TEXT_CEA_708;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;
import androidx.media2.CallbackMediaItem;
import androidx.media2.FileMediaItem;
import androidx.media2.MediaItem;
import androidx.media2.MediaPlayer2;
import androidx.media2.PlaybackParams;
import androidx.media2.UriMediaItem;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.ExoPlaybackException;
import androidx.media2.exoplayer.external.Format;
import androidx.media2.exoplayer.external.ParserException;
import androidx.media2.exoplayer.external.PlaybackParameters;
import androidx.media2.exoplayer.external.SeekParameters;
import androidx.media2.exoplayer.external.audio.AudioAttributes;
import androidx.media2.exoplayer.external.extractor.DefaultExtractorsFactory;
import androidx.media2.exoplayer.external.extractor.ExtractorsFactory;
import androidx.media2.exoplayer.external.extractor.ts.AdtsExtractor;
import androidx.media2.exoplayer.external.mediacodec.MediaFormatUtil;
import androidx.media2.exoplayer.external.source.ExtractorMediaSource;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.source.hls.HlsMediaSource;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.HttpDataSource;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.Util;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Utility methods for translating between the MediaPlayer2 and ExoPlayer APIs.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@TargetApi(Build.VERSION_CODES.KITKAT)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ class ExoPlayerUtils {

    private static final ExtractorsFactory sExtractorsFactory = new DefaultExtractorsFactory()
            .setAdtsExtractorFlags(AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING);

    /**
     * Returns an ExoPlayer media source for the given media item. The given {@link MediaItem} is
     * set as the tag of the source.
     */
    public static MediaSource createUnclippedMediaSource(
            DataSource.Factory dataSourceFactory, MediaItem mediaItem) {
        if (mediaItem instanceof UriMediaItem) {
            Uri uri = ((UriMediaItem) mediaItem).getUri();
            if (Util.inferContentType(uri) == C.TYPE_HLS) {
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .setTag(mediaItem)
                        .createMediaSource(uri);
            } else {
                return new ExtractorMediaSource.Factory(dataSourceFactory)
                        .setExtractorsFactory(sExtractorsFactory)
                        .setTag(mediaItem)
                        .createMediaSource(uri);
            }
        } else if (mediaItem instanceof FileMediaItem) {
            return new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(sExtractorsFactory)
                    .setTag(mediaItem)
                    .createMediaSource(Uri.EMPTY);
        } else if (mediaItem instanceof CallbackMediaItem) {
            CallbackMediaItem callbackMediaItem = (CallbackMediaItem) mediaItem;
            dataSourceFactory = DataSourceCallbackDataSource.getFactory(
                    callbackMediaItem.getDataSourceCallback());
            return new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(sExtractorsFactory)
                    .setTag(mediaItem)
                    .createMediaSource(Uri.EMPTY);
        } else {
            throw new IllegalStateException();
        }
    }

    /** Returns ExoPlayer audio attributes for the given audio attributes. */
    public static AudioAttributes getAudioAttributes(AudioAttributesCompat audioAttributesCompat) {
        return new AudioAttributes.Builder()
                .setContentType(audioAttributesCompat.getContentType())
                .setFlags(audioAttributesCompat.getFlags())
                .setUsage(audioAttributesCompat.getUsage())
                .build();
    }

    /** Returns audio attributes for the given ExoPlayer audio attributes. */
    public static AudioAttributesCompat getAudioAttributesCompat(AudioAttributes audioAttributes) {
        return new AudioAttributesCompat.Builder()
                .setContentType(audioAttributes.contentType)
                .setFlags(audioAttributes.flags)
                .setUsage(audioAttributes.usage)
                .build();
    }

    /** Returns ExoPlayer playback parameters for the given playback params. */
    public static PlaybackParameters getPlaybackParameters(PlaybackParams playbackParams2) {
        Float speed = playbackParams2.getSpeed();
        Float pitch = playbackParams2.getPitch();
        return new PlaybackParameters(speed != null ? speed : 1f, pitch != null ? pitch : 1f);
    }

    /** Returns the ExoPlayer seek parameters corresponding to the given seek mode. */
    public static SeekParameters getSeekParameters(int seekMode) {
        switch (seekMode) {
            case MediaPlayer2.SEEK_CLOSEST:
                return SeekParameters.EXACT;
            case MediaPlayer2.SEEK_CLOSEST_SYNC:
                return SeekParameters.CLOSEST_SYNC;
            case MediaPlayer2.SEEK_NEXT_SYNC:
                return SeekParameters.NEXT_SYNC;
            case MediaPlayer2.SEEK_PREVIOUS_SYNC:
                return SeekParameters.PREVIOUS_SYNC;
            default:
                throw new IllegalArgumentException();
        }
    }

    /** Returns the MEDIA_ERROR_* constant for an ExoPlayer player exception. */
    public static int getError(ExoPlaybackException exception) {
        if (exception.type == ExoPlaybackException.TYPE_SOURCE) {
            IOException sourceException = exception.getSourceException();
            if (sourceException instanceof ParserException) {
                return MEDIA_ERROR_MALFORMED;
            } else {
                if (sourceException instanceof HttpDataSource.HttpDataSourceException
                        && sourceException.getCause() instanceof SocketTimeoutException) {
                    return MEDIA_ERROR_TIMED_OUT;
                }
                return MEDIA_ERROR_IO;
            }
        }
        return MEDIA_ERROR_UNKNOWN;
    }

    /** Returns the ExoPlayer track type for the given MediaPlayer2 track type. */
    public static int getExoPlayerTrackType(int trackType) {
        switch (trackType) {
            case MEDIA_TRACK_TYPE_AUDIO:
                return C.TRACK_TYPE_AUDIO;
            case MEDIA_TRACK_TYPE_VIDEO:
                return C.TRACK_TYPE_VIDEO;
            case MEDIA_TRACK_TYPE_SUBTITLE:
                return C.TRACK_TYPE_TEXT;
            case MEDIA_TRACK_TYPE_METADATA:
                return C.TRACK_TYPE_METADATA;
            case MEDIA_TRACK_TYPE_UNKNOWN:
            case MEDIA_TRACK_TYPE_TIMEDTEXT: // Unexpected
            default:
                return C.TRACK_TYPE_UNKNOWN;
        }
    }

    /** Returns the track type corresponding to the given ExoPlayer track type. */
    public static int getTrackType(int exoPlayerTrackType) {
        switch (exoPlayerTrackType) {
            case C.TRACK_TYPE_AUDIO:
                return MEDIA_TRACK_TYPE_AUDIO;
            case C.TRACK_TYPE_VIDEO:
                return MEDIA_TRACK_TYPE_VIDEO;
            case C.TRACK_TYPE_TEXT:
                return MEDIA_TRACK_TYPE_SUBTITLE;
            case C.TRACK_TYPE_METADATA:
                return MEDIA_TRACK_TYPE_METADATA;
            case C.TRACK_TYPE_NONE:
            case C.TRACK_TYPE_CAMERA_MOTION:
            case C.TRACK_TYPE_DEFAULT:
            case C.TRACK_TYPE_UNKNOWN:
            default:
                return MEDIA_TRACK_TYPE_UNKNOWN;
        }
    }

    /** Returns the media format corresponding to an ExoPlayer format. */
    @SuppressLint("InlinedApi")
    public static MediaFormat getMediaFormat(Format format) {
        MediaFormat mediaFormat = new MediaFormat();
        String mimeType = format.sampleMimeType;
        mediaFormat.setString(MediaFormat.KEY_MIME, mimeType);
        int trackType = MimeTypes.getTrackType(mimeType);
        if (trackType == C.TRACK_TYPE_AUDIO) {
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, format.channelCount);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, format.sampleRate);
            if (format.language != null) {
                mediaFormat.setString(MediaFormat.KEY_LANGUAGE, format.language);
            }
        } else if (trackType == C.TRACK_TYPE_VIDEO) {
            MediaFormatUtil.maybeSetInteger(mediaFormat, MediaFormat.KEY_WIDTH, format.width);
            MediaFormatUtil.maybeSetInteger(mediaFormat, MediaFormat.KEY_HEIGHT, format.height);
            MediaFormatUtil.maybeSetFloat(
                    mediaFormat, MediaFormat.KEY_FRAME_RATE, format.frameRate);
            MediaFormatUtil.maybeSetInteger(
                    mediaFormat, MediaFormat.KEY_ROTATION, format.rotationDegrees);
            MediaFormatUtil.maybeSetColorInfo(mediaFormat, format.colorInfo);
        } else if (trackType == C.TRACK_TYPE_TEXT) {
            boolean isAutoselect = format.selectionFlags == C.SELECTION_FLAG_AUTOSELECT;
            boolean isDefault = format.selectionFlags == C.SELECTION_FLAG_DEFAULT;
            boolean isForced = format.selectionFlags == C.SELECTION_FLAG_FORCED;
            mediaFormat.setInteger(MediaFormat.KEY_IS_AUTOSELECT, isAutoselect ? 1 : 0);
            mediaFormat.setInteger(MediaFormat.KEY_IS_DEFAULT, isDefault ? 1 : 0);
            mediaFormat.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, isForced ? 1 : 0);
            if (format.language == null) {
                mediaFormat.setString(MediaFormat.KEY_LANGUAGE, C.LANGUAGE_UNDETERMINED);
            } else {
                mediaFormat.setString(MediaFormat.KEY_LANGUAGE, format.language);
            }
            // MediaPlayer2 uses text/* instead of application/* MIME types.
            if (MimeTypes.APPLICATION_CEA608.equals(mimeType)) {
                mediaFormat.setString(MediaFormat.KEY_MIME, MIMETYPE_TEXT_CEA_608);
            } else if (MimeTypes.APPLICATION_CEA708.equals(mimeType)) {
                mediaFormat.setString(MediaFormat.KEY_MIME, MIMETYPE_TEXT_CEA_708);
            }
        }
        return mediaFormat;
    }

    private ExoPlayerUtils() {
        // Prevent instantiation.
    }

}
