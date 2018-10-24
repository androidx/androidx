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
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_UNKNOWN;
import static androidx.media2.MediaPlayer2.TrackInfo.MEDIA_TRACK_TYPE_VIDEO;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.media.AudioAttributesCompat;
import androidx.media2.CallbackMediaItem2;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaPlayer2;
import androidx.media2.PlaybackParams2;
import androidx.media2.UriMediaItem2;
import androidx.media2.common.TrackInfoImpl;
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
import androidx.media2.exoplayer.external.source.TrackGroup;
import androidx.media2.exoplayer.external.source.TrackGroupArray;
import androidx.media2.exoplayer.external.source.hls.HlsMediaSource;
import androidx.media2.exoplayer.external.upstream.DataSource;
import androidx.media2.exoplayer.external.upstream.HttpDataSource;
import androidx.media2.exoplayer.external.util.MimeTypes;
import androidx.media2.exoplayer.external.util.Util;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

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
     * Returns an ExoPlayer media source for the given media item. The given {@link MediaItem2} is
     * set as the tag of the source.
     */
    public static MediaSource createUnclippedMediaSource(
            DataSource.Factory dataSourceFactory, MediaItem2 mediaItem2) {
        if (mediaItem2 instanceof UriMediaItem2) {
            Uri uri = ((UriMediaItem2) mediaItem2).getUri();
            if (Util.inferContentType(uri) == C.TYPE_HLS) {
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .setTag(mediaItem2)
                        .createMediaSource(uri);
            } else {
                return new ExtractorMediaSource.Factory(dataSourceFactory)
                        .setExtractorsFactory(sExtractorsFactory)
                        .setTag(mediaItem2)
                        .createMediaSource(uri);
            }
        } else if (mediaItem2 instanceof FileMediaItem2) {
            return new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(sExtractorsFactory)
                    .setTag(mediaItem2)
                    .createMediaSource(Uri.EMPTY);
        } else if (mediaItem2 instanceof CallbackMediaItem2) {
            CallbackMediaItem2 callbackMediaItem2 = (CallbackMediaItem2) mediaItem2;
            dataSourceFactory = DataSourceCallback2DataSource.getFactory(
                    callbackMediaItem2.getDataSourceCallback2());
            return new ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(sExtractorsFactory)
                    .setTag(mediaItem2)
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
    public static PlaybackParameters getPlaybackParameters(PlaybackParams2 playbackParams2) {
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
        // ExoPlayer seeks to read moov so we don't need to notify
        // MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK.
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

    /** Returns the track info list corresponding to an ExoPlayer track group array. */
    public static List<MediaPlayer2.TrackInfo> getTrackInfo(TrackGroupArray trackGroupArray) {
        ArrayList<MediaPlayer2.TrackInfo> trackInfos = new ArrayList<>();
        for (int i = 0; i < trackGroupArray.length; i++) {
            TrackGroup trackGroup = trackGroupArray.get(i);
            Format format = trackGroup.getFormat(0);
            MediaFormat mediaFormat = getMediaFormat(format);
            String mimeType = format.sampleMimeType;
            int trackType = getTrackType(mimeType);
            trackInfos.add(new TrackInfoImpl(trackType, mediaFormat));
        }
        // Note: the list returned by MediaPlayer2Impl is modifiable so we do the same here.
        return trackInfos;
    }

    /** Returns the track type corresponding to the given MIME type. */
    private static int getTrackType(String mimeType) {
        return MimeTypes.isAudio(mimeType) ? MEDIA_TRACK_TYPE_AUDIO
                : MimeTypes.isVideo(mimeType) ? MEDIA_TRACK_TYPE_VIDEO
                        : MimeTypes.isText(mimeType) ? MEDIA_TRACK_TYPE_SUBTITLE
                                : MEDIA_TRACK_TYPE_UNKNOWN;
    }

    /** Returns the media format corresponding to an ExoPlayer format. */
    @SuppressLint("InlinedApi")
    private static MediaFormat getMediaFormat(Format format) {
        String mimeType = format.sampleMimeType;
        MediaFormat mediaFormat = new MediaFormat();
        // Set format parameters that should always be set.
        mediaFormat.setString(MediaFormat.KEY_MIME, mimeType);
        if (MimeTypes.isAudio(mimeType)) {
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, format.channelCount);
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, format.sampleRate);
            MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        } else if (MimeTypes.isVideo(mimeType)) {
            mediaFormat.setString(MediaFormat.KEY_MIME, format.sampleMimeType);
            mediaFormat.setInteger(MediaFormat.KEY_WIDTH, format.width);
            mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, format.height);
            MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
            // Set format parameters that may be unset.
            MediaFormatUtil.maybeSetFloat(
                    mediaFormat, MediaFormat.KEY_FRAME_RATE, format.frameRate);
            MediaFormatUtil.maybeSetInteger(
                    mediaFormat, MediaFormat.KEY_ROTATION, format.rotationDegrees);
            MediaFormatUtil.maybeSetColorInfo(mediaFormat, format.colorInfo);
        } else {
            // TODO(b/111150876): Configure timed text/subtitle formats.
            mediaFormat.setInteger(MediaFormat.KEY_LANGUAGE, format.channelCount);
        }
        return mediaFormat;
    }

    private ExoPlayerUtils() {
        // Prevent instantiation.
    }

}
